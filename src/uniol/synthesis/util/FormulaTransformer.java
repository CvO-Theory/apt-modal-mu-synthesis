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

import java.util.Deque;
import java.util.ArrayDeque;

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

public class FormulaTransformer extends RecursiveFormulaWalker {
	private final Deque<Formula> resultStack = new ArrayDeque<>();
	private FormulaCreator creator;

	public void reset() {
	}

	public Formula transform(Formula formula) {
		resultStack.clear();
		creator = formula.getCreator();
		reset();

		walk(formula);

		assert resultStack.size() == 1;
		return resultStack.removeLast();
	}

	public FormulaCreator getCreator() {
		return creator;
	}

	private void setResult(Formula formula) {
		resultStack.addLast(formula);
	}

	private Formula getResult() {
		return resultStack.removeLast();
	}

	protected Formula transform(ConstantFormula formula) {
		return formula;
	}

	protected Formula transform(ConjunctionFormula formula) {
		return formula;
	}

	protected Formula transform(DisjunctionFormula formula) {
		return formula;
	}

	protected Formula transform(NegationFormula formula) {
		return formula;
	}

	protected Formula transform(VariableFormula formula) {
		return formula;
	}

	protected Formula transform(ModalityFormula formula) {
		return formula;
	}

	protected Formula transform(FixedPointFormula formula) {
		return formula;
	}

	protected Formula transform(LetFormula formula) {
		return formula;
	}

	@Override
	public void visit(NonRecursive engine, ConstantFormula formula) {
		setResult(transform(formula));
	}

	@Override
	public void visit(NonRecursive engine, VariableFormula formula) {
		setResult(transform(formula));
	}

	@Override
	public void enter(NonRecursive engine, ConjunctionFormula formula) {
	}

	@Override
	public void exit(NonRecursive engine, ConjunctionFormula formula) {
		Formula left = getResult();
		Formula right = getResult();
		ConjunctionFormula result = formula;
		if (!result.getLeft().equals(left) || !result.getRight().equals(right))
			result = creator.conjunction(left, right);
		setResult(transform(result));
	}

	@Override
	public void enter(NonRecursive engine, DisjunctionFormula formula) {
	}

	@Override
	public void exit(NonRecursive engine, DisjunctionFormula formula) {
		Formula left = getResult();
		Formula right = getResult();
		DisjunctionFormula result = formula;
		if (!result.getLeft().equals(left) || !result.getRight().equals(right))
			result = creator.disjunction(left, right);
		setResult(transform(result));
	}

	@Override
	public void enter(NonRecursive engine, NegationFormula formula) {
	}

	@Override
	public void exit(NonRecursive engine, NegationFormula formula) {
		Formula innerFormula = getResult();
		NegationFormula result = formula;
		if (!result.getFormula().equals(innerFormula))
			result = creator.negate(innerFormula);
		setResult(transform(result));
	}

	@Override
	public void enter(NonRecursive engine, ModalityFormula formula) {
	}

	@Override
	public void exit(NonRecursive engine, ModalityFormula formula) {
		Formula innerFormula = getResult();
		ModalityFormula result = formula;
		if (!formula.getFormula().equals(innerFormula))
			result = creator.modality(formula.getModality(), formula.getEvent(), innerFormula);
		setResult(transform(result));
	}

	@Override
	public void enter(NonRecursive engine, FixedPointFormula formula) {
	}

	@Override
	public void exit(NonRecursive engine, FixedPointFormula formula) {
		Formula innerFormula = getResult();
		FixedPointFormula result = formula;
		if (!formula.getFormula().equals(innerFormula))
			result = creator.fixedPoint(formula.getFixedPoint(), formula.getVariable(), innerFormula);
		setResult(transform(result));
	}

	@Override
	public void enter(NonRecursive engine, LetFormula formula) {
	}

	@Override
	protected void exitExpansion(NonRecursive engine, LetFormula formula) {
	}

	@Override
	public void exit(NonRecursive engine, LetFormula formula) {
		Formula innerFormula = getResult();
		Formula expansion = getResult();
		LetFormula result = formula;
		if (!formula.getFormula().equals(innerFormula) || !formula.getExpansion().equals(expansion))
			result = creator.let(formula.getVariable(), expansion, innerFormula);
		setResult(transform(result));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
