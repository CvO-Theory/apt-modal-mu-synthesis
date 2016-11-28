package uniol.synthesis.util;

import java.util.Deque;
import java.util.ArrayDeque;

import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.ConjunctionFormula;
import uniol.synthesis.adt.mu_calculus.DisjunctionFormula;
import uniol.synthesis.adt.mu_calculus.NegationFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;

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
		ConjunctionFormula result = creator.conjunction(left, right);
		setResult(transform(result));
	}

	@Override
	public void enter(NonRecursive engine, DisjunctionFormula formula) {
	}

	@Override
	public void exit(NonRecursive engine, DisjunctionFormula formula) {
		Formula left = getResult();
		Formula right = getResult();
		DisjunctionFormula result = creator.disjunction(left, right);
		setResult(transform(result));
	}

	@Override
	public void enter(NonRecursive engine, NegationFormula formula) {
	}

	@Override
	public void exit(NonRecursive engine, NegationFormula formula) {
		Formula innerFormula = getResult();
		NegationFormula result = creator.negate(innerFormula);
		setResult(transform(result));
	}

	@Override
	public void enter(NonRecursive engine, ModalityFormula formula) {
	}

	@Override
	public void exit(NonRecursive engine, ModalityFormula formula) {
		Formula innerFormula = getResult();
		ModalityFormula result = creator.modality(formula.getModality(), formula.getEvent(), innerFormula);
		setResult(transform(result));
	}

	@Override
	public void enter(NonRecursive engine, FixedPointFormula formula) {
	}

	@Override
	public void exit(NonRecursive engine, FixedPointFormula formula) {
		Formula innerFormula = getResult();
		FixedPointFormula result = creator.fixedPoint(formula.getFixedPoint(), formula.getVariable(), innerFormula);
		setResult(transform(result));
	}
}
