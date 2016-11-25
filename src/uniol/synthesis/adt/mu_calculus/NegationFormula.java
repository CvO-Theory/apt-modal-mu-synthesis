package uniol.synthesis.adt.mu_calculus;

import uniol.synthesis.util.FormulaCreator;

public class NegationFormula extends AbstractFormula {
	final private Formula formula;

	protected NegationFormula(FormulaCreator creator, Formula formula) {
		super(creator);
		this.formula = formula;
	}

	public Formula getFormula() {
		return formula;
	}

	public static NegationFormula negate(FormulaCreator creator, Formula innerFormula) {
		int hashCode = ~innerFormula.hashCode();
		for (Formula formula : creator.getFormulasWithHashCode(hashCode)) {
			if (formula instanceof NegationFormula) {
				NegationFormula result = (NegationFormula) formula;
				if (result.getFormula().equals(innerFormula))
					return result;
			}
		}
		NegationFormula result = new NegationFormula(creator, innerFormula);
		creator.addFormulaInternal(hashCode, result);
		return result;
	}
}
