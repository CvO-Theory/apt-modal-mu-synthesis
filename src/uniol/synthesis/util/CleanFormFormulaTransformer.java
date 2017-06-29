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

public class CleanFormFormulaTransformer {
	final private Map<VariableFormula, VariableFormula> variableReplacements = new HashMap<>();
	final private Map<VariableFormula, VariableFormula> oldVariableReplacements = new HashMap<>();
	final private Deque<Map<Formula, Formula>> cachedFormulas = new ArrayDeque<>();

	private CleanFormFormulaTransformer(Set<VariableFormula> freeVariables) {
		for (VariableFormula var : freeVariables)
			variableReplacements.put(var, var);
		cachedFormulas.addLast(new HashMap<Formula, Formula>());
	}

	private void setResult(Formula formula, Formula transformation) {
		cachedFormulas.getLast().put(formula, transformation);
	}

	private Formula getCache(Formula formula) {
		return cachedFormulas.getLast().get(formula);
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

	private VariableFormula exitScope(VariableFormula variable) {
		VariableFormula replacement = variableReplacements.get(variable);
		assert replacement != null;
		VariableFormula old = oldVariableReplacements.remove(replacement);
		if (old != null)
			variableReplacements.put(variable, old);
		else
			variableReplacements.remove(variable);
		return replacement;
	}

	private class FillCache extends FormulaWalker {
		private FillCache(Formula formula) {
			super(formula);
		}

		@Override
		public void walk(NonRecursive engine, ConstantFormula formula) {
			setResult(formula, formula);
		}

		@Override
		public void walk(NonRecursive engine, ConjunctionFormula formula) {
			Formula left = formula.getLeft();
			Formula right = formula.getRight();
			Formula transformedLeft = getCache(left);
			Formula transformedRight = getCache(right);
			if (transformedLeft == null || transformedRight == null) {
				engine.enqueue(this);
				if (transformedLeft == null)
					engine.enqueue(new FillCache(left));
				if (transformedRight == null)
					engine.enqueue(new FillCache(right));
				return;
			}
			if (left.equals(transformedLeft) && right.equals(transformedRight))
				setResult(formula, formula);
			else
				setResult(formula, formula.getCreator().conjunction(transformedLeft, transformedRight));
		}

		@Override
		public void walk(NonRecursive engine, DisjunctionFormula formula) {
			Formula left = formula.getLeft();
			Formula right = formula.getRight();
			Formula transformedLeft = getCache(left);
			Formula transformedRight = getCache(right);
			if (transformedLeft == null || transformedRight == null) {
				engine.enqueue(this);
				if (transformedLeft == null)
					engine.enqueue(new FillCache(left));
				if (transformedRight == null)
					engine.enqueue(new FillCache(right));
				return;
			}
			if (left.equals(transformedLeft) && right.equals(transformedRight))
				setResult(formula, formula);
			else
				setResult(formula, formula.getCreator().disjunction(transformedLeft, transformedRight));
		}

		@Override
		public void walk(NonRecursive engine, NegationFormula formula) {
			Formula child = formula.getFormula();
			Formula transformedChild = getCache(child);
			if (transformedChild == null) {
				engine.enqueue(this);
				engine.enqueue(new FillCache(child));
				return;
			}
			if (child.equals(transformedChild))
				setResult(formula, formula);
			else
				setResult(formula, formula.getCreator().negate(transformedChild));
		}

		@Override
		public void walk(NonRecursive engine, VariableFormula formula) {
			VariableFormula replacement = variableReplacements.get(formula);
			if (replacement != null)
				setResult(formula, replacement);
			else
				setResult(formula, formula);
		}

		@Override
		public void walk(NonRecursive engine, ModalityFormula formula) {
			Formula child = formula.getFormula();
			Formula transformedChild = getCache(child);
			if (transformedChild == null) {
				engine.enqueue(this);
				engine.enqueue(new FillCache(child));
				return;
			}
			if (child.equals(transformedChild))
				setResult(formula, formula);
			else
				setResult(formula, formula.getCreator().modality(formula.getModality(),
							formula.getEvent(), transformedChild));
		}

		@Override
		public void walk(NonRecursive engine, final FixedPointFormula formula) {
			enterScope(formula.getVariable());
			engine.enqueue(new NonRecursive.Walker() {
				@Override
				public void walk(NonRecursive engine) {
					Formula child = formula.getFormula();
					Formula transformedChild = getCache(child);
					assert transformedChild != null;

					VariableFormula newVariable = exitScope(formula.getVariable());

					if (child.equals(transformedChild) && formula.getVariable().equals(newVariable))
						setResult(formula, formula);
					else
						setResult(formula, formula.getCreator().fixedPoint(formula.getFixedPoint(),
									newVariable, transformedChild));
				}
			});
			engine.enqueue(new FillCache(formula.getFormula()));
		}

		@Override
		public void walk(NonRecursive engine, final LetFormula formula) {
			engine.enqueue(new NonRecursive.Walker() {
				@Override
				public void walk(NonRecursive engine) {
					Formula child = formula.getFormula();
					Formula transformedChild = getCache(child);

					VariableFormula newVariable = exitScope(formula.getVariable());

					Formula expansion = formula.getExpansion();
					Formula transformedExpansion = getCache(expansion);

					if (child.equals(transformedChild) && expansion.equals(transformedExpansion)
							&& formula.getVariable().equals(newVariable))
						setResult(formula, formula);
					else
						setResult(formula, formula.getCreator().let(newVariable,
									transformedExpansion, transformedChild));
				}
			});
			engine.enqueue(new FillCache(formula.getFormula()));
			engine.enqueue(new NonRecursive.Walker() {
				@Override
				public void walk(NonRecursive engine) {
					enterScope(formula.getVariable());
				}
			});
			if (getCache(formula.getExpansion()) == null)
				engine.enqueue(new FillCache(formula.getExpansion()));
		}
	}

	private Formula transform(Formula formula) {
		new NonRecursive().run(new FillCache(formula));
		return getCache(formula);
	}

	static public Formula cleanForm(Formula formula) {
		Set<VariableFormula> freeVariables = GetFreeVariables.getFreeVariables(formula);
		return new CleanFormFormulaTransformer(freeVariables).transform(formula);
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
