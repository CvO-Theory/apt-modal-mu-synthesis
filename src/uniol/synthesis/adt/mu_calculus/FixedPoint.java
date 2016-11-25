package uniol.synthesis.adt.mu_calculus;

public enum FixedPoint {
	LEAST("mu"),
	GREATEST("nu");

	private final String toStringValue;

	FixedPoint(String toStringValue) {
		this.toStringValue = toStringValue;
	}

	public FixedPoint negate() {
		switch (this) {
			case LEAST:
				return GREATEST;
			case GREATEST:
				return LEAST;
			default:
				throw new AssertionError();
		}
	}

	@Override
	public String toString() {
		return toStringValue;
	}
}
