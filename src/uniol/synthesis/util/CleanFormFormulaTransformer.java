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
import java.util.Set;
import java.util.HashSet;

import uniol.synthesis.adt.mu_calculus.ConjunctionFormula;
import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.DisjunctionFormula;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.LetFormula;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.NegationFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class CleanFormFormulaTransformer extends FormulaTransformer {
	final private Map<VariableFormula, VariableFormula> variableReplacements = new HashMap<>();
	final private Map<VariableFormula, VariableFormula> oldVariableReplacements = new HashMap<>();

	private CleanFormFormulaTransformer(Set<VariableFormula> freeVariables) {
		for (VariableFormula var : freeVariables)
			variableReplacements.put(var, var);
	}

	private void enterScope(VariableFormula variable) {
		VariableFormula replacement;
		if (variableReplacements.containsKey(variable)) {
			replacement = variable.getCreator().freshVariable(variable.getVariable());
		} else {
			// Variable is new, it does not need to be replaced
			replacement = variable;
		}
		VariableFormula old = variableReplacements.put(variable, replacement);
		VariableFormula oldOld = oldVariableReplacements.put(replacement, old);
		// We are in clean form and thus each bound variable must be different
		assert oldOld == null;
	}

	private void exitScope(VariableFormula variable) {
		VariableFormula replacement = variableReplacements.get(variable);
		assert replacement != null;
		VariableFormula old = oldVariableReplacements.remove(replacement);
		if (old != null)
			variableReplacements.put(variable, old);
		else
			variableReplacements.remove(variable);
	}

	@Override
	public void enter(NonRecursive engine, FixedPointFormula formula) {
		super.enter(engine, formula);
		enterScope(formula.getVariable());
	}

	@Override
	public void exit(NonRecursive engine, FixedPointFormula formula) {
		super.exit(engine, formula);
		exitScope(formula.getVariable());
	}

	@Override
	protected void exitExpansion(NonRecursive engine, LetFormula formula) {
		super.exitExpansion(engine, formula);
		enterScope(formula.getVariable());
	}

	@Override
	public void exit(NonRecursive engine, LetFormula formula) {
		super.exit(engine, formula);
		exitScope(formula.getVariable());
	}

	@Override
	protected Formula transform(VariableFormula formula) {
		Formula replacement = variableReplacements.get(formula);
		assert replacement != null : formula;
		return replacement;
	}

	@Override
	protected Formula transform(FixedPointFormula formula) {
		VariableFormula replacement = variableReplacements.get(formula.getVariable());
		return formula.getCreator().fixedPoint(formula.getFixedPoint(), replacement, formula.getFormula());
	}

	@Override
	protected Formula transform(LetFormula formula) {
		VariableFormula replacement = variableReplacements.get(formula.getVariable());
		return formula.getCreator().let(replacement, formula.getExpansion(), formula.getFormula());
	}

	static public Formula cleanForm(Formula formula) {
		Set<VariableFormula> freeVariables = GetFreeVariables.getFreeVariables(formula);
		return new CleanFormFormulaTransformer(freeVariables).transform(formula);
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
