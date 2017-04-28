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

import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.ConjunctionFormula;
import uniol.synthesis.adt.mu_calculus.DisjunctionFormula;
import uniol.synthesis.adt.mu_calculus.NegationFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;

public class PositiveFormFormulaTransformer extends FormulaTransformer {
	private boolean negated;

	@Override
	public void reset() {
		super.reset();
		negated = false;
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

	static public Formula positiveForm(Formula formula) {
		return new PositiveFormFormulaTransformer().transform(formula);
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
