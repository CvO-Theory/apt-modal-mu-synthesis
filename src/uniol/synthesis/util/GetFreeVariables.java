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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;

import uniol.synthesis.adt.mu_calculus.ConjunctionFormula;
import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.DisjunctionFormula;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.LetFormula;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.NegationFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class GetFreeVariables extends RecursiveFormulaWalker {
	final private Deque<Map<VariableFormula, Integer>> freeVariables = new ArrayDeque<>();
	final private Deque<Bag<VariableFormula>> currentlyBoundVariables = new ArrayDeque<>();

	private GetFreeVariables() {
		pushScope();
	}

	private void pushScope() {
		freeVariables.addLast(new HashMap<VariableFormula, Integer>());
		currentlyBoundVariables.addLast(new HashBag<VariableFormula>());
	}

	private Map<VariableFormula, Integer> popScope() {
		currentlyBoundVariables.removeLast();
		return freeVariables.removeLast();
	}

	private Map<VariableFormula, Integer> currentScopeFree() {
		return freeVariables.getLast();
	}

	private Bag<VariableFormula> currentScopeBound() {
		return currentlyBoundVariables.getLast();
	}

	private void addFreeVariable(VariableFormula variable, int toAdd) {
		Map<VariableFormula, Integer> currentScope = currentScopeFree();
		Integer count = currentScope.get(variable);
		if (count == null)
			count = toAdd;
		else
			count += toAdd;
		currentScope.put(variable, count);
	}

	private void addFreeVariables(Map<VariableFormula, Integer> toAdd) {
		for (Map.Entry<VariableFormula, Integer> entry : toAdd.entrySet())
			addFreeVariable(entry.getKey(), entry.getValue());
	}

	public Map<VariableFormula, Integer> getFreeVariablesCounts() {
		return Collections.unmodifiableMap(currentScopeFree());
	}

	public Set<VariableFormula> getFreeVariables() {
		return getFreeVariablesCounts().keySet();
	}

	@Override
	protected void visit(NonRecursive engine, VariableFormula formula) {
		if (!currentScopeBound().contains(formula))
			addFreeVariable(formula, 1);
	}

	@Override
	protected void enter(NonRecursive engine, FixedPointFormula formula) {
		currentScopeBound().add(formula.getVariable());
	}

	@Override
	protected void exit(NonRecursive engine, FixedPointFormula formula) {
		boolean changed = currentScopeBound().remove(formula.getVariable(), 1);
		assert changed;
	}

	@Override
	protected void enter(NonRecursive engine, LetFormula formula) {
		pushScope();
	}

	@Override
	protected void exitExpansion(NonRecursive engine, LetFormula formula) {
		pushScope();
	}

	@Override
	protected void exit(NonRecursive engine, LetFormula formula) {
		Map<VariableFormula, Integer> formulaScope = popScope();
		Map<VariableFormula, Integer> expansionScope = popScope();
		VariableFormula variable = formula.getVariable();
		if (formulaScope.remove(variable) != null) {
			for (VariableFormula var : currentScopeBound())
				expansionScope.remove(var);
			addFreeVariables(expansionScope);
		}
		for (VariableFormula var : currentScopeBound())
			formulaScope.remove(var);
		addFreeVariables(formulaScope);

	}

	static public Map<VariableFormula, Integer> getFreeVariablesCounts(Formula formula) {
		GetFreeVariables gfv = new GetFreeVariables();
		gfv.walk(formula);
		return gfv.getFreeVariablesCounts();
	}

	static public Set<VariableFormula> getFreeVariables(Formula formula) {
		GetFreeVariables gfv = new GetFreeVariables();
		gfv.walk(formula);
		return gfv.getFreeVariables();
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
