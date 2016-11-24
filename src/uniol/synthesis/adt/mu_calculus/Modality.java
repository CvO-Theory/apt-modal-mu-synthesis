package uniol.synthesis.adt.mu_calculus;

public enum Modality {
	UNIVERSAL,
	EXISTENTIAL;

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
