package uniol.synthesis.tableau;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import uniol.apt.adt.ts.State;
import uniol.synthesis.adt.mu_calculus.Formula;

public class UniversalTableau extends Tableau {
	private final Set<Tableau> children;

	public UniversalTableau(State state, Formula formula, Set<Tableau> children) {
		super(state, formula);
		this.children = children;
	}

	@Override
	public boolean isSuccessful() {
		for (Tableau child : children)
			if (!child.isSuccessful())
				return false;
		return true;
	}

	@Override
	public Collection<Tableau> getChildren() {
		return Collections.unmodifiableSet(children);
	}
}
