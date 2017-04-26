package uniol.synthesis.tableau;

import static java.util.Collections.singleton;

import uniol.apt.adt.ts.State;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static uniol.apt.util.matcher.Matchers.pairWith;

import uniol.synthesis.adt.mu_calculus.Event;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.Modality;

public class MissingArcsFinderTest {
	@Test
	public void testSimpleNoMissingArcs() {
		FormulaCreator creator = new FormulaCreator();

		TableauNode node = mock(TableauNode.class);
		when(node.getFormula()).thenReturn(creator.constant(true));

		Tableau tableau = mock(Tableau.class);
		when(tableau.getLeaves()).thenReturn(singleton(node));

		assertThat(new MissingArcsFinder().findMissing(tableau), empty());
	}

	@Test
	public void testUniversalMissingArc() {
		FormulaCreator creator = new FormulaCreator();
		State state = mock(State.class);

		TableauNode node = mock(TableauNode.class);
		when(node.getState()).thenReturn(state);
		when(node.getFormula()).thenReturn(
				creator.modality(Modality.UNIVERSAL, new Event("a"), creator.constant(true)));

		Tableau tableau = mock(Tableau.class);
		when(tableau.getLeaves()).thenReturn(singleton(node));

		assertThat(new MissingArcsFinder().findMissing(tableau), empty());
	}

	@Test
	public void testExistentialMissingArc() {
		FormulaCreator creator = new FormulaCreator();
		State state = mock(State.class);

		TableauNode node = mock(TableauNode.class);
		when(node.getState()).thenReturn(state);
		when(node.getFormula()).thenReturn(
				creator.modality(Modality.EXISTENTIAL, new Event("a"), creator.constant(true)));

		Tableau tableau = mock(Tableau.class);
		when(tableau.getLeaves()).thenReturn(singleton(node));

		assertThat(new MissingArcsFinder().findMissing(tableau), contains(pairWith(state, "a")));
	}
}
