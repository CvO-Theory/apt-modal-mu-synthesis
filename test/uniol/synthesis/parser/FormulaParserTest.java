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

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import uniol.apt.io.parser.ParseException;

import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class FormulaParserTest {
	private FormulaCreator creator = new FormulaCreator();

	@Test
	public void testVariable() throws ParseException {
		Formula expected = creator.variable("X");
		assertThat(FormulaParser.parse(creator, "X"), equalTo(expected));
	}

	@Test
	public void testNegation() throws ParseException {
		Formula expected = creator.negate(creator.constant(false));
		assertThat(FormulaParser.parse(creator, "!false"), equalTo(expected));
	}

	@Test
	public void testComplexFormula1() throws ParseException {
		Formula expected = creator.fixedPoint(FixedPoint.GREATEST, creator.variable("Z"),
				creator.fixedPoint(FixedPoint.LEAST, creator.variable("Y"),
					creator.modality(Modality.UNIVERSAL, "a",
						creator.disjunction(
							creator.conjunction(creator.modality(Modality.EXISTENTIAL,
									"b", creator.constant(true)),
								creator.variable("Z")),
							creator.variable("Y")))));
		assertThat(FormulaParser.parse(creator, "nu Z.mu Y.[a]((<b>true&&Z)||Y)"), equalTo(expected));
	}

	@Test
	public void testComplexFormula2() throws ParseException {
		Formula expected = creator.fixedPoint(FixedPoint.LEAST, creator.variable("Y"),
				creator.fixedPoint(FixedPoint.GREATEST, creator.variable("Z"),
					creator.modality(Modality.UNIVERSAL, "a",
						creator.conjunction(
							creator.disjunction(creator.modality(Modality.EXISTENTIAL, "b",
									creator.constant(true)),
								creator.variable("Z")),
							creator.variable("Y")))));
		assertThat(FormulaParser.parse(creator, "mu Y.nu Z.[a]((<b>true||Z)&&Y)"), equalTo(expected));
	}

	@Test
	public void testComplexFormula3() throws ParseException {
		Formula expected = creator.conjunction(creator.variable("X"), creator.negate(
					creator.fixedPoint(FixedPoint.GREATEST, creator.variable("X"),
						creator.fixedPoint(FixedPoint.LEAST, creator.variable("X"),
							creator.disjunction(creator.conjunction(creator.variable("X"),
									creator.variable("Y")),
								creator.modality(Modality.UNIVERSAL, "a",
									creator.constant(true)))))));
		assertThat(FormulaParser.parse(creator, "X&&!nu X.mu X.X&&Y||[a]true"), equalTo(expected));
	}

	@Test
	public void testAssociativityConjunction1() throws ParseException {
		VariableFormula x = creator.variable("X");
		Formula expected = creator.conjunction(creator.conjunction(x,
					creator.fixedPoint(FixedPoint.GREATEST, x,
						creator.conjunction(creator.conjunction(x,
								creator.fixedPoint(FixedPoint.GREATEST, x, x)),
							x))), x);
		assertThat(FormulaParser.parse(creator, "X&&(nu X.X&&(nu X.X)&&X)&&X"), equalTo(expected));
	}

	@Test
	public void testAssociativityConjunction2() throws ParseException {
		// Same as in previous test, but without the parentheses
		VariableFormula x = creator.variable("X");
		Formula expected = creator.conjunction(x, creator.fixedPoint(FixedPoint.GREATEST, x,
						creator.conjunction(x, creator.fixedPoint(FixedPoint.GREATEST, x,
								creator.conjunction(creator.conjunction(x, x), x)))));
		assertThat(FormulaParser.parse(creator, "X&&nu X.X&&nu X.X&&X&&X"), equalTo(expected));
	}

	@Test
	public void testAssociativityDisjunction1() throws ParseException {
		VariableFormula x = creator.variable("X");
		Formula expected = creator.disjunction(creator.disjunction(x,
					creator.fixedPoint(FixedPoint.GREATEST, x,
						creator.disjunction(creator.disjunction(x,
								creator.fixedPoint(FixedPoint.GREATEST, x, x)),
							x))), x);
		assertThat(FormulaParser.parse(creator, "X||(nu X.X||(nu X.X)||X)||X"), equalTo(expected));
	}

	@Test
	public void testAssociativityDisjunction2() throws ParseException {
		VariableFormula x = creator.variable("X");
		Formula expected = creator.disjunction(x, creator.fixedPoint(FixedPoint.GREATEST, x,
						creator.disjunction(x, creator.fixedPoint(FixedPoint.GREATEST, x,
								creator.disjunction(creator.disjunction(x, x), x)))));
		assertThat(FormulaParser.parse(creator, "X||nu X.X||nu X.X||X||X"), equalTo(expected));
	}

	@Test
	public void testAssociativityLet() throws ParseException {
		VariableFormula x = creator.variable("X");
		Formula expected = creator.let(x, creator.let(x, x, x), creator.let(x, x, x));
		assertThat(FormulaParser.parse(creator, "let X = let X = X in X in let X=X in X"), equalTo(expected));
	}

	@Test
	public void testPrecedence1() throws ParseException {
		VariableFormula x = creator.variable("X");
		Formula expected = creator.disjunction(x, creator.conjunction(x, x));
		assertThat(FormulaParser.parse(creator, "X||X&&X"), equalTo(expected));
	}

	@Test
	public void testPrecedence2() throws ParseException {
		VariableFormula x = creator.variable("X");
		Formula expected = creator.disjunction(creator.conjunction(x, x), x);
		assertThat(FormulaParser.parse(creator, "X&&X||X"), equalTo(expected));
	}

	@Test
	public void testPrecedence3() throws ParseException {
		// Test that muX.X||X is parsed as muX.(X||X)
		VariableFormula x = creator.variable("X");
		Formula expected = creator.fixedPoint(FixedPoint.LEAST, x, creator.disjunction(x, x));
		assertThat(FormulaParser.parse(creator, "mu X.X||X"), equalTo(expected));
	}

	@Test
	public void testMyFavoriteExample() throws ParseException {
		Modality ex = Modality.EXISTENTIAL;
		Formula expected = creator.fixedPoint(FixedPoint.GREATEST, creator.variable("X"), creator.conjunction(
				creator.modality(ex, "a", creator.modality(ex, "b", creator.modality(ex, "c",
							creator.constant(true)))),
				creator.modality(ex, "b", creator.modality(ex, "a", creator.modality(Modality.UNIVERSAL,
							"c", creator.variable("X"))))));
		assertThat(FormulaParser.parse(creator, "nu X. <a><b><c>true && <b><a>[c]X"), equalTo(expected));
	}

	@Test
	public void testUnicode() throws ParseException {
		Formula expected = creator.fixedPoint(FixedPoint.LEAST, creator.variable("X"),
				creator.fixedPoint(FixedPoint.GREATEST, creator.variable("X"), creator.variable("X")));
		assertThat(FormulaParser.parse(creator, "µX.νX.X"), equalTo(expected));
	}

	@Test(expectedExceptions = ParseException.class)
	public void testGarbageAfterEnd1() throws ParseException {
		FormulaParser.parse(creator, "X X");
	}

	@Test(expectedExceptions = ParseException.class)
	public void testGarbageAfterEnd2() throws ParseException {
		FormulaParser.parse(creator, "(X) X");
	}

	@Test(expectedExceptions = ParseException.class)
	public void testMissingParenthesis() throws ParseException {
		FormulaParser.parse(creator, "(X");
	}

	@Test(expectedExceptions = ParseException.class)
	public void testWildClosingParanthesis() throws ParseException {
		FormulaParser.parse(creator, "X&&)");
	}

	@Test(expectedExceptions = ParseException.class)
	public void testInvalidCharacter() throws ParseException {
		FormulaParser.parse(creator, "\u2622");
	}

	@Test
	public void testFunctionCallNoArgs() throws ParseException {
		Formula expected = creator.call("X");
		assertThat(FormulaParser.parse(creator, "X()"), equalTo(expected));
	}

	@Test
	public void testFunctionCallOneArg() throws ParseException {
		Formula expected = creator.call("X", creator.variable("X"));
		assertThat(FormulaParser.parse(creator, "X(X)"), equalTo(expected));
	}

	@Test
	public void testFunctionCallManyArgs() throws ParseException {
		Formula x = creator.variable("X");
		Formula y = creator.variable("Y");
		Formula expected = creator.call("X", x, x, x, y);
		assertThat(FormulaParser.parse(creator, "X(X,X,X,Y)"), equalTo(expected));
	}

	@Test(expectedExceptions = ParseException.class)
	public void testInvalidCall() throws ParseException {
		FormulaParser.parse(creator, "X(X,)");
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
