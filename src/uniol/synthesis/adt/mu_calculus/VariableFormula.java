package uniol.synthesis.adt.mu_calculus;

import uniol.synthesis.util.FormulaCreator;

public class VariableFormula extends AbstractFormula {
	private final Variable var;

	protected VariableFormula(FormulaCreator creator, Variable var) {
		super(creator);
		this.var = var;
	}

	public Variable getVariable() {
		return var;
	}

	public static VariableFormula variable(FormulaCreator creator, Variable var) {
		int hashCode = var.hashCode();
		for (Formula formula : creator.getFormulasWithHashCode(hashCode)) {
			if (formula instanceof VariableFormula) {
				VariableFormula result = (VariableFormula) formula;
				if (result.getVariable().equals(var))
					return result;
			}
		}
		VariableFormula result = new VariableFormula(creator, var);
		creator.addFormulaInternal(hashCode, result);
		return result;
	}
}
