package uniol.synthesis.adt.mu_calculus;

public abstract class BinaryFormula extends AbstractFormula {
	final private Formula left;
	final private Formula right;

	protected BinaryFormula(Formula left, Formula right) {
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
