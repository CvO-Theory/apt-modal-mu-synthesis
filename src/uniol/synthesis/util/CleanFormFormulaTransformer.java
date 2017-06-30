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

public class CleanFormFormulaTransformer extends FormulaFormulaTransformer {
	final private Map<VariableFormula, VariableFormula> variableReplacements = new HashMap<>();
	final private Map<VariableFormula, VariableFormula> oldVariableReplacements = new HashMap<>();

	@Override
	protected void enterScope(VariableFormula variable) {
		super.enterScope(variable);

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

		pushScope();
	}

	@Override
	protected void exitScope(VariableFormula variable) {
		super.exitScope(variable);

		VariableFormula replacement = variableReplacements.get(variable);
		assert replacement != null;
		VariableFormula old = oldVariableReplacements.remove(replacement);
		if (old != null)
			variableReplacements.put(variable, old);
		else
			variableReplacements.remove(variable);

		popScope();
	}

	@Override
	protected void enqueueWalker(NonRecursive engine, Formula formula) {
		engine.enqueue(new Worker(formula));
	}

	private class Worker extends FormulaFormulaTransformer.FillCache {
		private Worker(Formula formula) {
			super(formula);
		}

		@Override
		public void walk(NonRecursive engine, ConstantFormula formula) {
			setCache(formula, formula);
		}

		@Override
		public void walk(NonRecursive engine, VariableFormula formula) {
			VariableFormula replacement = variableReplacements.get(formula);
			if (replacement != null)
				setCache(formula, replacement);
			else
				setCache(formula, formula);
		}

		@Override
		public void walk(NonRecursive engine, final FixedPointFormula formula) {
			enterScope(formula.getVariable());
			engine.enqueue(new NonRecursive.Walker() {
				@Override
				public void walk(NonRecursive engine) {
					Formula transformedChild = getCache(formula.getFormula());
					assert transformedChild != null;

					VariableFormula newVariable = variableReplacements.get(formula.getVariable());
					exitScope(formula.getVariable());

					if (formula.getFormula().equals(transformedChild)
							&& formula.getVariable().equals(newVariable))
						setCache(formula, formula);
					else
						setCache(formula, formula.getCreator().fixedPoint(formula.getFixedPoint(),
									newVariable, transformedChild));
				}
			});
			if (getCache(formula.getFormula()) == null)
				enqueueWalker(engine, formula.getFormula());
		}

		@Override
		public void walk(NonRecursive engine, final LetFormula formula) {
			engine.enqueue(new NonRecursive.Walker() {
				@Override
				public void walk(NonRecursive engine) {
					Formula child = formula.getFormula();
					Formula transformedChild = getCache(child);

					VariableFormula newVariable = variableReplacements.get(formula.getVariable());
					exitScope(formula.getVariable());

					Formula expansion = formula.getExpansion();
					Formula transformedExpansion = getCache(expansion);

					if (child.equals(transformedChild) && expansion.equals(transformedExpansion)
							&& formula.getVariable().equals(newVariable))
						setCache(formula, formula);
					else
						setCache(formula, formula.getCreator().let(newVariable,
									transformedExpansion, transformedChild));
				}
			});
			enqueueWalker(engine, formula.getFormula());
			engine.enqueue(new NonRecursive.Walker() {
				@Override
				public void walk(NonRecursive engine) {
					enterScope(formula.getVariable());
				}
			});
			if (getCache(formula.getExpansion()) == null)
				enqueueWalker(engine, formula.getExpansion());
		}
	}

	static public Formula cleanForm(Formula formula) {
		CleanFormFormulaTransformer transformer = new CleanFormFormulaTransformer();
		for (VariableFormula var : GetFreeVariables.getFreeVariables(formula))
			transformer.variableReplacements.put(var, var);
		NonRecursive engine = new NonRecursive();
		transformer.transform(engine, formula);
		engine.run();
		return transformer.transform(engine, formula);
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
