package uniol.synthesis.tableau;

import java.util.Collection;
import java.util.Collections;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.apt.adt.ts.State;

public class TrueTableau extends Tableau {
	public TrueTableau(State state, Formula formula) {
		super(state, formula);
	}

	@Override
	public boolean isSuccessful() {
		return true;
	}

	@Override
	public Collection<Tableau> getChildren() {
		return Collections.emptyList();
	}
}
