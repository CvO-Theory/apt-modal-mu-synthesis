package uniol.synthesis.adt.mu_calculus;

public class DisjunctionFormula extends AbstractFormula {
	final private Formula left;
	final private Formula right;

	protected DisjunctionFormula(Formula left, Formula right) {
		this.left = left;
		this.right = right;
	}

	public Formula getRight() {
		return right;
	}

	public Formula getLeft() {
		return left;
	}
}
