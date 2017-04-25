package uniol.synthesis.util;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class SubstitutionTransformerTest {
	static private FormulaCreator creator = new FormulaCreator();
	static private VariableFormula variable = creator.variable("bar");
	static private Formula substitute = creator.freshVariable("substitute");

	@Test
	public void testConstantFormula() {
		Formula formula = creator.constant(true);
		assertThat(SubstitutionTransformer.substitute(formula, variable, substitute), sameInstance(formula));
	}

	@Test
	public void testVariableFormula() {
		assertThat(SubstitutionTransformer.substitute(variable, variable, substitute), sameInstance(substitute));
	}

	@Test
	public void testFixedPointFormula() {
		Formula True = creator.constant(true);
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, variable, True);
		assertThat(SubstitutionTransformer.substitute(formula, variable, substitute), sameInstance(formula));
	}

	@Test
	public void testFixedPointFormula2() {
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, variable, variable);
		assertThat(SubstitutionTransformer.substitute(formula, variable, substitute), sameInstance(formula));
	}

	@Test
	public void testFixedPointFormula3() {
		VariableFormula variable2 = creator.freshVariable("bar");
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, variable, variable2);
		assertThat(SubstitutionTransformer.substitute(formula, variable, substitute), sameInstance(formula));
	}

	@Test
	public void testNegate() {
		Formula formula = creator.negate(variable);
		Formula expected = creator.negate(substitute);
		assertThat(SubstitutionTransformer.substitute(formula, variable, substitute), sameInstance(expected));
	}
}
