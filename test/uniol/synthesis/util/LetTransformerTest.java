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

import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

import static uniol.synthesis.util.LetTransformer.let;
import static uniol.synthesis.util.UnLetTransformer.unLet;

public class LetTransformerTest {
	@Test
	public void testSimpleLet() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("var");
		Formula formula = creator.negate(creator.let(var, var, var));
		assertThat(let(formula), sameInstance((Formula) creator.negate(var)));
	}

	@Test
	public void testRecursiveLet() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("var");
		Formula formula = creator.let(var, creator.let(var, var, var), creator.let(var, var, var));
		assertThat(let(formula), sameInstance((Formula) var));
	}

	@Test
	public void testRecursiveLet2() {
		FormulaCreator creator = new FormulaCreator();
		Formula base = creator.negate(creator.constant(true));
		Formula formula = base;
		formula = creator.conjunction(formula, formula);
		formula = creator.conjunction(formula, formula);
		formula = creator.conjunction(formula, formula);

		Formula letted = let(formula);

		Formula expected = creator.let(creator.variable("cse0"),
				creator.let(creator.variable("cse1"),
					creator.let(creator.variable("cse2"), base,
					creator.conjunction(creator.variable("cse2"), creator.variable("cse2"))),
					creator.conjunction(creator.variable("cse1"), creator.variable("cse1"))),
				creator.conjunction(creator.variable("cse0"), creator.variable("cse0")));
		assertThat(letted, sameInstance(expected));
		assertThat(unLet(letted), sameInstance(formula));
	}

	@Test
	public void testMixOfConjunctionsAndDisjunctions() {
		FormulaCreator creator = new FormulaCreator();

		// Construct ((true&&true)&&(false||false))&&((false||false)&&(true&&true))
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);
		Formula inner1 = creator.conjunction(True, True);
		Formula inner2 = creator.disjunction(False, False);
		Formula formula = creator.conjunction(
				creator.conjunction(inner1, inner2),
				creator.conjunction(inner2, inner1));

		Formula letted = let(formula);

		// The let-form of this is one of the following:
		// let cse1 = (true&&true) in let cse0 = (false||false) in (cse1 && cse0) && (cse0 && cse1)
		// let cse0 = (false||false) in let cse1 = (true&&true) in (cse1 && cse0) && (cse0 && cse1)
		Formula inner = creator.conjunction(
				creator.conjunction(creator.variable("cse0"), creator.variable("cse1")),
				creator.conjunction(creator.variable("cse1"), creator.variable("cse0")));
		Formula expected1 = creator.let(creator.variable("cse0"),
					inner1,
					creator.let(creator.variable("cse1"),
						inner2,
						inner));
		Formula expected2 = creator.let(creator.variable("cse1"),
					inner2,
					creator.let(creator.variable("cse0"),
						inner1,
						inner));
		assertThat(letted, anyOf(sameInstance(expected1), sameInstance(expected2)));
	}

	@Test
	public void testError() throws Exception {
		FormulaCreator creator = new FormulaCreator();
		Formula False = creator.constant(false);
		Formula inner1 = creator.modality(Modality.UNIVERSAL, "a", False);
		Formula inner2 = creator.modality(Modality.UNIVERSAL, "b", False);
		Formula inner = creator.conjunction(inner1, inner2);
		Formula formula = creator.conjunction(inner,
				creator.conjunction(inner2,
					creator.conjunction(inner1, inner)));

		Formula letted = let(formula);

		VariableFormula cse0 = creator.variable("cse0");
		VariableFormula cse1 = creator.variable("cse1");
		VariableFormula cse2 = creator.variable("cse2");
		Formula expected = creator.let(cse0,
				creator.modality(Modality.UNIVERSAL, "b", False),
			creator.let(cse1,
				creator.modality(Modality.UNIVERSAL, "a", False),
			creator.let(cse2,
				creator.conjunction(cse1, cse0),
				creator.conjunction(cse2, creator.conjunction(cse0,
					creator.conjunction(cse1, cse2))))));
		assertThat(letted, sameInstance(expected));
	}

	@Test
	public void testError2() throws Exception {
		FormulaCreator creator = new FormulaCreator();
		Formula False = creator.constant(false);
		Formula inner1 = creator.modality(Modality.UNIVERSAL, "a", False);
		Formula inner2 = creator.conjunction(inner1,
				creator.modality(Modality.UNIVERSAL, "b", False));
		Formula formula = creator.conjunction(inner2, creator.conjunction(inner2, inner1));

		Formula letted = let(formula);

		VariableFormula cse0 = creator.variable("cse0");
		VariableFormula cse1 = creator.variable("cse1");
		Formula expected = creator.let(cse0,
				creator.modality(Modality.UNIVERSAL, "a", False),
			creator.let(cse1,
				creator.conjunction(cse0, creator.modality(Modality.UNIVERSAL, "b", False)),
			creator.conjunction(cse1, creator.conjunction(cse1, cse0))));
		assertThat(letted, sameInstance(expected));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
