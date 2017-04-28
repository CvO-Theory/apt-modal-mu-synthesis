package uniol.synthesis.adt.mu_calculus;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ModalityTest {
	@Test
	public void testNegate() {
		assertThat(Modality.UNIVERSAL.negate(), equalTo(Modality.EXISTENTIAL));
		assertThat(Modality.EXISTENTIAL.negate(), equalTo(Modality.UNIVERSAL));
	}

	@Test
	public void testToString() {
		assertThat(Modality.UNIVERSAL.toString("event"), equalTo("[event]"));
		assertThat(Modality.EXISTENTIAL.toString("event"), equalTo("<event>"));
	}

	@Test
	public void testValues() {
		assertThat(Modality.values(), arrayContainingInAnyOrder(Modality.EXISTENTIAL, Modality.UNIVERSAL));
	}

	@Test
	public void testValueOf() {
		assertThat(Modality.valueOf("UNIVERSAL"), is(Modality.UNIVERSAL));
		assertThat(Modality.valueOf("EXISTENTIAL"), is(Modality.EXISTENTIAL));
	}
}
