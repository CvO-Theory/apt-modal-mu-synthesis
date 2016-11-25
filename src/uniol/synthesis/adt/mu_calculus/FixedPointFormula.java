package uniol.synthesis.adt.mu_calculus;

import uniol.synthesis.util.FormulaCreator;

public class FixedPointFormula extends AbstractFormula {
	final private FixedPoint fixedPoint;
	final private VariableFormula variable;
	final private Formula formula;

	protected FixedPointFormula(FormulaCreator creator, FixedPoint fixedPoint, VariableFormula variable, Formula formula) {
		super(creator);
		this.fixedPoint = fixedPoint;
		this.variable = variable;
		this.formula = formula;
	}

	public FixedPoint getFixedPoint() {
		return fixedPoint;
	}

	public VariableFormula getVariable() {
		return variable;
	}

	public Formula getFormula() {
		return formula;
	}

	public static FixedPointFormula fixedPoint(FormulaCreator creator, FixedPoint fixedPoint, VariableFormula variable, Formula innerFormula) {
		int hashCode = fixedPoint.hashCode() ^ variable.hashCode() ^ innerFormula.hashCode();
		for (Formula formula : creator.getFormulasWithHashCode(hashCode)) {
			if (formula instanceof FixedPointFormula) {
				FixedPointFormula result = (FixedPointFormula) formula;
				if (result.getFixedPoint().equals(fixedPoint) && result.getVariable().equals(variable)
						&& result.getFormula().equals(innerFormula))
					return result;
			}
		}
		FixedPointFormula result = new FixedPointFormula(creator, fixedPoint, variable, innerFormula);
		creator.addFormulaInternal(hashCode, result);
		return result;
	}
}
