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

package uniol.synthesis.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class GetFreeVariablesTest {
	static private FormulaCreator creator = new FormulaCreator();

	static private class FreeVariablesMatcher extends TypeSafeDiagnosingMatcher<Formula> {
		final private Map<VariableFormula, Integer> expectedCounts;

		private FreeVariablesMatcher(Map<VariableFormula, Integer> expectedCounts) {
			this.expectedCounts = expectedCounts;
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("free variables should be ").appendValue(expectedCounts);
		}

		private int get(Map<VariableFormula, Integer> map, VariableFormula var) {
			Integer result = map.get(var);
			if (result == null)
				return 0;
			return result;
		}

		@Override
		protected boolean matchesSafely(Formula formula, Description mismatchDescription) {
			boolean result = true;
			String spacer = "";
			Map<VariableFormula, Integer> freeVars = GetFreeVariables.getFreeVariablesCounts(formula);

			Set<VariableFormula> variables = new HashSet<>(expectedCounts.keySet());
			variables.addAll(freeVars.keySet());

			for (VariableFormula variable : variables) {
				int expected = get(expectedCounts, variable);
				int got = get(freeVars, variable);
				if (expected != got) {
					mismatchDescription.appendText(spacer).appendText("expected ");
					mismatchDescription.appendValue(variable).appendText(" to appear ");
					mismatchDescription.appendValue(expected).appendText("-times, but got ");
					mismatchDescription.appendValue(got).appendText("-times");
					result = false;
					spacer = " and ";
				}
			}

			return result;
		}
	}

	static private Matcher<Formula> freeVariables(VariableFormula[] variables, int... counts) {
		if (variables.length != counts.length)
			throw new AssertionError("Illegal argument lengths");
		Map<VariableFormula, Integer> expected = new HashMap<>();
		for (int i = 0; i < variables.length; i++) {
			expected.put(variables[i], counts[i]);
		}
		return new FreeVariablesMatcher(expected);
	}

	private VariableFormula[] variables(String... vars) {
		VariableFormula[] result = new VariableFormula[vars.length];
		for (int i = 0; i < vars.length; i++)
			result[i] = creator.variable(vars[i]);
		return result;
	}

	@Test
	public void testConstantFormula() {
		Formula formula = creator.constant(true);
		assertThat(GetFreeVariables.getFreeVariables(formula), empty());
		assertThat(formula, freeVariables(variables()));
	}

	@Test
	public void testVariableFormula() {
		Formula formula = creator.variable("foo");
		assertThat(GetFreeVariables.getFreeVariables(formula), contains(formula));
		assertThat(formula, freeVariables(variables("foo"), 1));
	}

	@Test
	public void testFixedPointFormula() {
		Formula True = creator.constant(true);
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, var, True);
		assertThat(GetFreeVariables.getFreeVariables(formula), empty());
		assertThat(formula, freeVariables(variables()));
	}

	@Test
	public void testFixedPointFormula2() {
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, var, var);
		assertThat(GetFreeVariables.getFreeVariables(formula), empty());
		assertThat(formula, freeVariables(variables()));
	}

	@Test
	public void testFixedPointFormula3() {
		VariableFormula var = creator.variable("foo");
		VariableFormula var2 = creator.variable("bar");
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, var, var2);
		assertThat(GetFreeVariables.getFreeVariables(formula), contains(var2));
		assertThat(formula, freeVariables(variables("bar"), 1));
	}

	@Test
	public void testLetFormula() {
		Formula True = creator.constant(true);
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.let(var, True, True);
		assertThat(GetFreeVariables.getFreeVariables(formula), empty());
		assertThat(formula, freeVariables(variables()));
	}

	@Test
	public void testLetFormula2() {
		Formula True = creator.constant(true);
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.let(var, True, var);
		assertThat(GetFreeVariables.getFreeVariables(formula), empty());
		assertThat(formula, freeVariables(variables()));
	}

	@Test
	public void testLetFormula3() {
		Formula True = creator.constant(true);
		VariableFormula var = creator.variable("foo");
		VariableFormula var2 = creator.variable("bar");
		Formula formula = creator.let(var, True, var2);
		assertThat(GetFreeVariables.getFreeVariables(formula), contains(var2));
		assertThat(formula, freeVariables(variables("bar"), 1));
	}

	@Test
	public void testLetFormula4() {
		VariableFormula var = creator.variable("foo");
		VariableFormula var2 = creator.variable("bar");
		VariableFormula var3 = creator.variable("baz");
		Formula formula = creator.let(var, var2, var3);
		assertThat(GetFreeVariables.getFreeVariables(formula), contains(var3));
		assertThat(formula, freeVariables(variables("baz"), 1));
	}

	@Test
	public void testLetFormula5() {
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.let(var, var, var);
		assertThat(GetFreeVariables.getFreeVariables(formula), contains(var));
		assertThat(formula, freeVariables(variables("foo"), 1));
	}

	@Test
	public void testLetFormula6a() {
		VariableFormula var = creator.variable("foo");
		VariableFormula var2 = creator.variable("bar");
		Formula formula = creator.let(var, var, var2);
		assertThat(GetFreeVariables.getFreeVariables(formula), contains(var2));
		assertThat(formula, freeVariables(variables("bar"), 1));
	}

	@Test
	public void testLetFormula6b() {
		VariableFormula var = creator.variable("foo");
		VariableFormula var2 = creator.variable("bar");
		Formula formula = creator.let(var, var2, var);
		assertThat(GetFreeVariables.getFreeVariables(formula), contains(var2));
		assertThat(formula, freeVariables(variables("bar"), 1));
	}

	@Test
	public void testLetFormula6c() {
		VariableFormula var = creator.variable("foo");
		VariableFormula var2 = creator.variable("bar");
		Formula formula = creator.let(var2, var, var);
		assertThat(GetFreeVariables.getFreeVariables(formula), contains(var));
		assertThat(formula, freeVariables(variables("foo"), 1));
	}

	@Test
	public void testLetAndFixedPoint() {
		VariableFormula var = creator.variable("foo");
		VariableFormula var2 = creator.variable("bar");
		Formula innerFormula = creator.let(var, var2, var);
		Formula formula = creator.fixedPoint(FixedPoint.GREATEST, var, innerFormula);
		assertThat(GetFreeVariables.getFreeVariables(formula), contains(var2));
		assertThat(formula, freeVariables(variables("bar"), 1));
	}

	@Test
	public void testLetAndFixedPoint2a() {
		VariableFormula var = creator.variable("foo");
		VariableFormula var2 = creator.variable("bar");
		Formula innerFormula = creator.let(var2, var, var);
		Formula formula = creator.fixedPoint(FixedPoint.GREATEST, var, innerFormula);
		assertThat(GetFreeVariables.getFreeVariables(formula), empty());
		assertThat(formula, freeVariables(variables()));
	}

	@Test
	public void testLetAndFixedPoint2b() {
		VariableFormula var = creator.variable("foo");
		Formula innerFormula = creator.let(var, var, var);
		Formula formula = creator.fixedPoint(FixedPoint.GREATEST, var, innerFormula);
		assertThat(GetFreeVariables.getFreeVariables(formula), empty());
		assertThat(formula, freeVariables(variables()));
	}

	@Test
	public void testLetAndFixedPoint3() {
		VariableFormula var = creator.variable("foo");
		VariableFormula var2 = creator.variable("bar");
		Formula innerFormula = creator.fixedPoint(FixedPoint.GREATEST, var, var);
		Formula formula = creator.let(var, var2, innerFormula);
		assertThat(GetFreeVariables.getFreeVariables(formula), empty());
		assertThat(formula, freeVariables(variables()));
	}

	@Test
	public void testMultiplicity() {
		VariableFormula var = creator.variable("foo");
		VariableFormula var2 = creator.variable("bar");
		Formula formula = creator.conjunction(
				creator.conjunction(var, var2),
				creator.let(var, var2, creator.conjunction(var2, var)));
		assertThat(GetFreeVariables.getFreeVariables(formula), containsInAnyOrder(var, var2));
		assertThat(formula, freeVariables(variables("bar", "foo"), 3, 1));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
