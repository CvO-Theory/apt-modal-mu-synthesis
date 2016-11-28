package uniol.synthesis.tableau;

import java.util.Collection;
import java.util.Collections;

import uniol.apt.adt.ts.State;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class FixedPointTableau extends Tableau {
	private final Tableau child;

	public FixedPointTableau(State state, Formula formula, Tableau child) {
		super(state, formula);
		this.child = child;
	}

	@Override
	public boolean isSuccessful() {
		return child.isSuccessful();
	}

	@Override
	public Collection<Tableau> getChildren() {
		return Collections.singleton(child);
	}
}
