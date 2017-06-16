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

import java.util.Map;
import java.util.HashMap;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;

import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.LetFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class SubstitutionTransformer extends FormulaTransformer {
	private final Bag<VariableFormula> currentlyBoundVariables = new HashBag<>();
	private final Map<VariableFormula, Formula> substitution;

	public SubstitutionTransformer(Map<VariableFormula, Formula> substitution) {
		this.substitution = substitution;
	}

	@Override
	public void reset() {
		currentlyBoundVariables.clear();
	}

	protected Formula transform(VariableFormula formula) {
		if (currentlyBoundVariables.contains(formula))
			return formula;
		Formula transformation = substitution.get(formula);
		if (transformation != null)
			return transformation;
		return formula;
	}

	@Override
	public void enter(NonRecursive engine, FixedPointFormula formula) {
		super.enter(engine, formula);
		currentlyBoundVariables.add(formula.getVariable());
	}

	@Override
	public void exit(NonRecursive engine, FixedPointFormula formula) {
		super.exit(engine, formula);
		boolean changed = currentlyBoundVariables.remove(formula.getVariable(), 1);
		assert changed;
	}

	@Override
	protected void exitExpansion(NonRecursive engine, LetFormula formula) {
		super.exitExpansion(engine, formula);
		currentlyBoundVariables.add(formula.getVariable());
	}

	@Override
	public void exit(NonRecursive engine, LetFormula formula) {
		super.exit(engine, formula);
		boolean changed = currentlyBoundVariables.remove(formula.getVariable(), 1);
		assert changed;
	}

	static public Formula substitute(Formula formula, Map<VariableFormula, Formula> substitution) {
		return new SubstitutionTransformer(substitution).transform(formula);
	}

	static public Formula substitute(Formula formula, VariableFormula variable, Formula substitution) {
		Map<VariableFormula, Formula> sub = new HashMap<>();
		sub.put(variable, substitution);
		return substitute(formula, sub);
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
