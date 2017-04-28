package uniol.synthesis.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class SolveEquationSystemTest {
	static private final Map<VariableFormula, Formula> emptyMap = Collections.emptyMap();

	@Test
	public void testEmptySystem() {
		assertThat(new SolveEquationSystem().solve(FixedPoint.GREATEST, emptyMap), equalTo(emptyMap));
	}

	@Test
	public void testSolvedSystem() {
		FormulaCreator creator = new FormulaCreator();
		Map<VariableFormula, Formula> input = new HashMap<>();
		input.put(creator.variable("X"), creator.constant(true));
		input.put(creator.variable("Y"), creator.constant(false));
		input.put(creator.variable("Z"), creator.conjunction(creator.constant(true), creator.constant(false)));

		assertThat(new SolveEquationSystem().solve(FixedPoint.GREATEST, input), equalTo(input));
	}

	@Test
	public void testRecursiveVariables() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula x = creator.variable("X");
		VariableFormula y = creator.variable("Y");
		VariableFormula z = creator.variable("Z");

		Map<VariableFormula, Formula> input = new HashMap<>();
		input.put(x, x);
		input.put(y, y);
		input.put(z, creator.conjunction(creator.constant(true), z));

		Map<VariableFormula, Formula> expected = new HashMap<>();
		expected.put(x, creator.fixedPoint(FixedPoint.LEAST, x, x));
		expected.put(y, creator.fixedPoint(FixedPoint.LEAST, y, y));
		expected.put(z, creator.fixedPoint(FixedPoint.LEAST, z, creator.conjunction(creator.constant(true), z)));

		assertThat(new SolveEquationSystem().solve(FixedPoint.LEAST, input), equalTo(expected));
	}

	@Test
	public void testRealRecursiveVariables() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula x = creator.variable("X");
		VariableFormula y = creator.variable("Y");

		Map<VariableFormula, Formula> input = new HashMap<>();
		input.put(x, creator.constant(true));
		input.put(y, x);

		Map<VariableFormula, Formula> expected = new HashMap<>();
		expected.put(x, creator.constant(true));
		expected.put(y, creator.constant(true));

		assertThat(new SolveEquationSystem().solve(FixedPoint.GREATEST, input), equalTo(expected));
	}

	@Test
	public void testRealExample() {
		FormulaCreator creator = new FormulaCreator();
		Formula False = creator.constant(false);
		VariableFormula x = creator.variable("X");
		VariableFormula y = creator.variable("Y");
		Modality ex = Modality.EXISTENTIAL;
		Modality un = Modality.UNIVERSAL;
		FixedPoint fp = FixedPoint.GREATEST;

		Map<VariableFormula, Formula> input = new HashMap<>();
		input.put(x, creator.conjunction(creator.modality(ex, "a", y),
					creator.modality(un, "b", False)));
		input.put(y, creator.conjunction(creator.modality(un, "a", False),
					creator.modality(un, "b", x)));

		// This is the result when we first substitute variable Y
		Formula solutionX1 = creator.fixedPoint(fp, x, creator.conjunction(
				creator.modality(ex, "a", creator.conjunction(
						creator.modality(un, "a", False),
						creator.modality(un, "b", x))),
				creator.modality(un, "b", False)));
		Formula solutionY1 = creator.conjunction(
				creator.modality(un, "a", False),
				creator.modality(un, "b", solutionX1));

		// This is the result when we first substitute variable X
		Formula solutionY2 = creator.fixedPoint(fp, y, creator.conjunction(
				creator.modality(un, "a", False),
				creator.modality(un, "b", creator.conjunction(
						creator.modality(ex, "a", y),
						creator.modality(un, "b", False)))));
		Formula solutionX2 = creator.conjunction(
			creator.modality(ex, "a", solutionY2),
			creator.modality(un, "b", False));

		Map<VariableFormula, Formula> result = new SolveEquationSystem().solve(fp, input);
		assertThat(result, anyOf(
					allOf(hasEntry(x, solutionX1), hasEntry(y, solutionY1)),
					allOf(hasEntry(x, solutionX2), hasEntry(y, solutionY2))));
		assertThat(result.entrySet(), hasSize(2));
	}
}
