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

	static private Tableau mockTableau(boolean successfull) {
		Tableau tableau = mock(Tableau.class);
		when(tableau.isSuccessful()).thenReturn(successfull);
		return tableau;
	}

	static private TransitionSystem getEmptyTS() {
		TransitionSystem ts = new TransitionSystem();
		ts.setInitialState(ts.createState("s0"));
		return ts;
	}

	@Test
	public void testSuccessfulTableau() {
		Tableau tableau = mockTableau(true);
		TransitionSystem ts = mock(TransitionSystem.class);
		State state = mock(State.class);
		when(ts.getInitialState()).thenReturn(state);

		Set<Pair<TransitionSystem, Tableau>> result = new HashSet<>();
		new RealiseFormula(result, ts, tableau, null, null, null, null).walk(null);

		assertThat(result, contains(pairWith(ts, tableau)));
	}

	@Test
	public void testUnsuccessfulTableauNoMissingArcs() {
		// This test tries to expand based on the unsuccessful tableau. However, we pretend that no arcs are
		// missing and that no expanded tableaus exist, thus the code under test doesn't actually do much.

		Tableau tableau = mockTableau(false);
		TransitionSystem ts = getEmptyTS();
		MissingArcsFinder missingArcsFinder = mock(MissingArcsFinder.class);
		RealiseFormula.ContinueTableauFactory continueTableauFactory
			= mock(RealiseFormula.ContinueTableauFactory.class);

		when(missingArcsFinder.findMissing(tableau)).thenReturn(Collections.<Pair<State, String>>emptySet());
		when(continueTableauFactory.continueTableau(tableau)).thenReturn(Collections.<Tableau>emptySet());

		Set<Pair<TransitionSystem, Tableau>> result = new HashSet<>();
		new RealiseFormula(result, ts, tableau, missingArcsFinder, new StateWithSameNameTransformerFactory(),
				continueTableauFactory, new NOPOverapproximateTS()).walk(null);

		assertThat(result, empty());
		verify(missingArcsFinder, times(1)).findMissing(tableau);
		verify(continueTableauFactory, times(1)).continueTableau((Tableau) anyObject());
	}

	static private TransitionSystem getExpandedTS(TransitionSystem ts, Set<Pair<State, String>> missingArcs) {
		Tableau tableau = mockTableau(false);
		MissingArcsFinder missingArcsFinder = mock(MissingArcsFinder.class);
		RealiseFormula.ContinueTableauFactory continueTableauFactory
			= mock(RealiseFormula.ContinueTableauFactory.class);
		RealiseFormula.OverapproximateTS overapproximateTS = mock(RealiseFormula.OverapproximateTS.class);

		when(missingArcsFinder.findMissing(tableau)).thenReturn(missingArcs);
		when(continueTableauFactory.continueTableau(tableau)).thenReturn(Collections.<Tableau>emptySet());

		new RealiseFormula(null, ts, tableau, missingArcsFinder, new StateWithSameNameTransformerFactory(),
				continueTableauFactory, overapproximateTS).walk(null);

		verify(missingArcsFinder, times(1)).findMissing(tableau);
		verify(continueTableauFactory, times(1)).continueTableau((Tableau) anyObject());

		ArgumentCaptor<TransitionSystem> expandedTSCaptor = ArgumentCaptor.forClass(TransitionSystem.class);
		verify(overapproximateTS).overapproximate(expandedTSCaptor.capture());
		return expandedTSCaptor.getValue();
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

		Tableau tableau = mockTableau(false);
		Tableau tableau1 = mockTableau(true);
		Tableau tableau2 = mockTableau(true);
		TransitionSystem ts = getEmptyTS();
		MissingArcsFinder missingArcsFinder = mock(MissingArcsFinder.class);
		RealiseFormula.ContinueTableauFactory continueTableauFactory
			= mock(RealiseFormula.ContinueTableauFactory.class);
		NonRecursive engine = mock(NonRecursive.class);

		when(missingArcsFinder.findMissing(tableau)).thenReturn(Collections.<Pair<State, String>>emptySet());
		when(continueTableauFactory.continueTableau((Tableau) anyObject())).thenReturn(new HashSet<Tableau>(
					Arrays.asList(tableau1, tableau2)));

		Set<Pair<TransitionSystem, Tableau>> result = new HashSet<>();
		new RealiseFormula(result, ts, tableau, missingArcsFinder, new StateWithSameNameTransformerFactory(),
				continueTableauFactory, new NOPOverapproximateTS()).walk(engine);

		assertThat(result, empty());
		verify(missingArcsFinder, times(1)).findMissing(tableau);
		verify(continueTableauFactory, times(1)).continueTableau((Tableau) anyObject());
		verify(engine, times(2)).enqueue(Mockito.isA(RealiseFormula.class));
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
}
