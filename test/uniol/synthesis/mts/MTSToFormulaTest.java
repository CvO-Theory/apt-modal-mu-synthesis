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

package uniol.synthesis.mts;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.ts.Arc;
import uniol.apt.adt.ts.State;
import uniol.apt.adt.ts.TransitionSystem;

import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class MTSToFormulaTest {
	@Test
	public void testConjunction() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);

		MTSToFormula m = new MTSToFormula();
		assertThat(m.conjunction(True, True), is(True));
		assertThat(m.conjunction(True, False), is(False));
		assertThat(m.conjunction(False, True), is(False));
		assertThat(m.conjunction(False, False), is((Formula) creator.conjunction(False, False)));
	}

	@Test
	public void testDisjunction() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);

		MTSToFormula m = new MTSToFormula();
		assertThat(m.disjunction(True, True), is((Formula) creator.disjunction(True, True)));
		assertThat(m.disjunction(True, False), is(True));
		assertThat(m.disjunction(False, True), is(True));
		assertThat(m.disjunction(False, False), is(False));
	}

	@Test
	public void testSimpleGetVariable() {
		FormulaCreator creator = new FormulaCreator();
		TransitionSystem mts = new TransitionSystem();
		mts.createStates("s0", "s1", "s2", "s3");

		MTSToFormula m = new MTSToFormula();
		assertThat(m.getVariable(creator, mts.getNode("s0")), is(creator.variable("s0")));
		assertThat(m.getVariable(creator, mts.getNode("s1")), is(creator.variable("s1")));
		assertThat(m.getVariable(creator, mts.getNode("s2")), is(creator.variable("s2")));
		assertThat(m.getVariable(creator, mts.getNode("s3")), is(creator.variable("s3")));
	}

	@Test
	public void testFilterMustArcs() {
		Arc arc1 = mock(Arc.class);
		Arc arc2 = mock(Arc.class);
		Arc arc3 = mock(Arc.class);
		Arc arc4 = mock(Arc.class);

		when(arc1.hasExtension("may")).thenReturn(true);
		when(arc2.hasExtension("may")).thenReturn(false);
		when(arc3.hasExtension("may")).thenReturn(true);
		when(arc4.hasExtension("may")).thenReturn(false);

		Set<Arc> arcs = new LinkedHashSet<>(Arrays.asList(arc1, arc2, arc3, arc4));
		assertThat(new MTSToFormula().filterMustArcs(arcs), containsInAnyOrder(arc2, arc4));
	}

	@Test
	public void testStateToFormulaEmpty() {
		FormulaCreator creator = new FormulaCreator();
		State state = mock(State.class);
		TransitionSystem ts = mock(TransitionSystem.class);
		Set<String> alphabet = Collections.emptySet();

		when(ts.getAlphabet()).thenReturn(alphabet);
		when(state.getGraph()).thenReturn(ts);

		assertThat(new MTSToFormula().stateToFormula(creator, state), is((Formula) creator.constant(true)));
	}

	@Test
	public void testStateToFormulaNoEdges() {
		FormulaCreator creator = new FormulaCreator();
		State state = mock(State.class);
		TransitionSystem ts = mock(TransitionSystem.class);
		Set<String> alphabet = new LinkedHashSet<>(Arrays.asList("a", "b"));

		when(ts.getAlphabet()).thenReturn(alphabet);
		when(state.getGraph()).thenReturn(ts);

		Formula expected = creator.conjunction(
				creator.modality(Modality.UNIVERSAL, "a", creator.constant(false)),
				creator.modality(Modality.UNIVERSAL, "b", creator.constant(false)));
		assertThat(new MTSToFormula().stateToFormula(creator, state), is(expected));
	}

	@Test
	public void testStateToFormulaThreeMustEdges() {
		Modality ex = Modality.EXISTENTIAL;
		Modality un = Modality.UNIVERSAL;
		FormulaCreator creator = new FormulaCreator();
		State state = mock(State.class);
		State target1 = mock(State.class);
		State target2 = mock(State.class);
		Arc arcA1 = mock(Arc.class);
		Arc arcA2 = mock(Arc.class);
		Arc arcB = mock(Arc.class);
		TransitionSystem ts = mock(TransitionSystem.class);

		Set<String> alphabet = new LinkedHashSet<>(Arrays.asList("a", "b"));
		when(ts.getAlphabet()).thenReturn(alphabet);
		when(arcA1.getTarget()).thenReturn(target1);
		when(arcA2.getTarget()).thenReturn(target2);
		when(arcB.getTarget()).thenReturn(target1);

		when(target1.getId()).thenReturn("X1");
		when(target2.getId()).thenReturn("X2");

		when(state.getGraph()).thenReturn(ts);
		Set<Arc> aPostset = new LinkedHashSet<>(Arrays.asList(arcA1, arcA2));
		when(state.getPostsetEdgesByLabel("a")).thenReturn(aPostset);
		when(state.getPostsetEdgesByLabel("b")).thenReturn(Collections.singleton(arcB));

		VariableFormula x1 = creator.variable("X1");
		VariableFormula x2 = creator.variable("X2");
		Formula expected = creator.conjunction(
				// Part for a
				creator.conjunction(
					// must-part for a
					creator.conjunction(
						creator.modality(ex, "a", x1),
						creator.modality(ex, "a", x2)),
					// may-part for a
					creator.modality(un, "a", creator.disjunction(x1, x2))),
				// Part for b
				creator.conjunction(
					// must-part for b
					creator.modality(ex, "b", x1),
					// may-part for b
					creator.modality(un, "b", x1)));
		assertThat(new MTSToFormula().stateToFormula(creator, state), is(expected));
	}

	@Test
	public void testMTSToEquationSystemSimple() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula x0 = creator.variable("s0");

		TransitionSystem mts = new TransitionSystem();
		mts.createStates("s0");
		mts.setInitialState("s0");

		Map<VariableFormula, Formula> result = new MTSToFormula().mtsToEquationSystem(creator, mts);
		assertThat(result, hasEntry(x0, (Formula) creator.constant(true)));
		assertThat(result.entrySet(), hasSize(1));
	}

	@Test
	public void testMTSToFormulaSimple() {
		FormulaCreator creator = new FormulaCreator();
		TransitionSystem mts = new TransitionSystem();
		mts.createStates("s0");
		mts.setInitialState("s0");

		Formula formula = new MTSToFormula().mtsToFormula(creator, mts);
		assertThat(formula, is((Formula) creator.constant(true)));
	}

	@Test
	public void testMTSToFormula() {
		Modality ex = Modality.EXISTENTIAL;
		Modality un = Modality.UNIVERSAL;
		FormulaCreator creator = new FormulaCreator();
		TransitionSystem mts = new TransitionSystem();
		mts.createStates("s0", "s1", "s2");
		mts.setInitialState("s0");
		mts.createArc("s0", "s1", "a");
		mts.createArc("s1", "s2", "b").putExtension("may", "may");
		mts.createArc("s2", "s2", "a").putExtension("may", "may");

		VariableFormula varS1 = creator.variable("s1");
		VariableFormula varS2 = creator.variable("s2");
		Formula expectedS2 = creator.fixedPoint(FixedPoint.GREATEST, varS2,
				creator.conjunction(
					creator.modality(un, "a", varS2),
					creator.modality(un, "b", creator.constant(false))));
		Formula expectedS1 = creator.conjunction(
				creator.modality(un, "a", creator.constant(false)),
				creator.modality(un, "b", varS2));
		Formula inner = creator.conjunction(creator.conjunction(
					creator.modality(ex, "a", varS1),
					creator.modality(un, "a", varS1)),
				creator.modality(un, "b", creator.constant(false)));
		Formula expected1 = creator.let(varS1, creator.let(varS2, expectedS2, expectedS1), inner);
		Formula expected2 = creator.let(varS2, expectedS2, creator.let(varS1, expectedS1, inner));

		assertThat(new MTSToFormula().mtsToFormula(creator, mts), anyOf(is(expected1), is(expected2)));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
