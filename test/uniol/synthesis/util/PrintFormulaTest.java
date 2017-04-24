package uniol.synthesis.util;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import uniol.synthesis.adt.mu_calculus.Event;
import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class PrintFormulaTest {
	@Test
	public void testConstantFormula() {
		FormulaCreator creator = new FormulaCreator();
		assertThat(creator.constant(true), hasToString("true"));
		assertThat(creator.constant(false), hasToString("false"));
	}

	@Test
	public void testVariableFormula() {
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.variable("foo");
		assertThat(formula, hasToString("foo"));
	}

	@Test
	public void testFixedPointFormula() {
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, creator.variable("foo"), creator.constant(true));
		assertThat(formula, hasToString("(mufoo.true)"));
	}

	@Test
	public void testFixedPointFormula2() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, var, var);
		assertThat(formula, hasToString("(mufoo.foo)"));
	}

	@Test
	public void testFixedPointFormula3() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("foo");
		VariableFormula var2 = creator.variable("bar");
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, var, var2);
		assertThat(formula, hasToString("(mufoo.bar)"));
	}

	@Test
	public void testFixedPointFormula4() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.fixedPoint(FixedPoint.GREATEST, var,
				creator.fixedPoint(FixedPoint.LEAST, var, var));
		assertThat(formula, hasToString("(nufoo.(mufoo.foo))"));
	}

	@Test
	public void testDoubleNegation() {
		FormulaCreator creator = new FormulaCreator();
		Formula inner = creator.variable("var");
		Formula formula = creator.negate(creator.negate(inner));
		assertThat(formula, hasToString("(!(!var))"));
	}

	@Test
	public void testNegate() {
		FormulaCreator creator = new FormulaCreator();
		Formula var = creator.variable("var");
		Formula formula = creator.negate(var);
		assertThat(formula, hasToString("(!var)"));
	}

	@Test
	public void testConjunction() {
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.conjunction(creator.constant(true), creator.constant(false));
		assertThat(formula, hasToString("(true&&false)"));
		assertThat(creator.negate(formula), hasToString("(!(true&&false))"));
	}

	@Test
	public void testDisjunction() {
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.disjunction(creator.constant(true), creator.constant(false));
		assertThat(formula, hasToString("(true||false)"));
		assertThat(creator.negate(formula), hasToString("(!(true||false))"));
	}

	@Test
	public void testFixedPoint() {
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, creator.variable("foo"), creator.constant(true));
		assertThat(formula, hasToString("(mufoo.true)"));
	}

	@Test
	public void testModality() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Event event = new Event("foo");
		Formula formula = creator.modality(Modality.UNIVERSAL, event, True);
		assertThat(formula, hasToString("[foo]true"));
	}
}
