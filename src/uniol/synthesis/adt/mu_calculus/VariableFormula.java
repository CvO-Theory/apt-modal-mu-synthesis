package uniol.synthesis.adt.mu_calculus;

public class VariableFormula extends AbstractFormula {
	private final Variable var;

	protected VariableFormula(Variable var) {
		this.var = var;
	}

	public Variable getVariable() {
		return var;
	}
}
