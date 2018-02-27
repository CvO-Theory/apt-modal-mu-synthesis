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

public abstract class FormulaFormulaTransformer extends FormulaTransformer<Formula> {
	protected abstract class FillCache extends FormulaTransformer<Formula>.FillCache {
		public FillCache(Formula formula) {
			super(formula);
		}

		@Override
		public void walk(NonRecursive engine, ConstantFormula formula) {
			setCache(formula, formula);
		}

		@Override
		public void walk(NonRecursive engine, VariableFormula formula) {
			setCache(formula, formula);
		}

		@Override
		public Formula conjunction(ConjunctionFormula formula, List<Formula> transformed) {
			assert formula.getFormulas().size() == transformed.size();
			for (int i = 0; i < transformed.size(); i++) {
				if (!formula.getFormulas().get(i).equals(transformed.get(i)))
					return formula.getCreator().conjunction(transformed);
			}
			return formula;
		}

		@Override
		public Formula disjunction(DisjunctionFormula formula, List<Formula> transformed) {
			assert formula.getFormulas().size() == transformed.size();
			for (int i = 0; i < transformed.size(); i++) {
				if (!formula.getFormulas().get(i).equals(transformed.get(i)))
					return formula.getCreator().disjunction(transformed);
			}
			return formula;
		}

		@Override
		public Formula negate(NegationFormula formula, Formula transformedChild) {
			if (formula.getFormula().equals(transformedChild))
				return formula;
			return formula.getCreator().negate(transformedChild);
		}

		@Override
		public Formula modality(ModalityFormula formula, Formula transformedChild) {
			if (formula.getFormula().equals(transformedChild))
				return formula;
			return formula.getCreator().modality(formula.getModality(), formula.getEvent(),
					transformedChild);
		}

		@Override
		public Formula fixedPoint(FixedPointFormula formula, Formula transformedChild) {
			if (formula.getFormula().equals(transformedChild))
				return formula;
			return formula.getCreator().fixedPoint(formula.getFixedPoint(), formula.getVariable(),
					transformedChild);
		}
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
