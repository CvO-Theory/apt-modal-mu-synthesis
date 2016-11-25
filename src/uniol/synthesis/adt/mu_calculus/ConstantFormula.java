package uniol.synthesis.adt.mu_calculus;

public class ConstantFormula extends AbstractFormula {
	private final boolean value;

	protected ConstantFormula(boolean value) {
		this.value = value;
	}

	public boolean getValue() {
		return value;
	}
}
