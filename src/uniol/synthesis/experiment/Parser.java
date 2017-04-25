package uniol.synthesis.experiment;

import uniol.synthesis.adt.mu_calculus.Event;
import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
import uniol.synthesis.util.FormulaCreator;

public class Parser {
	final private FormulaCreator creator;
	private String str;
	private char lookAhead;
	private int offset;

	private static final char EOF = Character.MAX_VALUE;

	private Parser(FormulaCreator creator) {
		this.creator = creator;
	}

	static public Formula parse(FormulaCreator creator, String str) throws ParseException {
		return new Parser(creator).parse(str);
	}

	private Formula parse(String str) throws ParseException {
		this.str = str;
		this.offset = 0;

		nextLookAhead();
		Formula formula = parse();
		if (lookAhead != EOF)
			throw new ParseException("Garbage after end of formula");
		return formula;
	}

	private void nextLookAhead() {
		do {
			if (offset == str.length()) {
				lookAhead = EOF;
				return;
			}
			lookAhead = str.charAt(offset);
			offset++;
		} while (lookAhead == ' ');
	}

	private void expect(String expected) throws ParseException {
		for (int i = 0; i < expected.length(); i++) {
			char own = lookAhead;
			char other = expected.charAt(i);
			if (own != other)
				throw new ParseException("Expected " + other + " but found " + own + " while expecting " + expected + " at offset " + offset);
			nextLookAhead();
		}
	}

	private Formula parse() throws ParseException {
		return parseDisjunction();
	}

	private Formula parseDisjunction() throws ParseException {
		Formula result = parseConjunction();
		while (lookAhead == '|') {
			expect("||");
			result = creator.disjunction(result, parseConjunction());
		}
		return result;
	}

	private Formula parseConjunction() throws ParseException {
		Formula result = parseAtomOrUnary();
		while (lookAhead == '&') {
			expect("&&");
			result = creator.conjunction(result, parseAtomOrUnary());
		}
		return result;
	}

	private Formula parseAtomOrUnary() throws ParseException {
		switch (lookAhead) {
			case 'm':
			case 'µ':
				return parseLeastFixedPoint();
			case 'n':
			case 'ν':
				return parseGreatestFixedPoint();
			case 'f':
				return parseConstant("false", creator.constant(false));
			case 't':
				return parseConstant("true", creator.constant(true));
			case '<':
				return parseModality("<", ">", Modality.EXISTENTIAL);
			case '[':
				return parseModality("[", "]", Modality.UNIVERSAL);
			case '!':
				return parseNegation();
			case '(':
				expect("(");
				Formula result = parse();
				expect(")");
				return result;
			default:
				return parseVariable();
		}
	}

	private Formula parseLeastFixedPoint() throws ParseException {
		if (lookAhead == 'µ')
			expect("µ");
		else
			expect("mu");
		VariableFormula variable = parseVariable();
		expect(".");
		Formula formula = parse();
		return creator.fixedPoint(FixedPoint.LEAST, variable, formula);
	}

	private Formula parseGreatestFixedPoint() throws ParseException {
		if (lookAhead == 'ν')
			expect("ν");
		else
			expect("nu");
		VariableFormula variable = parseVariable();
		expect(".");
		Formula formula = parse();
		return creator.fixedPoint(FixedPoint.GREATEST, variable, formula);
	}

	private Formula parseConstant(String expected, Formula result) throws ParseException {
		expect(expected);
		return result;
	}

	private Formula parseNegation() throws ParseException {
		expect("!");
		Formula formula = parseAtomOrUnary();
		return creator.negate(formula);
	}

	private Formula parseModality(String begin, String end, Modality modality) throws ParseException {
		expect(begin);
		Event event = new Event(String.valueOf(lookAhead));
		nextLookAhead();
		expect(end);
		Formula formula = parseAtomOrUnary();
		return creator.modality(modality, event, formula);
	}

	private VariableFormula parseVariable() throws ParseException {
		String var = String.valueOf(lookAhead);
		nextLookAhead();
		return creator.variable(var);
	}
}
