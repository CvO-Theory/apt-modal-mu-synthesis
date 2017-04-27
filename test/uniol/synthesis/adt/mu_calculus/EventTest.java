package uniol.synthesis.adt.mu_calculus;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class EventTest {
	@Test
	public void testEquals() {
		Event a1 = new Event("a");
		Event a2 = new Event("a");
		Event b = new Event("b");
		Object o = new Object();

		assertThat(a1, equalTo(a2));
		assertThat(a1.hashCode(), equalTo(a2.hashCode()));
		assertThat(a1, not(equalTo(b)));
		assertThat(a1, not(equalTo(o)));
	}
}
