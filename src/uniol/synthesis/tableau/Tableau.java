package uniol.synthesis.tableau;

import java.util.Collection;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.apt.adt.ts.State;

public abstract class Tableau {
	private final State state;
	private final Formula formula;

	public Tableau(State state, Formula formula) {
		this.state = state;
		this.formula = formula;
	}

	public State getState() {
		return state;
	}

	public Formula getFormula() {
		return formula;
	}

	public abstract boolean isSuccessful();
	public abstract Collection<Tableau> getChildren();
}
