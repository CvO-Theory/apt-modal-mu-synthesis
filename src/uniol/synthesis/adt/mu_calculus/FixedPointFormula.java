package uniol.synthesis.adt.mu_calculus;

public class FixedPointFormula extends AbstractFormula {
	final private FixedPoint fixedPoint;
	final private Variable variable;
	final private Formula formula;

	protected FixedPointFormula(FixedPoint fixedPoint, Variable variable, Formula formula) {
		this.fixedPoint = fixedPoint;
		this.variable = variable;
		this.formula = formula;
	}

	public FixedPoint getFixedPoint() {
		return fixedPoint;
	}

	public Variable getVariable() {
		return variable;
	}

	public Formula getFormula() {
		return formula;
	}
}
