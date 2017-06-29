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

public class SubstitutionTransformer {
	private final Bag<VariableFormula> currentlyBoundVariables = new HashBag<>();
	private final Deque<Map<Formula, Formula>> cachedSubstitutions = new ArrayDeque<>();
	private final Map<VariableFormula, Formula> substitution;

	public SubstitutionTransformer(Map<VariableFormula, Formula> substitution) {
		this.substitution = substitution;
		this.cachedSubstitutions.addLast(new HashMap<Formula, Formula>());
	}

	private void setResult(Formula formula, Formula transformation) {
		cachedSubstitutions.getLast().put(formula, transformation);
	}

	private Formula getCache(Formula formula) {
		return cachedSubstitutions.getLast().get(formula);
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
			Formula expansion = null;
			if (!currentlyBoundVariables.contains(formula))
				expansion = substitution.get(formula);
			if (expansion == null)
				setResult(formula, formula);
			else
				setResult(formula, expansion);
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
			currentlyBoundVariables.add(formula.getVariable());
			cachedSubstitutions.addLast(new HashMap<Formula, Formula>());
			engine.enqueue(new NonRecursive.Walker() {
				@Override
				public void walk(NonRecursive engine) {
					Formula child = formula.getFormula();
					Formula transformedChild = getCache(child);
					assert transformedChild != null;

					currentlyBoundVariables.remove(formula.getVariable(), 1);
					cachedSubstitutions.removeLast();

					if (child.equals(transformedChild))
						setResult(formula, formula);
					else
						setResult(formula, formula.getCreator().fixedPoint(formula.getFixedPoint(),
									formula.getVariable(), transformedChild));
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

					currentlyBoundVariables.remove(formula.getVariable(), 1);
					cachedSubstitutions.removeLast();

					Formula expansion = formula.getExpansion();
					Formula transformedExpansion = getCache(expansion);

					if (child.equals(transformedChild) && expansion.equals(transformedExpansion))
						setResult(formula, formula);
					else
						setResult(formula, formula.getCreator().let(formula.getVariable(),
									transformedExpansion, transformedChild));
				}
			});
			engine.enqueue(new FillCache(formula.getFormula()));
			engine.enqueue(new NonRecursive.Walker() {
				@Override
				public void walk(NonRecursive engine) {
					currentlyBoundVariables.add(formula.getVariable());
					cachedSubstitutions.addLast(new HashMap<Formula, Formula>());
				}
			});
			if (getCache(formula.getExpansion()) == null)
				engine.enqueue(new FillCache(formula.getExpansion()));
		}
	}

	public Formula transform(Formula formula) {
		Formula result = getCache(formula);
		if (result == null) {
			new NonRecursive().run(new FillCache(formula));
			result = getCache(formula);
		}
		return result;
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
