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

		Formula clean = CleanFormFormulaTransformer.cleanForm(formula);
		Formula expected = creator.fixedPoint(FixedPoint.GREATEST, var, creator.fixedPoint(FixedPoint.LEAST,
					creator.variable("foo0"), creator.variable("foo0")));
		assertThat(clean, sameInstance(expected));
	}

	@Test
	public void testCache() {
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.variable("foo");
		for (int i = 0; i < 1000; i++)
			formula = creator.conjunction(formula, formula);
		assertThat(CleanFormFormulaTransformer.cleanForm(formula), sameInstance(formula));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
