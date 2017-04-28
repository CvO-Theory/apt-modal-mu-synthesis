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

import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.ConjunctionFormula;
import uniol.synthesis.adt.mu_calculus.DisjunctionFormula;
import uniol.synthesis.adt.mu_calculus.NegationFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;

public abstract class RecursiveFormulaWalker {
	public void walk(Formula formula) {
		new NonRecursive().run(new RecursiveWalker(formula));
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

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
