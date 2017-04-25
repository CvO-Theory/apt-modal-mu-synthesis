package uniol.synthesis.parser;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import uniol.synthesis.adt.mu_calculus.Event;
import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
import uniol.synthesis.experiment.ParseException;
import uniol.synthesis.experiment.Parser;
import uniol.synthesis.util.FormulaCreator;

public class ParserTest {
	private FormulaCreator creator = new FormulaCreator();

	@Test
	public void testVariable() throws ParseException {
		Formula expected = creator.variable("X");
		assertThat(Parser.parse(creator, "X"), equalTo(expected));
	}

	@Test
	public void testNegation() throws ParseException {
		Formula expected = creator.negate(creator.constant(false));
		assertThat(Parser.parse(creator, "!false"), equalTo(expected));
	}

	@Test
	public void testComplexFormula1() throws ParseException {
		Formula expected = creator.fixedPoint(FixedPoint.GREATEST, creator.variable("Z"),
				creator.fixedPoint(FixedPoint.LEAST, creator.variable("Y"),
					creator.modality(Modality.UNIVERSAL, new Event("a"),
						creator.disjunction(
							creator.conjunction(creator.modality(Modality.EXISTENTIAL,
									new Event("b"), creator.constant(true)),
								creator.variable("Z")),
							creator.variable("Y")))));
		assertThat(Parser.parse(creator, "nu Z.mu Y.[a]((<b>true&&Z)||Y)"), equalTo(expected));
	}

	@Test
	public void testComplexFormula2() throws ParseException {
		Formula expected = creator.fixedPoint(FixedPoint.LEAST, creator.variable("Y"),
				creator.fixedPoint(FixedPoint.GREATEST, creator.variable("Z"),
					creator.modality(Modality.UNIVERSAL, new Event("a"),
						creator.conjunction(
							creator.disjunction(creator.modality(Modality.EXISTENTIAL, new
									Event("b"), creator.constant(true)),
								creator.variable("Z")),
							creator.variable("Y")))));
		assertThat(Parser.parse(creator, "mu Y.nu Z.[a]((<b>true||Z)&&Y)"), equalTo(expected));
	}

	@Test
	public void testComplexFormula3() throws ParseException {
		Formula expected = creator.conjunction(creator.variable("X"), creator.negate(
					creator.fixedPoint(FixedPoint.GREATEST, creator.variable("X"),
						creator.fixedPoint(FixedPoint.LEAST, creator.variable("X"),
							creator.disjunction(creator.conjunction(creator.variable("X"),
									creator.variable("Y")),
								creator.modality(Modality.UNIVERSAL, new Event("a"),
									creator.constant(true)))))));
		assertThat(Parser.parse(creator, "X&&!nu X.mu X.X&&Y||[a]true"), equalTo(expected));
	}

	@Test
	public void testComplexFormula4() throws ParseException {
		VariableFormula x = creator.variable("X");
		Formula expected = creator.conjunction(creator.conjunction(x,
					creator.fixedPoint(FixedPoint.GREATEST, x,
						creator.conjunction(creator.conjunction(x,
								creator.fixedPoint(FixedPoint.GREATEST, x, x)),
							x))), x);
		assertThat(Parser.parse(creator, "X&&(nuX.X&&(nuX.X)&&X)&&X"), equalTo(expected));
	}

	@Test
	public void testMyFavoriteExample() throws ParseException {
		Modality ex = Modality.EXISTENTIAL;
		Event a = new Event("a");
		Event b = new Event("b");
		Event c = new Event("c");
		Formula expected = creator.fixedPoint(FixedPoint.GREATEST, creator.variable("X"), creator.conjunction(
				creator.modality(ex, a, creator.modality(ex, b, creator.modality(ex, c,
							creator.constant(true)))),
				creator.modality(ex, b, creator.modality(ex, a, creator.modality(Modality.UNIVERSAL, c,
							creator.variable("X"))))));
		assertThat(Parser.parse(creator, "nu X. <a><b><c>true && <b><a>[c]X"), equalTo(expected));
	}

	@Test
	public void testUnicode() throws ParseException {
		Formula expected = creator.fixedPoint(FixedPoint.LEAST, creator.variable("X"),
				creator.fixedPoint(FixedPoint.GREATEST, creator.variable("X"), creator.variable("X")));
		assertThat(Parser.parse(creator, "µX.νX.X"), equalTo(expected));
	}

	@Test(expectedExceptions = ParseException.class)
	public void testGarbageAfterEnd() throws ParseException {
		Parser.parse(creator, "X X");
	}

	@Test(expectedExceptions = ParseException.class)
	public void testMissingParenthesis() throws ParseException {
		Parser.parse(creator, "(X");
	}

}
