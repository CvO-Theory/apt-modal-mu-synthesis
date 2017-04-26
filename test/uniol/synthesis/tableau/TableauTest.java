package uniol.synthesis.tableau;

import java.util.Collections;
import static java.util.Arrays.asList;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uniol.synthesis.tableau.TableauMatchers.*;

public class TableauTest {
	@Test
	public void testEmpty() {
		Tableau t = new Tableau(Collections.<TableauNode>emptySet());
		assertThat(t.getLeaves(), empty());
	}

	@Test
	public void testNotEmpty() {
		TableauNode n1 = mock(TableauNode.class);
		TableauNode n2 = mock(TableauNode.class);
		Tableau t = new Tableau(asList(n1, n2));
		assertThat(t.getLeaves(), containsInAnyOrder(n1, n2));
	}

	@Test
	public void testSuccessfulEmpty() {
		Tableau t = new Tableau(Collections.<TableauNode>emptySet());
		assertThat(t, isSuccessfulTableau(true));
	}

	@Test
	public void testSuccessfulOneSuccess() {
		TableauNode n1 = mock(TableauNode.class);
		when(n1.isSuccessful()).thenReturn(true);
		Tableau t = new Tableau(asList(n1));
		assertThat(t, isSuccessfulTableau(true));
	}

	@Test
	public void testSuccessfulOneFails() {
		TableauNode n1 = mock(TableauNode.class);
		when(n1.isSuccessful()).thenReturn(false);
		Tableau t = new Tableau(asList(n1));
		assertThat(t, isSuccessfulTableau(false));
	}

	@Test
	public void testSuccessfulOneOutOfThreeFails() {
		TableauNode n1 = mock(TableauNode.class);
		TableauNode n2 = mock(TableauNode.class);
		TableauNode n3 = mock(TableauNode.class);
		when(n1.isSuccessful()).thenReturn(true);
		when(n2.isSuccessful()).thenReturn(false);
		when(n3.isSuccessful()).thenReturn(true);
		Tableau t = new Tableau(asList(n1, n2, n3));
		assertThat(t, isSuccessfulTableau(false));
	}
}
