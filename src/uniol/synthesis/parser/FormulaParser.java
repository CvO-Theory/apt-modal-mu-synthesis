package uniol.synthesis.parser;

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

import uniol.synthesis.adt.mu_calculus.Event;
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
			Formula formula = formulas.get(ctx.term(0));
			ParseTree right = ctx.term(1);
			if (right != null)
				formula = creator.disjunction(formula, formulas.get(right));
			formulas.put(ctx, formula);
		}

		@Override
		public void exitTermConjunction(MuCalculusFormulaParser.TermConjunctionContext ctx) {
			Formula formula = formulas.get(ctx.term(0));
			ParseTree right = ctx.term(1);
			if (right != null)
				formula = creator.conjunction(formula, formulas.get(right));
			formulas.put(ctx, formula);
		}

		@Override
		public void exitTermNegation(MuCalculusFormulaParser.TermNegationContext ctx) {
			Formula formula = formulas.get(ctx.term());
			formula = creator.negate(formula);
			formulas.put(ctx, formula);
		}

		@Override
		public void exitTermVariable(MuCalculusFormulaParser.TermVariableContext ctx) {
			String var = ctx.IDENTIFIER().getText();
			Formula formula = creator.variable(var);
			formulas.put(ctx, formula);
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
			formulas.put(ctx, creator.modality(mod, new Event(event), formulas.get(inner)));
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
		public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
				String msg, RecognitionException e) {
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