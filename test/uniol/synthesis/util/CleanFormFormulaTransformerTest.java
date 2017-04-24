package uniol.synthesis.util;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class CleanFormFormulaTransformerTest {
	@Test
	public void testConstantFormula() {
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.constant(true);
		assertThat(CleanFormFormulaTransformer.cleanForm(formula), sameInstance(formula));
	}

	@Test
	public void testVariableFormula() {
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.variable("foo");
		assertThat(CleanFormFormulaTransformer.cleanForm(formula), sameInstance(formula));
	}

	@Test
	public void testFixedPointFormula() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, var, True);
		assertThat(CleanFormFormulaTransformer.cleanForm(formula), sameInstance(formula));
	}

	@Test
	public void testFixedPointFormula2() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, var, var);
		assertThat(CleanFormFormulaTransformer.cleanForm(formula), sameInstance(formula));
	}

	@Test
	public void testFixedPointFormula3() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("foo");
		VariableFormula var2 = creator.variable("bar");
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, var, var2);
		assertThat(CleanFormFormulaTransformer.cleanForm(formula), sameInstance(formula));
	}

	@Test
	public void testFixedPointFormula4() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("foo");
		Formula innerFormula = creator.fixedPoint(FixedPoint.LEAST, var, var);
		Formula formula = creator.fixedPoint(FixedPoint.GREATEST, var, innerFormula);

		Formula expected = creator.fixedPoint(FixedPoint.GREATEST, var, creator.fixedPoint(FixedPoint.LEAST,
					creator.variable("{foo0}"), creator.variable("{foo0}")));
		assertThat(CleanFormFormulaTransformer.cleanForm(formula), sameInstance(expected));
	}
}
