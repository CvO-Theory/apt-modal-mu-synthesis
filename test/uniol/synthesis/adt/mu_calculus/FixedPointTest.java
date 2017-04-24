package uniol.synthesis.adt.mu_calculus;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class FixedPointTest {
	@Test
	public void testNegate() {
		assertThat(FixedPoint.LEAST.negate(), equalTo(FixedPoint.GREATEST));
		assertThat(FixedPoint.GREATEST.negate(), equalTo(FixedPoint.LEAST));
	}

	@Test
	public void testToString() {
		assertThat(FixedPoint.LEAST, hasToString("mu"));
		assertThat(FixedPoint.GREATEST, hasToString("nu"));
	}
}
