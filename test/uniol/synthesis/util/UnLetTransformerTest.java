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

import static uniol.synthesis.util.UnLetTransformer.unLet;

public class UnLetTransformerTest {
	@Test
	public void testSimpleLet() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("var");
		Formula formula = creator.let(var, var, var);
		assertThat(unLet(formula), sameInstance((Formula) var));
	}

	@Test
	public void testRecursiveLet() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("var");
		Formula formula = creator.let(var, creator.let(var, var, var), creator.let(var, var, var));
		assertThat(unLet(formula), sameInstance((Formula) var));
	}

	@Test
	public void testRecursiveLet2() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("var");
		VariableFormula var2 = creator.variable("foo");
		Formula True = creator.constant(true);
		Formula formula = creator.let(var,
				creator.let(var2, True, creator.conjunction(var2, var2)),
				creator.let(var, creator.conjunction(var, var),
					creator.conjunction(var, var)));
		Formula expected = True;
		expected = creator.conjunction(expected, expected);
		expected = creator.conjunction(expected, expected);
		expected = creator.conjunction(expected, expected);
		assertThat(unLet(formula), sameInstance(expected));
	}

	@Test
	public void testBoundVariable() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("var");
		Formula innerFormula = creator.fixedPoint(FixedPoint.GREATEST, var, var);
		Formula formula = creator.let(var, creator.constant(true), innerFormula);
		assertThat(unLet(formula), sameInstance(innerFormula));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
