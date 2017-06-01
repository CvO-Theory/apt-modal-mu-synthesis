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

package uniol.synthesis.expansion;

import java.util.Set;
import java.util.HashSet;

import uniol.apt.adt.ts.State;
import uniol.apt.adt.ts.TransitionSystem;
import uniol.apt.analysis.isomorphism.IsomorphismLogic;
import uniol.apt.analysis.synthesize.PNProperties;
import uniol.apt.io.renderer.RenderException;
import uniol.apt.io.renderer.impl.DotLTSRenderer;
import uniol.apt.util.Pair;

import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
import uniol.synthesis.tableau.Tableau;
import uniol.synthesis.util.NonRecursive;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.fail;
import static uniol.apt.util.matcher.Matchers.pairWith;
import static uniol.synthesis.tableau.TableauMatchers.*;

@SuppressWarnings("unchecked")
public class RealiseFormulaIntegrationTest {
	private Matcher<TransitionSystem> isomorphicTo(final TransitionSystem expected) {
		return new TypeSafeDiagnosingMatcher<TransitionSystem>() {
			@Override
			public boolean matchesSafely(TransitionSystem actual, Description mismatchDescription) {
				if (new IsomorphismLogic(expected, actual, true).isIsomorphic())
					return true;

				try {
					mismatchDescription.appendText(new DotLTSRenderer().render(actual));
				} catch (RenderException e) {
					mismatchDescription.appendText("Exception while rendering actual input: ");
					mismatchDescription.appendValue(e);
				}
				return false;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("Transition system isomorphic to the given TS");
			}
		};
	}

	static private class Result {
		public TransitionSystem resultTS;
		public Tableau<State> resultTab;
	}

	private Pair<TransitionSystem, Tableau<State>> realiseUnique(PNProperties properties, Formula formula) {
		final Result result = new Result();
		new NonRecursive().run(new RealiseFormula(properties, formula,
					new RealiseFormula.RealisationCallback() {
						@Override
						public void foundRealisation(TransitionSystem ts, Tableau<State> tableau) {
							assertThat(result.resultTS, nullValue());
							assertThat(result.resultTab, nullValue());
							result.resultTS = ts;
							result.resultTab = tableau;
						}
		}));

		assertThat(result.resultTS, not(nullValue()));
		assertThat(result.resultTab, not(nullValue()));
		return new Pair<>(result.resultTS, result.resultTab);
	}

	@Test
	public void testMyFavoriteExample() {
		FormulaCreator creator = new FormulaCreator();
		Modality ex = Modality.EXISTENTIAL;
		Formula formula = creator.fixedPoint(FixedPoint.GREATEST, creator.variable("X"), creator.conjunction(
				creator.modality(ex, "a", creator.modality(ex, "b", creator.modality(ex, "c",
							creator.constant(true)))),
				creator.modality(ex, "b", creator.modality(ex, "a", creator.modality(Modality.UNIVERSAL, "c",
							creator.variable("X"))))));

		// Just for documentation: This is the formula that is being solved
		assertThat(formula, hasToString("(nu X.(<a><b><c>true&&<b><a>[c]X))"));

		PNProperties properties = new PNProperties().requireKBounded(2);
		Pair<TransitionSystem, Tableau<State>> pair = realiseUnique(properties, formula);
		TransitionSystem ts = pair.getFirst();
		Tableau<State> tableau = pair.getSecond();

		// Create the expected ts
		TransitionSystem expected = new TransitionSystem();
		expected.createStates("s0", "s1", "s2", "s3");
		expected.setInitialState("s0");
		expected.createArc("s0", "s1", "a");
		expected.createArc("s0", "s2", "b");
		expected.createArc("s1", "s3", "b");
		expected.createArc("s2", "s3", "a");
		expected.createArc("s3", "s0", "c");

		assertThat(tableau, hasLeaves(contains(hasStateAndFormula(ts.getInitialState(), creator.constant(true)))));
		assertThat(ts, isomorphicTo(expected));
	}

	@Test
	public void testABBAA3Bounded() {
		FormulaCreator creator = new FormulaCreator();
		Modality ex = Modality.EXISTENTIAL;
		Formula formula = creator.modality(ex, "a", creator.modality(ex, "b", creator.modality(ex, "b",
						creator.modality(ex, "a", creator.modality(ex, "a",
								creator.constant(true))))));

		// Just for documentation: This is the formula that is being solved
		assertThat(formula, hasToString("<a><b><b><a><a>true"));

		PNProperties properties = new PNProperties().requireKBounded(3);
		Pair<TransitionSystem, Tableau<State>> pair = realiseUnique(properties, formula);
		TransitionSystem ts = pair.getFirst();
		Tableau<State> tableau = pair.getSecond();

		// Create the expected ts
		TransitionSystem expected = new TransitionSystem();
		expected.createStates("s0", "s1", "s2", "s3", "s4", "s5", "s6");
		expected.setInitialState("s0");
		expected.createArc("s0", "s1", "a");
		expected.createArc("s1", "s2", "b");
		expected.createArc("s2", "s3", "b");
		expected.createArc("s3", "s4", "a");
		expected.createArc("s4", "s5", "a");
		expected.createArc("s2", "s6", "a");

		assertThat(tableau, hasLeaves(contains(hasFormula(creator.constant(true)))));
		assertThat(ts, isomorphicTo(expected));
	}

	@Test
	public void testABBAA2Bounded() {
		FormulaCreator creator = new FormulaCreator();
		Modality ex = Modality.EXISTENTIAL;
		Formula formula = creator.modality(ex, "a", creator.modality(ex, "b", creator.modality(ex, "b",
						creator.modality(ex, "a", creator.modality(ex, "a",
								creator.constant(true))))));

		// Just for documentation: This is the formula that is being solved
		assertThat(formula, hasToString("<a><b><b><a><a>true"));

		PNProperties properties = new PNProperties().requireKBounded(2);
		Pair<TransitionSystem, Tableau<State>> pair = realiseUnique(properties, formula);
		TransitionSystem ts = pair.getFirst();
		Tableau<State> tableau = pair.getSecond();

		// Create the expected ts
		TransitionSystem expected = new TransitionSystem();
		expected.createStates("s0", "s1", "s2", "s3", "s4", "s5", "s6", "s7");
		expected.setInitialState("s0");
		expected.createArc("s0", "s1", "a");
		expected.createArc("s0", "s2", "b");
		expected.createArc("s1", "s3", "b");
		expected.createArc("s2", "s3", "a");
		expected.createArc("s3", "s4", "a");
		expected.createArc("s3", "s5", "b");
		expected.createArc("s4", "s6", "b");
		expected.createArc("s5", "s6", "a");
		expected.createArc("s6", "s7", "a");

		assertThat(tableau, hasLeaves(contains(hasFormula(creator.constant(true)))));
		assertThat(ts, isomorphicTo(expected));
	}

	@Test
	public void testABBAANoSideArm() {
		FormulaCreator creator = new FormulaCreator();
		Modality ex = Modality.EXISTENTIAL;
		Formula formula = creator.modality(ex, "a", creator.modality(ex, "b", creator.conjunction(
					creator.modality(Modality.UNIVERSAL, "a", creator.constant(false)),
					creator.modality(ex, "b", creator.modality(ex, "a", creator.modality(ex, "a",
								creator.constant(true)))))));

		// Just for documentation: This is the formula that is being solved
		assertThat(formula, hasToString("<a><b>([a]false&&<b><a><a>true)"));

		PNProperties properties = new PNProperties().requireKBounded(3);

		new NonRecursive().run(new RealiseFormula(properties, formula,
					new RealiseFormula.RealisationCallback() {
						@Override
						public void foundRealisation(TransitionSystem ts, Tableau<State> tableau) {
							fail();
						}
					}));
	}

	@Test
	public void testDisjunction() {
		FormulaCreator creator = new FormulaCreator();
		Modality ex = Modality.EXISTENTIAL;
		Formula formula = creator.disjunction(
				creator.modality(ex, "a", creator.constant(true)),
				creator.modality(ex, "b", creator.constant(true)));

		// Just for documentation: This is the formula that is being solved
		assertThat(formula, hasToString("(<a>true||<b>true)"));

		PNProperties properties = new PNProperties().requireKBounded(1);

		final Set<Pair<TransitionSystem, Tableau<State>>> result = new HashSet<>();
		new NonRecursive().run(new RealiseFormula(properties, formula,
					new RealiseFormula.RealisationCallback() {
						@Override
						public void foundRealisation(TransitionSystem ts, Tableau<State> tableau) {
							result.add(new Pair<>(ts, tableau));
						}
					}));

		// Create the expected ts
		TransitionSystem expected1 = new TransitionSystem();
		expected1.createStates("s0", "s1");
		expected1.setInitialState("s0");
		expected1.createArc("s0", "s1", "a");

		TransitionSystem expected2 = new TransitionSystem();
		expected2.createStates("s0", "s1");
		expected2.setInitialState("s0");
		expected2.createArc("s0", "s1", "b");

		Formula True = creator.constant(true);
		assertThat(result, containsInAnyOrder(
					pairWith(isomorphicTo(expected1), hasLeaves(contains(hasFormula(True)))),
					pairWith(isomorphicTo(expected2), hasLeaves(contains(hasFormula(True))))));
	}

	@Test
	public void testNegation() {
		final FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.negate(creator.constant(false));
		PNProperties properties = new PNProperties().requireKBounded(1);

		final TransitionSystem expectedTS = new TransitionSystem();
		expectedTS.setInitialState(expectedTS.createState());

		final boolean[] alreadyCalled = new boolean[1];
		new NonRecursive().run(new RealiseFormula(properties, formula,
					new RealiseFormula.RealisationCallback() {
						@Override
						public void foundRealisation(TransitionSystem ts, Tableau<State> tableau) {
							assertThat(alreadyCalled[0], is(false));
							alreadyCalled[0] = true;
							assertThat(ts, isomorphicTo(expectedTS));
							assertThat(tableau, hasLeaves(contains(hasStateAndFormula(ts.getInitialState(), creator.constant(true)))));
						}
					}));
		assertThat(alreadyCalled[0], is(true));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
