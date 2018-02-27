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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.Transformer;

import uniol.apt.adt.ts.State;
import uniol.apt.adt.ts.TransitionSystem;
import uniol.apt.analysis.synthesize.MissingLocationException;
import uniol.apt.analysis.synthesize.PNProperties;
import uniol.apt.util.Pair;

import uniol.synthesis.tableau.Tableau;
import uniol.synthesis.util.NonRecursive;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.fail;
import static uniol.apt.util.matcher.Matchers.pairWith;
import static uniol.apt.adt.matcher.Matchers.*;

@SuppressWarnings("unchecked")
public class RealiseFormulaTest {
	static class NOPOverapproximateTS implements RealiseFormula.OverapproximateTS {
		@Override
		public TransitionSystem overapproximate(TransitionSystem ts) {
			return ts;
		}
	}

	static class StateWithSameNameTransformerFactory implements RealiseFormula.ReachingWordTransformerFactory {
		@Override
		public Transformer<State, State> create(final TransitionSystem target) {
			return new Transformer<State, State>() {
				@Override
				public State transform(State state) {
					return target.getNode(state.getId());
				}
			};
		}
	}

	static class NeverCalledRealisationCallback implements RealiseFormula.RealisationCallback {
		@Override
		public void foundRealisation(TransitionSystem ts, Tableau<State> tableau) {
			fail();
		}
	}

	static class AddToSetRealisationCallback implements RealiseFormula.RealisationCallback {
		public Set<Pair<TransitionSystem, Tableau<State>>> result = new HashSet<>();

		@Override
		public void foundRealisation(TransitionSystem ts, Tableau<State> tableau) {
			result.add(new Pair<>(ts, tableau));
		}
	}

	static private Tableau<State> mockTableau(boolean successfull) {
		Tableau<State> tableau = mock(Tableau.class);
		when(tableau.isSuccessful()).thenReturn(successfull);
		when(tableau.transform((Transformer<State, State>) anyObject())).thenReturn(tableau);
		return tableau;
	}

	static private TransitionSystem getEmptyTS() {
		TransitionSystem ts = new TransitionSystem();
		ts.setInitialState(ts.createState("s0"));
		return ts;
	}

	@Test
	public void testSuccessfulTableau() {
		Tableau<State> tableau = mockTableau(true);
		TransitionSystem ts = mock(TransitionSystem.class);
		State state = mock(State.class);
		when(ts.getInitialState()).thenReturn(state);
		RealiseFormula.ContinueTableauFactory continueTableauFactory
			= mock(RealiseFormula.ContinueTableauFactory.class);

		when(continueTableauFactory.continueTableau(tableau)).thenReturn(Collections.singleton(tableau));

		AddToSetRealisationCallback addToSet = new AddToSetRealisationCallback();
		RealiseFormula realise = new RealiseFormula(addToSet, null, new StateWithSameNameTransformerFactory(),
				continueTableauFactory, new NOPOverapproximateTS(), 1);

		new RealiseFormula.Worker(realise, ts, tableau).walk(null);

		assertThat(addToSet.result, contains(pairWith(ts, tableau)));
	}

	static private TransitionSystem getExpandedTS(TransitionSystem ts, Set<Pair<State, String>> missingArcs) {
		Tableau<State> tableau = mockTableau(false);
		MissingArcsFinder<State> missingArcsFinder = mock(MissingArcsFinder.class);
		RealiseFormula.ContinueTableauFactory continueTableauFactory
			= mock(RealiseFormula.ContinueTableauFactory.class);
		RealiseFormula.OverapproximateTS overapproximateTS = mock(RealiseFormula.OverapproximateTS.class);
		NonRecursive engine = mock(NonRecursive.class);

		when(overapproximateTS.overapproximate(ts)).thenReturn(ts);
		when(missingArcsFinder.findMissing(tableau)).thenReturn(missingArcs);
		when(continueTableauFactory.continueTableau(tableau)).thenReturn(Collections.singleton(tableau));

		new RealiseFormula.Worker(new RealiseFormula(new NeverCalledRealisationCallback(), missingArcsFinder,
				new StateWithSameNameTransformerFactory(), continueTableauFactory,
				overapproximateTS, 1), ts, tableau).walk(engine);

		verify(missingArcsFinder, times(1)).findMissing(tableau);
		verify(continueTableauFactory, times(1 + missingArcs.size())).continueTableau((Tableau<State>) anyObject());

		ArgumentCaptor<RealiseFormula.Worker> nextWorkerCaptor = ArgumentCaptor.forClass(RealiseFormula.Worker.class);
		verify(engine, times(1)).enqueue(nextWorkerCaptor.capture());
		return nextWorkerCaptor.getValue().getTsForTest();
	}

	@Test
	public void testUnsuccessfulTableauNoMissingArcs() {
		// This test tries to expand based on the unsuccessful tableau. However, we pretend that no arcs are
		// missing and that no expanded tableaus exist, thus the code under test doesn't actually do much.

		TransitionSystem ts = getEmptyTS();
		TransitionSystem expanded = getExpandedTS(ts, Collections.<Pair<State, String>>emptySet());

		assertThat(expanded.getNodes(), contains(nodeWithID("s0")));
		assertThat(expanded.getEdges(), empty());
	}

	@Test
	public void testUnsuccessfulTableauMissingArc() {
		// Test that missing arcs are added
		TransitionSystem ts = getEmptyTS();
		Pair<State, String> missingArc = new Pair<>(ts.getInitialState(), "a");
		TransitionSystem expanded = getExpandedTS(ts, Collections.singleton(missingArc));

		assertThat(expanded.getNodes(), containsInAnyOrder(nodeWithID("s0"), nodeWithID("s1")));
		assertThat(expanded.getEdges(), contains(arcThatConnectsVia("s0", "s1", "a")));
	}

	@Test
	public void testRecursion() {
		// We call the code with a non-successful tableau. No arcs are missing, but two new tableaus will be
		// created. We test that recursion on these tableaus really happens.

		Tableau<State> tableau = mockTableau(false);
		Tableau<State> tableau1 = mockTableau(false);
		Tableau<State> tableau2 = mockTableau(false);
		TransitionSystem ts = getEmptyTS();
		MissingArcsFinder<State> missingArcsFinder = mock(MissingArcsFinder.class);
		RealiseFormula.ContinueTableauFactory continueTableauFactory
			= mock(RealiseFormula.ContinueTableauFactory.class);
		NonRecursive engine = mock(NonRecursive.class);

		when(missingArcsFinder.findMissing(tableau)).thenReturn(Collections.<Pair<State, String>>emptySet());
		when(continueTableauFactory.continueTableau((Tableau) anyObject())).thenReturn(new HashSet<Tableau<State>>(
					Arrays.asList(tableau1, tableau2)));

		new RealiseFormula.Worker(new RealiseFormula(
					new NeverCalledRealisationCallback(), missingArcsFinder,
					new StateWithSameNameTransformerFactory(), continueTableauFactory,
					new NOPOverapproximateTS(), 1), ts, tableau).walk(engine);

		verify(missingArcsFinder, times(1)).findMissing(tableau1);
		verify(missingArcsFinder, times(1)).findMissing(tableau2);
		verify(continueTableauFactory, times(1)).continueTableau((Tableau<State>) anyObject());
		verify(engine, times(2)).enqueue(Mockito.isA(RealiseFormula.Worker.class));
	}

	@Test
	public void testImpossibleException() {
		// Prepare a transition system which causes a MissingLocationException. This can not happen in the
		// actual algorithm since nothing adds locations there.

		TransitionSystem ts = new TransitionSystem();
		ts.createStates("s0", "s1");
		ts.setInitialState("s0");
		ts.createArc("s0", "s1", "a");
		ts.createArc("s0", "s1", "b");
		ts.getEvent("a").putExtension("location", "a");

		try {
			new RealiseFormula.DefaultOverapproximateTS(new PNProperties()).overapproximate(ts);
			fail();
		} catch (RuntimeException e) {
			assertThat(e.getCause(), instanceOf(MissingLocationException.class));
		}
	}

	private void testOverapproximate(boolean multiple) {
		Tableau<State> tableau = mockTableau(false);
		TransitionSystem ts = getEmptyTS();
		MissingArcsFinder<State> missingArcsFinder = mock(MissingArcsFinder.class);
		RealiseFormula.ContinueTableauFactory continueTableauFactory
			= mock(RealiseFormula.ContinueTableauFactory.class);
		RealiseFormula.OverapproximateTS overapprox = spy(new NOPOverapproximateTS());
		NonRecursive engine = mock(NonRecursive.class);

		Pair<State, String> missingArc = new Pair<>(ts.getInitialState(), "a");
		when(missingArcsFinder.findMissing(tableau)).thenReturn(Collections.singleton(missingArc));
		when(continueTableauFactory.continueTableau(tableau)).thenReturn(Collections.singleton(tableau));

		new RealiseFormula.Worker(new RealiseFormula(
					new NeverCalledRealisationCallback(), missingArcsFinder,
					new StateWithSameNameTransformerFactory(), continueTableauFactory, overapprox,
					multiple ? 2 : 1), ts, tableau).walk(engine);

		verify(missingArcsFinder, times(multiple ? 2 : 1)).findMissing(tableau);
		verify(continueTableauFactory, times(multiple ? 3 : 2)).continueTableau((Tableau<State>) anyObject());
		verify(overapprox, times(1)).overapproximate((TransitionSystem) anyObject());
		verify(engine, times(1)).enqueue(Mockito.isA(RealiseFormula.Worker.class));
	}

	@Test
	public void testMultipleInvocationsWithoutOverapproximation() {
		testOverapproximate(true);
	}

	@Test
	public void testSingleInvocation() {
		testOverapproximate(false);
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
