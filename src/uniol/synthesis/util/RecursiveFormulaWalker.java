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

public abstract class RecursiveFormulaWalker implements NonRecursive.Walker {
	final protected Formula startFormula;

	public RecursiveFormulaWalker(Formula formula) {
		this.startFormula = formula;
	}

	@Override
	final public void walk(NonRecursive engine) {
		engine.enqueue(new RecursiveWalker(startFormula));
	}

	private class RecursiveWalker extends FormulaWalker {
		private boolean enter = true;

		public RecursiveWalker(Formula formula) {
			super(formula);
		}

		@Override
		public void walk(NonRecursive engine, ConstantFormula formula) {
			RecursiveFormulaWalker.this.visit(engine, formula);
		}

		@Override
		public void walk(NonRecursive engine, VariableFormula formula) {
			RecursiveFormulaWalker.this.visit(engine, formula);
		}

		@Override
		public void walk(NonRecursive engine, ConjunctionFormula formula) {
			if (enter) {
				enter = false;
				RecursiveFormulaWalker.this.enter(engine, formula);
				engine.enqueue(this);
				engine.enqueue(new RecursiveWalker(formula.getLeft()));
				engine.enqueue(new RecursiveWalker(formula.getRight()));
			} else {
				RecursiveFormulaWalker.this.exit(engine, formula);
			}
		}

		@Override
		public void walk(NonRecursive engine, DisjunctionFormula formula) {
			if (enter) {
				enter = false;
				RecursiveFormulaWalker.this.enter(engine, formula);
				engine.enqueue(this);
				engine.enqueue(new RecursiveWalker(formula.getLeft()));
				engine.enqueue(new RecursiveWalker(formula.getRight()));
			} else {
				RecursiveFormulaWalker.this.exit(engine, formula);
			}
		}

		@Override
		public void walk(NonRecursive engine, NegationFormula formula) {
			if (enter) {
				enter = false;
				RecursiveFormulaWalker.this.enter(engine, formula);
				engine.enqueue(this);
				engine.enqueue(new RecursiveWalker(formula.getFormula()));
			} else {
				RecursiveFormulaWalker.this.exit(engine, formula);
			}
		}

		@Override
		public void walk(NonRecursive engine, ModalityFormula formula) {
			if (enter) {
				enter = false;
				RecursiveFormulaWalker.this.enter(engine, formula);
				engine.enqueue(this);
				engine.enqueue(new RecursiveWalker(formula.getFormula()));
			} else {
				RecursiveFormulaWalker.this.exit(engine, formula);
			}
		}

		@Override
		public void walk(NonRecursive engine, FixedPointFormula formula) {
			if (enter) {
				enter = false;
				RecursiveFormulaWalker.this.enter(engine, formula);
				engine.enqueue(this);
				engine.enqueue(new RecursiveWalker(formula.getFormula()));
			} else {
				RecursiveFormulaWalker.this.exit(engine, formula);
			}
		}
	}

	protected void visit(NonRecursive engine, ConstantFormula formula) {
	}

	protected void visit(NonRecursive engine, VariableFormula formula) {
	}

	protected void enter(NonRecursive engine, ConjunctionFormula formula) {
	}

	protected void exit(NonRecursive engine, ConjunctionFormula formula) {
	}

	protected void enter(NonRecursive engine, DisjunctionFormula formula) {
	}

	protected void exit(NonRecursive engine, DisjunctionFormula formula) {
	}

	protected void enter(NonRecursive engine, NegationFormula formula) {
	}

	protected void exit(NonRecursive engine, NegationFormula formula) {
	}

	protected void enter(NonRecursive engine, ModalityFormula formula) {
	}

	protected void exit(NonRecursive engine, ModalityFormula formula) {
	}

	protected void enter(NonRecursive engine, FixedPointFormula formula) {
	}

	protected void exit(NonRecursive engine, FixedPointFormula formula) {
	}
}
