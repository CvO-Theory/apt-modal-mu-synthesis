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
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
import uniol.synthesis.parser.FormulaParser;

public class PrintFormulaTest {
	private void test(Formula formula, String expected) throws Exception {
		assertThat(formula, hasToString(expected));
		// Test that the result is parsable
		assertThat(FormulaParser.parse(formula.getCreator(), expected), equalTo(formula));
	}

	@Test
	public void testConstantFormula() throws Exception {
		FormulaCreator creator = new FormulaCreator();
		test(creator.constant(true), "true");
		test(creator.constant(false), "false");
	}

	@Test
	public void testVariableFormula() throws Exception {
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.variable("foo");
		test(formula, "foo");
	}

	@Test
	public void testFixedPointFormula() throws Exception {
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, creator.variable("foo"), creator.constant(true));
		test(formula, "(mu foo.true)");
	}

	@Test
	public void testFixedPointFormula2() throws Exception {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, var, var);
		test(formula, "(mu foo.foo)");
	}

	@Test
	public void testFixedPointFormula3() throws Exception {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("foo");
		VariableFormula var2 = creator.variable("bar");
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, var, var2);
		test(formula, "(mu foo.bar)");
	}

	@Test
	public void testFixedPointFormula4() throws Exception {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.fixedPoint(FixedPoint.GREATEST, var,
				creator.fixedPoint(FixedPoint.LEAST, var, var));
		test(formula, "(nu foo.(mu foo.foo))");
	}

	@Test
	public void testDoubleNegation() throws Exception {
		FormulaCreator creator = new FormulaCreator();
		Formula inner = creator.variable("var");
		Formula formula = creator.negate(creator.negate(inner));
		test(formula, "(!(!var))");
	}

	@Test
	public void testNegate() throws Exception {
		FormulaCreator creator = new FormulaCreator();
		Formula var = creator.variable("var");
		Formula formula = creator.negate(var);
		test(formula, "(!var)");
	}

	@Test
	public void testConjunction() throws Exception {
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.conjunction(creator.constant(true), creator.constant(false));
		test(formula, "(true&&false)");
		test(creator.negate(formula), "(!(true&&false))");
	}

	@Test
	public void testDisjunction() throws Exception {
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.disjunction(creator.constant(true), creator.constant(false));
		test(formula, "(true||false)");
		test(creator.negate(formula), "(!(true||false))");
	}

	@Test
	public void testFixedPoint() throws Exception {
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, creator.variable("foo"), creator.constant(true));
		test(formula, "(mu foo.true)");
	}

	@Test
	public void testModality() throws Exception {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula formula = creator.modality(Modality.UNIVERSAL, "foo", True);
		test(formula, "[foo]true");
	}

	@Test
	public void testLet() throws Exception {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("var");
		Formula expansion = creator.constant(true);
		Formula inner = creator.constant(false);
		Formula formula = creator.let(var, expansion, inner);
		test(formula, "(let var = true in false)");
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
