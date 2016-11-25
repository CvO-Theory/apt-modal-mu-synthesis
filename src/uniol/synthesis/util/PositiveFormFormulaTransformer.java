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
	private boolean negated = false;

	public PositiveFormFormulaTransformer(Formula formula) {
		super(formula);
	}

	@Override
	public void enter(NonRecursive engine, NegationFormula formula) {
		super.enter(engine, formula);
		negated = !negated;
	}

	@Override
	public void exit(NonRecursive engine, NegationFormula formula) {
		super.enter(engine, formula);
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
		return getCreator().fixedPoint(formula.getFixedPoint().negate(), formula.getVariable(), formula.getFormula());
	}

	static public Formula positiveForm(Formula formula) {
		PositiveFormFormulaTransformer positive = new PositiveFormFormulaTransformer(formula);
		new NonRecursive().run(positive);
		return positive.getTransformedFormula();
	}
}
