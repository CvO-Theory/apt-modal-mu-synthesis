package uniol.synthesis.tableau;

import java.util.Collections;
import static java.util.Arrays.asList;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

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
}
