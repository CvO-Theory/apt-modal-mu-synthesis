package uniol.synthesis.adt.mu_calculus;

public enum ConstantFormula implements Formula {
	TRUE("true"),
	FALSE("false");

	private final String toStringValue;

	ConstantFormula(String toStringValue) {
		this.toStringValue = toStringValue;
	}

	@Override
	public String toString() {
		return toStringValue;
	}
}
