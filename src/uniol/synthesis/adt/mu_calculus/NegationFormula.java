package uniol.synthesis.adt.mu_calculus;

public class NegationFormula extends AbstractFormula {
	final private Formula formula;

	protected NegationFormula(Formula formula) {
		this.formula = formula;
	}

	public Formula getFormula() {
		return formula;
	}
}
