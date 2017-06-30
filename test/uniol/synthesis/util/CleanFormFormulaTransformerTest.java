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

		Formula expected = creator.fixedPoint(FixedPoint.GREATEST, var, creator.fixedPoint(FixedPoint.LEAST,
					creator.variable("{foo.0}"), creator.variable("{foo.0}")));
		assertThat(CleanFormFormulaTransformer.cleanForm(formula), sameInstance(expected));
	}

	@Test
	public void testLet() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.let(var, creator.constant(true), var);
		assertThat(CleanFormFormulaTransformer.cleanForm(formula), sameInstance(formula));
	}

	@Test
	public void testLetAndFixedPoint() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("foo");
		VariableFormula var2 = creator.variable("bar");
		Formula innerFormula = creator.let(var, var2, var);
		Formula formula = creator.fixedPoint(FixedPoint.GREATEST, var, innerFormula);
		Formula expected = creator.fixedPoint(FixedPoint.GREATEST, var, creator.let(creator.variable("{foo.0}"),
					var2, creator.variable("{foo.0}")));
		assertThat(CleanFormFormulaTransformer.cleanForm(formula), sameInstance(expected));
	}

	@Test
	public void testBoundAndFreeVariable() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("var");
		VariableFormula var2 = creator.variable("{var.0}");
		Formula formula = creator.conjunction(creator.fixedPoint(FixedPoint.GREATEST, var, var), var);
		Formula expected = creator.conjunction(creator.fixedPoint(FixedPoint.GREATEST, var2, var2), var);
		assertThat(CleanFormFormulaTransformer.cleanForm(formula), sameInstance(expected));
	}

	@Test
	public void testBoundVariable() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("var");
		VariableFormula var2 = creator.variable("{var.0}");
		Formula innerFormula = creator.fixedPoint(FixedPoint.GREATEST, var, var);
		Formula formula = creator.let(var, var, creator.fixedPoint(FixedPoint.GREATEST, var, var));
		Formula expected = creator.let(var, var, creator.fixedPoint(FixedPoint.GREATEST, var2, var2));
		assertThat(CleanFormFormulaTransformer.cleanForm(formula), sameInstance(expected));
	}

	@Test
	public void testLetScoping() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("var");
		VariableFormula varb = creator.variable("{var.1}");
		VariableFormula var2 = creator.variable("var2");
		VariableFormula var2b = creator.variable("{var2.0}");
		Formula formula = creator.let(var2, creator.constant(true),
				creator.let(var2, var, creator.conjunction(var2,
					creator.fixedPoint(FixedPoint.GREATEST, var, var2))));
		Formula expected = creator.let(var2, creator.constant(true),
				creator.let(var2b, var, creator.conjunction(var2b,
					creator.fixedPoint(FixedPoint.GREATEST, varb, var2b))));
		assertThat(CleanFormFormulaTransformer.cleanForm(formula), sameInstance(expected));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
