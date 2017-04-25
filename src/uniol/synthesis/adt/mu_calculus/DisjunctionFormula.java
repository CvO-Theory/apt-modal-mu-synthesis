package uniol.synthesis.adt.mu_calculus;

public class DisjunctionFormula extends AbstractFormula {
	final private Formula left;
	final private Formula right;

	protected DisjunctionFormula(FormulaCreator creator, Formula left, Formula right) {
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

	static DisjunctionFormula disjunction(FormulaCreator creator, Formula left, Formula right) {
		int hashCode = ~(left.hashCode() ^ Integer.rotateLeft(right.hashCode(), 16));
		for (Formula formula : creator.getFormulasWithHashCode(hashCode)) {
			if (formula instanceof DisjunctionFormula) {
				DisjunctionFormula result = (DisjunctionFormula) formula;
				if (result.getLeft().equals(left) && result.getRight().equals(right))
					return result;
			}
		}
		DisjunctionFormula result = new DisjunctionFormula(creator, left, right);
		creator.addFormulaInternal(hashCode, result);
		return result;
	}
}
