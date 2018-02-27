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

public class AlphabetFinderTest {
	static private FormulaCreator creator = new FormulaCreator();

	@Test
	public void testConstantFormula() {
		Formula formula = creator.constant(true);
		assertThat(AlphabetFinder.getAlphabet(formula), empty());
	}

	@Test
	public void testVariableFormula() {
		Formula formula = creator.variable("foo");
		assertThat(AlphabetFinder.getAlphabet(formula), empty());
	}

	@Test
	public void testFixedPointFormula() {
		Formula True = creator.constant(true);
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, creator.variable("foo"), True);
		assertThat(AlphabetFinder.getAlphabet(formula), empty());
	}

	@Test
	public void testFixedPointFormula2() {
		VariableFormula var = creator.variable("foo");
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, var, var);
		assertThat(AlphabetFinder.getAlphabet(formula), empty());
	}

	@Test
	public void testModality1() {
		Formula formula = creator.modality(Modality.EXISTENTIAL, "a", creator.constant(true));
		assertThat(AlphabetFinder.getAlphabet(formula), contains("a"));
	}

	@Test
	public void testModality2() {
		Formula formula = creator.negate(creator.modality(Modality.EXISTENTIAL, "a", creator.constant(true)));
		formula = creator.conjunction(formula, creator.modality(Modality.UNIVERSAL, "b", creator.variable("X")));
		assertThat(AlphabetFinder.getAlphabet(formula), containsInAnyOrder("a", "b"));
	}

	@Test
	public void testModality3() {
		Formula formula = creator.modality(Modality.EXISTENTIAL, "a",
				creator.disjunction(
					creator.modality(Modality.UNIVERSAL, "b", creator.constant(false)),
					creator.modality(Modality.EXISTENTIAL, "a", creator.constant(true))));
		assertThat(AlphabetFinder.getAlphabet(formula), containsInAnyOrder("a", "b"));
	}

	@Test
	public void testCall() {
		Formula formula = creator.call("foo",
				creator.modality(Modality.UNIVERSAL, "a", creator.constant(true)),
				creator.modality(Modality.EXISTENTIAL, "c", creator.constant(false)));
		assertThat(AlphabetFinder.getAlphabet(formula), containsInAnyOrder("a", "c"));
	}

	@Test
	public void testCache() {
		Formula formula = creator.modality(Modality.EXISTENTIAL, "a", creator.constant(true));
		for (int i = 0; i < 1000; i++) {
			formula = creator.conjunction(formula, formula);
			// Prevent conjunction() from flattening everything
			formula = creator.disjunction(formula, creator.constant(false));
		}
		// There are now 2^1000 occurrences of the modality in the formula
		assertThat(AlphabetFinder.getAlphabet(formula), contains("a"));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
