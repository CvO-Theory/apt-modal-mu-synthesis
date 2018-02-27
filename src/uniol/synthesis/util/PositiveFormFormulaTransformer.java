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

import java.util.List;

import uniol.synthesis.adt.mu_calculus.ConjunctionFormula;
import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.DisjunctionFormula;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.NegationFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class PositiveFormFormulaTransformer {
	private final FormulaFormulaTransformer positiveFormTransformer = new FormulaFormulaTransformer() {
		@Override
		protected void enqueueWalker(NonRecursive engine, Formula formula) {
			engine.enqueue(new FormulaFormulaTransformer.FillCache(formula) {
				@Override
				public void walk(NonRecursive engine, NegationFormula formula) {
					Formula child = formula.getFormula();
					Formula transformedChild = negativeFormTransformer.getCache(child);
					if (transformedChild == null) {
						engine.enqueue(this);
						negativeFormTransformer.enqueueWalker(engine, child);
						return;
					}
					setCache(formula, transformedChild);
				}
			});
		}
	};
	private final FormulaTransformer<Formula> negativeFormTransformer = new FormulaTransformer<Formula>() {
		@Override
		protected void enqueueWalker(NonRecursive engine, Formula formula) {
			engine.enqueue(new FormulaTransformer<Formula>.FillCache(formula) {
				@Override
				public void walk(NonRecursive engine, ConstantFormula formula) {
					setCache(formula, formula.getCreator().constant(!formula.getValue()));
				}

				@Override
				public void walk(NonRecursive engine, VariableFormula formula) {
					if (isCurrentlyBound(formula))
						// Must be bound by a fixed point, so do not negate
						setCache(formula, formula);
					else
						setCache(formula, formula.getCreator().negate(formula));
				}

				@Override
				public void walk(NonRecursive engine, NegationFormula formula) {
					Formula child = formula.getFormula();
					Formula transformedChild = positiveFormTransformer.getCache(child);
					if (transformedChild == null) {
						engine.enqueue(this);
						positiveFormTransformer.enqueueWalker(engine, child);
						return;
					}
					setCache(formula, transformedChild);
				}

				@Override
				public Formula conjunction(ConjunctionFormula formula, List<Formula> transformedChildren) {
					return formula.getCreator().disjunction(transformedChildren);
				}

				@Override
				public Formula disjunction(DisjunctionFormula formula, List<Formula> transformedChildren) {
					return formula.getCreator().conjunction(transformedChildren);
				}

				@Override
				public Formula negate(NegationFormula formula, Formula transformedChild) {
					throw new AssertionError("This function should not be called");
				}

				@Override
				public Formula modality(ModalityFormula formula, Formula transformedChild) {
					return formula.getCreator().modality(formula.getModality().negate(),
							formula.getEvent(), transformedChild);
				}

				@Override
				public Formula fixedPoint(FixedPointFormula formula, Formula transformedChild) {
					return formula.getCreator().fixedPoint(formula.getFixedPoint().negate(),
							formula.getVariable(), transformedChild);
				}
			});
		}
	};

	public Formula transform(Formula formula) {
		NonRecursive engine = new NonRecursive();
		Formula result = positiveFormTransformer.transform(engine, formula);
		if (result == null) {
			engine.run();
			result = positiveFormTransformer.transform(engine, formula);
			assert result != null;
		}
		return result;
	}

	static public Formula positiveForm(Formula formula) {
		return new PositiveFormFormulaTransformer().transform(formula);
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
