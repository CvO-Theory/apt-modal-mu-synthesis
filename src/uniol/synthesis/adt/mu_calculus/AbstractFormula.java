package uniol.synthesis.adt.mu_calculus;

import java.util.Deque;

import uniol.synthesis.util.NonRecursive;
import uniol.synthesis.util.PrintFormula;

public abstract class AbstractFormula implements Formula {
	private final FormulaCreator creator;

	protected AbstractFormula(FormulaCreator creator) {
		this.creator = creator;
	}

	@Override
	public FormulaCreator getCreator() {
		return creator;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		PrintFormula.printFormula(sb, this);
		return sb.toString();
	}
}
