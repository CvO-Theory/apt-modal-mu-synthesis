package uniol.synthesis.adt.mu_calculus;

public abstract class UnaryFormula extends AbstractFormula {
	final private Formula formula;

	protected UnaryFormula(Formula formula) {
		this.formula = formula;
	}

	public Formula getFormula() {
		return formula;
	}
}
