package uniol.synthesis.tableau;

import java.util.Collection;
import java.util.Collections;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.apt.adt.ts.State;

public class FalseTableau extends Tableau {
	public FalseTableau(State state, Formula formula) {
		super(state, formula);
	}

	@Override
	public boolean isSuccessful() {
		return false;
	}

	@Override
	public Collection<Tableau> getChildren() {
		return Collections.emptyList();
	}
}
