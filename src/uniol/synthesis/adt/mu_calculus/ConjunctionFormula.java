package uniol.synthesis.adt.mu_calculus;

public class ConjunctionFormula extends AbstractFormula {
	final private Formula left;
	final private Formula right;

	protected ConjunctionFormula(FormulaCreator creator, Formula left, Formula right) {
		super(creator);
		this.left = left;
		this.right = right;
	}

	public Formula getRight() {
		return right;
	}

	public Formula getLeft() {
		return left;
	}

	static ConjunctionFormula conjunction(FormulaCreator creator, Formula left, Formula right) {
		int hashCode = left.hashCode() ^ Integer.rotateLeft(right.hashCode(), 16);
		for (Formula formula : creator.getFormulasWithHashCode(hashCode)) {
			if (formula instanceof ConjunctionFormula) {
				ConjunctionFormula result = (ConjunctionFormula) formula;
				if (result.getLeft().equals(left) && result.getRight().equals(right))
					return result;
			}
		}
		ConjunctionFormula result = new ConjunctionFormula(creator, left, right);
		creator.addFormulaInternal(hashCode, result);
		return result;
	}
}
