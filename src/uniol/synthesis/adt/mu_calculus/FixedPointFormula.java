package uniol.synthesis.adt.mu_calculus;

public class FixedPointFormula extends UnaryFormula {
	private final FixedPoint fixedPoint;
	private final Variable variable;

	protected FixedPointFormula(FixedPoint fixedPoint, Variable variable, Formula formula) {
		super(formula);
		this.fixedPoint = fixedPoint;
		this.variable = variable;
	}

	public FixedPoint getFixedPoint() {
		return fixedPoint;
	}

	public Variable getVariable() {
		return variable;
	}
}
