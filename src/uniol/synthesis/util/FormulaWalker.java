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

public abstract class FormulaWalker implements NonRecursive.Walker {
	final protected Formula formula;

	public FormulaWalker(Formula formula) {
		this.formula = formula;
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
}
