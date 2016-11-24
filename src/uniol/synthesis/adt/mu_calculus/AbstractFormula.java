package uniol.synthesis.adt.mu_calculus;

import java.util.Deque;

import uniol.synthesis.util.NonRecursive;
import uniol.synthesis.util.PrintFormula;

public abstract class AbstractFormula implements Formula {
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		new NonRecursive().run(new PrintFormula(sb, this));
		return sb.toString();
	}
}
