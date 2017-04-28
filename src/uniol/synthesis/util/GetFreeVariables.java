/*-
 * APT - Analysis of Petri Nets and labeled Transition systems
 * Copyright (C) 2017  Uli Schlachter
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package uniol.synthesis.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;

import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.ConjunctionFormula;
import uniol.synthesis.adt.mu_calculus.DisjunctionFormula;
import uniol.synthesis.adt.mu_calculus.NegationFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;

public class GetFreeVariables extends RecursiveFormulaWalker {
	final private Set<VariableFormula> freeVariables = new HashSet<>();
	final private Bag<VariableFormula> currentlyBoundVariables = new HashBag<>();

	private GetFreeVariables() {
	}

	public Set<VariableFormula> getFreeVariables() {
		return Collections.unmodifiableSet(freeVariables);
	}

	@Override
	protected void visit(NonRecursive engine, VariableFormula formula) {
		if (!currentlyBoundVariables.contains(formula))
			freeVariables.add(formula);
	}

	@Override
	protected void enter(NonRecursive engine, FixedPointFormula formula) {
		currentlyBoundVariables.add(formula.getVariable());
	}

	@Override
	protected void exit(NonRecursive engine, FixedPointFormula formula) {
		boolean changed = currentlyBoundVariables.remove(formula.getVariable(), 1);
		assert changed;
	}

	static public Set<VariableFormula> getFreeVariables(Formula formula) {
		GetFreeVariables gfv = new GetFreeVariables();
		gfv.walk(formula);
		return gfv.getFreeVariables();
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
