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
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uniol.synthesis.adt.mu_calculus.ConjunctionFormula;
import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.DisjunctionFormula;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.LetFormula;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.NegationFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class PositiveFormFormulaTransformer extends FormulaTransformer {
	private boolean negated;
	private final Deque<Map<VariableFormula, VariableFormula>> negatedVariables = new ArrayDeque<>();
	private final Deque<Set<VariableFormula>> usedVariables = new ArrayDeque<>();

	@Override
	public void reset() {
		super.reset();
		negated = false;
		negatedVariables.clear();
		negatedVariables.addLast(new HashMap<VariableFormula, VariableFormula>());
		usedVariables.clear();
		usedVariables.addLast(new HashSet<VariableFormula>());
	}

	@Override
	public void enter(NonRecursive engine, NegationFormula formula) {
		super.enter(engine, formula);
		negated = !negated;
	}

	@Override
	public void exit(NonRecursive engine, NegationFormula formula) {
		super.exit(engine, formula);
		negated = !negated;
	}

	@Override
	protected Formula transform(ConstantFormula formula) {
		if (!negated)
			return formula;
		return getCreator().constant(!formula.getValue());
	}

	@Override
	protected Formula transform(ConjunctionFormula formula) {
		if (!negated)
			return formula;
		return getCreator().disjunction(formula.getLeft(), formula.getRight());
	}

	@Override
	protected Formula transform(DisjunctionFormula formula) {
		if (!negated)
			return formula;
		return getCreator().conjunction(formula.getLeft(), formula.getRight());
	}

	@Override
	protected Formula transform(NegationFormula formula) {
		return formula.getFormula();
	}

	@Override
	protected Formula transform(VariableFormula formula) {
		VariableFormula negatedVariable = negatedVariables.getLast().get(formula);
		if (negatedVariable != null) {
			VariableFormula var = formula;
			if (negated)
				var = negatedVariable;
			usedVariables.getLast().add(var);
			return var;
		}
		if (!negated)
			return formula;
		return getCreator().negate(formula);
	}

	@Override
	protected Formula transform(ModalityFormula formula) {
		if (!negated)
			return formula;
		return getCreator().modality(formula.getModality().negate(), formula.getEvent(), formula.getFormula());
	}

	@Override
	protected Formula transform(FixedPointFormula formula) {
		if (!negated)
			return formula;
		// Dunno how to do this nicely, so do it un-nice.
		// !mu X.X = nu X.X, but the rest of this code generates nu
		// X.!X. Handle this by substituting !X for X in the inner
		// formula and calculating the positive form of that again.
		VariableFormula var = formula.getVariable();
		Formula inner = formula.getFormula();
		inner = SubstitutionTransformer.substitute(inner, var, formula.getCreator().negate(var));
		inner = new PositiveFormFormulaTransformer().transform(inner);
		return getCreator().fixedPoint(formula.getFixedPoint().negate(), formula.getVariable(), inner);
	}

	@Override
	protected void exitExpansion(NonRecursive engine, LetFormula formula) {
		VariableFormula variable = formula.getVariable();
		negatedVariables.addLast(new HashMap<VariableFormula, VariableFormula>(negatedVariables.getLast()));
		negatedVariables.getLast().put(variable, getCreator().freshVariable(variable.getVariable()));
		usedVariables.addLast(new HashSet<VariableFormula>());
	}

	@Override
	public void exit(NonRecursive engine, LetFormula formula) {
		super.exit(engine, formula);
		negatedVariables.removeLast();
		Set<VariableFormula> used = usedVariables.removeLast();
		usedVariables.getLast().addAll(used);
	}

	@Override
	public Formula transform(LetFormula formula) {
		VariableFormula variable = formula.getVariable();
		Formula expansion = formula.getExpansion();

		VariableFormula negatedVariable = negatedVariables.getLast().remove(variable);
		Formula negatedExpansion = getCreator().negate(expansion);
		// It would be nice if someone came up with a nicer way to do this... one that is not recursive.
		negatedExpansion = new PositiveFormFormulaTransformer().transform(negatedExpansion);

		if (negated) {
			// The expansion was negated while traversing, so the meaning of these two is swapped.
			Formula tmp = negatedExpansion;
			negatedExpansion = expansion;
			expansion = tmp;
		}

		Formula result = formula.getFormula();
		if (usedVariables.getLast().remove(variable)) {
			result = getCreator().let(variable, expansion, result);
		}
		if (usedVariables.getLast().remove(negatedVariable)) {
			result = getCreator().let(negatedVariable, negatedExpansion, result);
		}

		return result;
	}

	static public Formula positiveForm(Formula formula) {
		return new PositiveFormFormulaTransformer().transform(formula);
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
