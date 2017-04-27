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

	@Test
	public void testValues() {
		assertThat(FixedPoint.values(), arrayContainingInAnyOrder(FixedPoint.GREATEST, FixedPoint.LEAST));
	}

	@Test
	public void testValueOf() {
		assertThat(FixedPoint.valueOf("LEAST"), is(FixedPoint.LEAST));
		assertThat(FixedPoint.valueOf("GREATEST"), is(FixedPoint.GREATEST));
	}
}
