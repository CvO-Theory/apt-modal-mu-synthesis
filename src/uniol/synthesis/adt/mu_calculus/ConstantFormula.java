package uniol.synthesis.adt.mu_calculus;

import uniol.synthesis.util.FormulaCreator;

public class ConstantFormula extends AbstractFormula {
	private final boolean value;

	protected ConstantFormula(FormulaCreator creator, boolean value) {
		super(creator);
		this.value = value;
	}

	public boolean getValue() {
		return value;
	}

	public static ConstantFormula constant(FormulaCreator creator, boolean value) {
		int hashCode = Boolean.valueOf(value).hashCode();
		for (Formula formula : creator.getFormulasWithHashCode(hashCode)) {
			if (formula instanceof ConstantFormula) {
				ConstantFormula result = (ConstantFormula) formula;
				if (result.getValue() == value)
					return result;
			}
		}
		ConstantFormula result = new ConstantFormula(creator, value);
		creator.addFormulaInternal(hashCode, result);
		return result;
	}
}
