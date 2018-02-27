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

import uniol.synthesis.adt.mu_calculus.CallFormula;
import uniol.synthesis.adt.mu_calculus.ConjunctionFormula;
import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.DisjunctionFormula;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.LetFormula;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.NegationFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public abstract class FormulaWalker implements NonRecursive.Walker {
	final private Formula formula;

	public FormulaWalker(Formula formula) {
		assert formula != null;
		this.formula = formula;
	}

	public Formula getFormula() {
		return formula;
	}

	@Override
	final public void walk(NonRecursive engine) {
		if (formula instanceof ConstantFormula) {
			walk(engine, (ConstantFormula) formula);
		} else if (formula instanceof ConjunctionFormula) {
			walk(engine, (ConjunctionFormula) formula);
		} else if (formula instanceof DisjunctionFormula) {
			walk(engine, (DisjunctionFormula) formula);
		} else if (formula instanceof NegationFormula) {
			walk(engine, (NegationFormula) formula);
		} else if (formula instanceof VariableFormula) {
			walk(engine, (VariableFormula) formula);
		} else if (formula instanceof ModalityFormula) {
			walk(engine, (ModalityFormula) formula);
		} else if (formula instanceof FixedPointFormula) {
			walk(engine, (FixedPointFormula) formula);
		} else if (formula instanceof LetFormula) {
			walk(engine, (LetFormula) formula);
		} else if (formula instanceof CallFormula) {
			walk(engine, (CallFormula) formula);
		} else {
			throw new AssertionError("Unknown subclass of formula: " + formula.getClass());
		}
	}

	public abstract void walk(NonRecursive engine, ConstantFormula formula);
	public abstract void walk(NonRecursive engine, ConjunctionFormula formula);
	public abstract void walk(NonRecursive engine, DisjunctionFormula formula);
	public abstract void walk(NonRecursive engine, NegationFormula formula);
	public abstract void walk(NonRecursive engine, VariableFormula formula);
	public abstract void walk(NonRecursive engine, ModalityFormula formula);
	public abstract void walk(NonRecursive engine, FixedPointFormula formula);
	public abstract void walk(NonRecursive engine, LetFormula formula);

	public void walk(NonRecursive engine, CallFormula call) {
		throw new AssertionError("CallFormula not supported, but got: " + call);
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
