package uniol.synthesis.adt.mu_calculus;

public enum FixedPoint {
	LEAST("mu"),
	GREATEST("nu");

	private final String toStringValue;

	FixedPoint(String toStringValue) {
		this.toStringValue = toStringValue;
	}

	@Override
	public String toString() {
		return toStringValue;
	}
}
