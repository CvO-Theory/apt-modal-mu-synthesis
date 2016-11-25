package uniol.synthesis.adt.mu_calculus;

public enum Modality {
	UNIVERSAL,
	EXISTENTIAL;

	public Modality negate() {
		switch (this) {
			case UNIVERSAL:
				return EXISTENTIAL;
			case EXISTENTIAL:
				return UNIVERSAL;
			default:
				throw new AssertionError();
		}
	}

	public String toString(Event event) {
		switch (this) {
			case UNIVERSAL:
				return "[" + event + "]";
			case EXISTENTIAL:
				return "<" + event + ">";
			default:
				throw new AssertionError();
		}
	}
}
