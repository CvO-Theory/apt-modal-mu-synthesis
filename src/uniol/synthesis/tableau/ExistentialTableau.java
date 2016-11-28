package uniol.synthesis.tableau;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import uniol.apt.adt.ts.State;
import uniol.synthesis.adt.mu_calculus.Formula;

public class ExistentialTableau extends Tableau {
	private final Set<Tableau> children;

	public ExistentialTableau(State state, Formula formula, Set<Tableau> children) {
		super(state, formula);
		this.children = children;
	}

	@Override
	public boolean isSuccessful() {
		for (Tableau child : children)
			if (child.isSuccessful())
				return true;
		return false;
	}

	@Override
	public Collection<Tableau> getChildren() {
		return Collections.unmodifiableSet(children);
	}
}
