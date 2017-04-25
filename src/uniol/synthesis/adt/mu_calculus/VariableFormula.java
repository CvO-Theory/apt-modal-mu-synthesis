package uniol.synthesis.adt.mu_calculus;

public class VariableFormula extends AbstractFormula {
	private final String var;

	protected VariableFormula(FormulaCreator creator, String var) {
		super(creator);
		this.var = var;
	}

	public String getVariable() {
		return var;
	}

	static VariableFormula variable(FormulaCreator creator, String var) {
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
