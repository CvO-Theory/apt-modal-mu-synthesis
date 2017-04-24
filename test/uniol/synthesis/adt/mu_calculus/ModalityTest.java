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
		Event event = new Event("event");
		assertThat(Modality.UNIVERSAL.toString(event), equalTo("[event]"));
		assertThat(Modality.EXISTENTIAL.toString(event), equalTo("<event>"));
	}
}
