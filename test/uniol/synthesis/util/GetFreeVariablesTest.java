package uniol.synthesis.util;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class GetFreeVariablesTest {
	static private FormulaCreator creator = new FormulaCreator();

	@Test
	public void testConstantFormula() {
		Formula formula = creator.constant(true);
		assertThat(GetFreeVariables.getFreeVariables(formula), empty());
	}

	@Test
	public void testVariableFormula() {
		Formula formula = creator.variable("foo");
		assertThat(GetFreeVariables.getFreeVariables(formula), contains(formula));
	}

	@Test
	public void testFixedPointFormula() {
		Formula True = creator.constant(true);
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, var, True);
		assertThat(GetFreeVariables.getFreeVariables(formula), empty());
	}

	@Test
	public void testFixedPointFormula2() {
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, var, var);
		assertThat(GetFreeVariables.getFreeVariables(formula), empty());
	}

	@Test
	public void testFixedPointFormula3() {
		VariableFormula var = creator.variable("foo");
		VariableFormula var2 = creator.variable("bar");
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, var, var2);
		assertThat(GetFreeVariables.getFreeVariables(formula), contains(var2));
	}
}
