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
import uniol.synthesis.adt.mu_calculus.LetFormula;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

import static uniol.synthesis.util.PositiveFormFormulaTransformer.positiveForm;

public class PositiveFormFormulaTransformerTest {
	@Test
	public void testConstantFormula() {
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.constant(true);
		assertThat(positiveForm(formula), sameInstance(formula));
	}

	@Test
	public void testVariableFormula() {
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.variable("foo");
		assertThat(positiveForm(formula), sameInstance(formula));
	}

	@Test
	public void testFixedPointFormula() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, var, True);
		assertThat(positiveForm(formula), sameInstance(formula));
	}

	@Test
	public void testFixedPointFormula2() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, var, var);
		assertThat(positiveForm(formula), sameInstance(formula));
	}

	@Test
	public void testFixedPointFormula3() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("foo");
		VariableFormula var2 = creator.variable("bar");
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, var, var2);
		assertThat(positiveForm(formula), sameInstance(formula));
	}

	@Test
	public void testFixedPointFormula4() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("foo");
		Formula innerFormula = creator.fixedPoint(FixedPoint.LEAST, var, var);
		Formula formula = creator.fixedPoint(FixedPoint.GREATEST, var, innerFormula);
		assertThat(positiveForm(formula), sameInstance(formula));
	}

	@Test
	public void testDoubleNegation() {
		FormulaCreator creator = new FormulaCreator();
		Formula inner = creator.variable("var");
		Formula formula = creator.negate(creator.negate(inner));
		assertThat(positiveForm(formula), sameInstance(inner));
	}

	@Test
	public void testConstant() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);
		assertThat(positiveForm(creator.negate(True)), sameInstance(False));
		assertThat(positiveForm(creator.negate(False)), sameInstance(True));
	}

	@Test
	public void testNegate() {
		FormulaCreator creator = new FormulaCreator();
		Formula var = creator.variable("var");
		Formula formula = creator.negate(var);
		assertThat(positiveForm(formula), sameInstance(formula));
	}

	@Test
	public void testConjunction() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);
		Formula formula = creator.conjunction(True, False);
		Formula expected = creator.disjunction(False, True);
		assertThat(positiveForm(formula), sameInstance(formula));
		assertThat(positiveForm(creator.negate(formula)), sameInstance(expected));
	}

	@Test
	public void testDisjunction() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);
		Formula formula = creator.disjunction(True, False);
		Formula expected = creator.conjunction(False, True);
		assertThat(positiveForm(formula), sameInstance(formula));
		assertThat(positiveForm(creator.negate(formula)), sameInstance(expected));
	}

	@Test
	public void testVariable() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.negate(var);
		assertThat(positiveForm(formula), sameInstance(formula));
	}

	@Test
	public void testFixedPoint() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, var, True);
		Formula expected = creator.fixedPoint(FixedPoint.GREATEST, var, False);
		assertThat(positiveForm(formula), sameInstance(formula));
		assertThat(positiveForm(creator.negate(formula)), sameInstance(expected));
	}

	@Test
	public void testModality() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);
		Formula formula = creator.modality(Modality.UNIVERSAL, "foo", True);
		Formula expected = creator.modality(Modality.EXISTENTIAL, "foo", False);
		assertThat(positiveForm(formula), sameInstance(formula));
		assertThat(positiveForm(creator.negate(formula)), sameInstance(expected));
	}

	@Test
	public void testLet1() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		VariableFormula var = creator.variable("foo");
		VariableFormula var2 = creator.variable("bar");
		Formula formula = creator.let(var, True, var2);
		Formula expected = creator.negate(var2);
		assertThat(positiveForm(formula), sameInstance((Formula) var2));
		assertThat(positiveForm(creator.negate(formula)), sameInstance(expected));
	}

	@Test
	public void testLet2() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.let(var, True, var);

		assertThat(positiveForm(formula), sameInstance(formula));

		// The negation of the formula should be (let X = False in X), but X is a fresh variable
		Formula res = positiveForm(creator.negate(formula));
		assertThat(res, instanceOf(LetFormula.class));
		LetFormula result = (LetFormula) res;
		assertThat(result.getVariable(), sameInstance(result.getFormula()));
		assertThat(result.getExpansion(), sameInstance(False));
	}

	@Test
	public void testLet3() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.let(var, True, creator.disjunction(var, creator.negate(var)));

		// The positive form of the formula should be (let X1 = false in let X = true in X||!X), but X1 is fresh
		Formula res = positiveForm(formula);
		assertThat(res, instanceOf(LetFormula.class));
		LetFormula result = (LetFormula) res;
		assertThat(result.getExpansion(), sameInstance(False));
		assertThat(result.getFormula(), instanceOf(LetFormula.class));

		LetFormula inner = (LetFormula) result.getFormula();
		assertThat(inner.getExpansion(), sameInstance(True));
		assertThat(inner.getFormula(), sameInstance((Formula)
					creator.disjunction(inner.getVariable(), result.getVariable())));

		// The negation of the formula should be (let X1 = false in let X = true in X1&&X), but X1 is fresh
		res = positiveForm(creator.negate(formula));
		assertThat(res, instanceOf(LetFormula.class));
		result = (LetFormula) res;
		assertThat(result.getExpansion(), sameInstance(False));
		assertThat(result.getFormula(), instanceOf(LetFormula.class));

		inner = (LetFormula) result.getFormula();
		assertThat(inner.getExpansion(), sameInstance(True));
		assertThat(inner.getFormula(), sameInstance((Formula)
					creator.conjunction(result.getVariable(), inner.getVariable())));
	}

	@Test
	public void testRecursiveLet() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("X");
		Formula formula = creator.let(var, creator.constant(false), creator.conjunction(
					var, // this binds to the outer-most let
					creator.let(var, creator.constant(true),
						var))); // this binds to the inner let
		assertThat(positiveForm(formula), sameInstance(formula));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
