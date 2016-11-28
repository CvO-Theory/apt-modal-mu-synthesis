package uniol.synthesis.util;

import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.ConjunctionFormula;
import uniol.synthesis.adt.mu_calculus.DisjunctionFormula;
import uniol.synthesis.adt.mu_calculus.NegationFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;

public class PrintFormula extends FormulaWalker {
	private final StringBuilder sb;

	private PrintFormula(StringBuilder sb, Formula formula) {
		super(formula);
		this.sb = sb;
	}

	@Override
	public void walk(NonRecursive engine, ConstantFormula formula) {
		if (formula.getValue())
			enqueue(engine, "true");
		else
			enqueue(engine, "false");
	}

	@Override
	public void walk(NonRecursive engine, ConjunctionFormula formula) {
		enqueue(engine, ")");
		enqueue(engine, formula.getRight());
		enqueue(engine, "&&");
		enqueue(engine, formula.getLeft());
		enqueue(engine, "(");
	}

	@Override
	public void walk(NonRecursive engine, DisjunctionFormula formula) {
		enqueue(engine, ")");
		enqueue(engine, formula.getRight());
		enqueue(engine, "||");
		enqueue(engine, formula.getLeft());
		enqueue(engine, "(");
	}

	@Override
	public void walk(NonRecursive engine, NegationFormula formula) {
		enqueue(engine, ")");
		enqueue(engine, formula.getFormula());
		enqueue(engine, "(!");
	}

	@Override
	public void walk(NonRecursive engine, VariableFormula formula) {
		enqueue(engine, formula.getVariable().toString());
	}

	@Override
	public void walk(NonRecursive engine, ModalityFormula formula) {
		enqueue(engine, formula.getFormula());
		enqueue(engine, formula.getModality().toString(formula.getEvent()));
	}

	@Override
	public void walk(NonRecursive engine, FixedPointFormula formula) {
		enqueue(engine, ")");
		enqueue(engine, formula.getFormula());
		enqueue(engine, ".");
		enqueue(engine, formula.getVariable().toString());
		enqueue(engine, formula.getFixedPoint().toString());
		enqueue(engine, "(");
	}

	private void enqueue(NonRecursive engine, Formula formula) {
		engine.enqueue(new PrintFormula(sb, formula));
	}

	private void enqueue(NonRecursive engine, final String string) {
		engine.enqueue(new NonRecursive.Walker() {
			@Override
			public void walk(NonRecursive engine) {
				sb.append(string);
			}
		});
	}

	static public void printFormula(StringBuilder sb, Formula formula) {
		new NonRecursive().run(new PrintFormula(sb, formula));
	}
}
