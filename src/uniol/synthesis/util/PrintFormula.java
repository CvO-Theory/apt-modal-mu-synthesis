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

import java.io.IOException;
import java.util.List;

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

public class PrintFormula extends FormulaWalker {
	private final Appendable sb;

	private PrintFormula(Appendable sb, Formula formula) {
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
		enqueue(engine, formula.getVariable());
		enqueue(engine, " ");
		enqueue(engine, formula.getFixedPoint().toString());
		enqueue(engine, "(");
	}

	@Override
	public void walk(NonRecursive engine, LetFormula formula) {
		enqueue(engine, ")");
		enqueue(engine, formula.getFormula());
		enqueue(engine, " in ");
		enqueue(engine, formula.getExpansion());
		enqueue(engine, " = ");
		enqueue(engine, formula.getVariable());
		enqueue(engine, "(let ");
	}

	@Override
	public void walk(NonRecursive engine, CallFormula formula) {
		enqueue(engine, ")");
		List<Formula> arguments = formula.getArguments();
		for (int index = arguments.size() - 1; index > 0; index--) {
			enqueue(engine, arguments.get(index));
			enqueue(engine, ", ");
		}
		if (!arguments.isEmpty())
			enqueue(engine, arguments.get(0));
		enqueue(engine, "(");
		enqueue(engine, formula.getFunction());
	}

	private void enqueue(NonRecursive engine, Formula formula) {
		engine.enqueue(new PrintFormula(sb, formula));
	}

	private void enqueue(NonRecursive engine, final String string) {
		engine.enqueue(new NonRecursive.Walker() {
			@Override
			public void walk(NonRecursive engine) {
				try {
					sb.append(string);
				} catch (IOException e) {
					throw new RuntimeIOException(e);
				}
			}
		});
	}

	static public void printFormula(StringBuilder sb, Formula formula) {
		new NonRecursive().run(new PrintFormula(sb, formula));
	}

	static public void printFormula(Appendable sb, Formula formula) throws IOException {
		try {
			new NonRecursive().run(new PrintFormula(sb, formula));
		} catch (RuntimeIOException e) {
			throw e.getCause();
		}
	}

	static private class RuntimeIOException extends RuntimeException {
		public static final long serialVersionUID = 0;

		public RuntimeIOException(IOException e) {
			super(e);
		}

		@Override
		public IOException getCause() {
			return (IOException) super.getCause();
		}
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
