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

	@Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Let formulas are not supported")
	public void testLetFormula() {
		Formula True = creator.constant(true);
		Formula formula = creator.let(variable, True, True);
		SubstitutionTransformer.substitute(formula, variable, substitute);
	}

	// The following test shows why the SubstitutionTransformer cannot easily handle let's: In the original formula,
	// the fixed point has "foo" (var2) as a free variable. After applying the implicit substitution that the let
	// represents, this turns into "bar" (var) which is now bound by the fixed point. However, in the conjunction
	// "foo" (var2) also appears as a free variable.
	// Thus, after the substitution, this "really free" instance of "bar" (var) should be substituted while the
	// bound occurrence should stay bound. This cannot be nicely done, thus we just forbid let's in the
	// substitution.
	@Test(enabled = false)
	public void testThatBreaksWithLets() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula variable2 = creator.variable("foo");
		Formula formula = creator.let(variable2, variable, creator.conjunction(variable2,
				creator.fixedPoint(FixedPoint.GREATEST, variable, variable2)));
		Formula expected = creator.let(variable2, variable, creator.conjunction(substitute,
				creator.fixedPoint(FixedPoint.GREATEST, variable, variable2)));
		assertThat(SubstitutionTransformer.substitute(formula, variable, substitute), sameInstance(expected));
	}

	@Test
	public void testCache() {
		FormulaCreator creator = new FormulaCreator();
		Formula formula = variable;
		Formula expected = substitute;
		for (int i = 0; i < 1000; i++) {
			formula = creator.conjunction(formula, formula);
			expected = creator.conjunction(expected, expected);
		}
		assertThat(SubstitutionTransformer.substitute(formula, variable, substitute), sameInstance(expected));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
