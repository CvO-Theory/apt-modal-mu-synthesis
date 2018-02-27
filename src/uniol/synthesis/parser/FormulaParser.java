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

package uniol.synthesis.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import uniol.apt.io.parser.ParseException;

import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.Modality;

public class FormulaParser {
	private final FormulaCreator creator;

	public FormulaParser(FormulaCreator creator) {
		this.creator = creator;
	}

	private class FormulaListener extends MuCalculusFormulaBaseListener {
		private final ParseTreeProperty<Formula> formulas = new ParseTreeProperty<>();
		private Formula formula;

		private Formula getFormula() {
			return formula;
		}

		@Override
		public void exitFormula(MuCalculusFormulaParser.FormulaContext ctx) {
			formula = formulas.get(ctx.term());
		}

		@Override
		public void exitTermDisjunction(MuCalculusFormulaParser.TermDisjunctionContext ctx) {
			formulas.put(ctx, creator.disjunction(formulas.get(ctx.term(0)), formulas.get(ctx.term(1))));
		}

		@Override
		public void exitTermConjunction(MuCalculusFormulaParser.TermConjunctionContext ctx) {
			formulas.put(ctx, creator.conjunction(formulas.get(ctx.term(0)), formulas.get(ctx.term(1))));
		}

		@Override
		public void exitTermNegation(MuCalculusFormulaParser.TermNegationContext ctx) {
			Formula result = formulas.get(ctx.term());
			result = creator.negate(result);
			formulas.put(ctx, result);
		}

		@Override
		public void exitTermVariable(MuCalculusFormulaParser.TermVariableContext ctx) {
			String var = ctx.IDENTIFIER().getText();
			Formula result = creator.variable(var);
			formulas.put(ctx, result);
		}

		@Override
		public void exitTermParantheses(MuCalculusFormulaParser.TermParanthesesContext ctx) {
			formulas.put(ctx, formulas.get(ctx.term()));
		}

		@Override
		public void exitTermExistentialModality(MuCalculusFormulaParser.TermExistentialModalityContext ctx) {
			modality(ctx, Modality.EXISTENTIAL, ctx.IDENTIFIER().getText(), ctx.term());
		}

		@Override
		public void exitTermUniversalModality(MuCalculusFormulaParser.TermUniversalModalityContext ctx) {
			modality(ctx, Modality.UNIVERSAL, ctx.IDENTIFIER().getText(), ctx.term());
		}

		private void modality(MuCalculusFormulaParser.TermContext ctx, Modality mod, String event,
				MuCalculusFormulaParser.TermContext inner) {
			formulas.put(ctx, creator.modality(mod, event, formulas.get(inner)));
		}

		@Override
		public void exitTermLet(MuCalculusFormulaParser.TermLetContext ctx) {
			formulas.put(ctx, creator.let(creator.variable(ctx.IDENTIFIER().getText()),
							formulas.get(ctx.term(0)), formulas.get(ctx.term(1))));
		}

		@Override
		public void exitTermCall(MuCalculusFormulaParser.TermCallContext ctx) {
			MuCalculusFormulaParser.ArgumentsContext argsContext = ctx.arguments();
			List<Formula> args = Collections.emptyList();
			if (argsContext != null) {
				List<MuCalculusFormulaParser.TermContext> list = argsContext.term();
				args = new ArrayList<>(list.size());
				for (MuCalculusFormulaParser.TermContext tCtx : list) {
					args.add(formulas.get(tCtx));
				}
			}
			formulas.put(ctx, creator.call(ctx.IDENTIFIER().getText(), args));
		}

		@Override
		public void exitTermLeastFixedPoint(MuCalculusFormulaParser.TermLeastFixedPointContext ctx) {
			fixedPoint(ctx, FixedPoint.LEAST, ctx.IDENTIFIER().getText(), ctx.term());
		}

		@Override
		public void exitTermGreatestFixedPoint(MuCalculusFormulaParser.TermGreatestFixedPointContext ctx) {
			fixedPoint(ctx, FixedPoint.GREATEST, ctx.IDENTIFIER().getText(), ctx.term());
		}

		private void fixedPoint(MuCalculusFormulaParser.TermContext ctx, FixedPoint fp, String variable,
				MuCalculusFormulaParser.TermContext inner) {
			formulas.put(ctx, creator.fixedPoint(fp, creator.variable(variable), formulas.get(inner)));
		}

		@Override
		public void exitTermFalse(MuCalculusFormulaParser.TermFalseContext ctx) {
			formulas.put(ctx, creator.constant(false));
		}

		@Override
		public void exitTermTrue(MuCalculusFormulaParser.TermTrueContext ctx) {
			formulas.put(ctx, creator.constant(true));
		}
	}

	private static class ThrowingErrorListener extends BaseErrorListener {
		@Override
		public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
				int charPositionInLine, String msg, RecognitionException e) {
			throw new ParseRuntimeException("line " + line + " col " + charPositionInLine + ": " + msg, e);
		}
	}

	private static class ParseRuntimeException extends RuntimeException {
		public static final long serialVersionUID = 0;

		public ParseRuntimeException(String msg, Throwable cause) {
			super(msg, new ParseException(msg, cause));
		}

		public ParseException getParseException() {
			return (ParseException) getCause();
		}
	}

	public Formula parse(String str) throws ParseException {
		CharStream input = new ANTLRInputStream(str);
		MuCalculusFormulaLexer lexer = new MuCalculusFormulaLexer(input);

		// Don't spam errors to stderr, instead throw exceptions
		lexer.removeErrorListeners();
		lexer.addErrorListener(new ThrowingErrorListener());

		CommonTokenStream tokens = new CommonTokenStream(lexer);
		MuCalculusFormulaParser parser = new MuCalculusFormulaParser(tokens);
		parser.setBuildParseTree(true);

		// Don't spam errors to stderr, instead throw exceptions
		parser.removeErrorListeners();
		parser.addErrorListener(new ThrowingErrorListener());

		ParseTree tree;
		try {
			tree = parser.formula();
		} catch (ParseRuntimeException ex) {
			throw ex.getParseException();
		}
		FormulaListener listener = new FormulaListener();
		try {
			ParseTreeWalker.DEFAULT.walk(listener, tree);
		} catch (ParseRuntimeException ex) {
			throw ex.getParseException();
		}
		return listener.getFormula();
	}

	static public Formula parse(FormulaCreator creator, String str) throws ParseException {
		return new FormulaParser(creator).parse(str);
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
