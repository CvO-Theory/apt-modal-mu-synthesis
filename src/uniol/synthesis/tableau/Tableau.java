package uniol.synthesis.tableau;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.Transformer;

import uniol.apt.adt.ts.State;

public class Tableau {
	private final Set<TableauNode> leaves;

	public Tableau(Collection<TableauNode> leaves) {
		this.leaves = Collections.unmodifiableSet(new HashSet<TableauNode>(leaves));
	}

	public Set<TableauNode> getLeaves() {
		return leaves;
	}

	public boolean isSuccessful() {
		for (TableauNode node : leaves)
			if (!node.isSuccessful())
				return false;
		return true;
	}

	public Tableau transform(final Transformer<State, State> transformer) {
		Set<TableauNode> result = new HashSet<>();
		for (TableauNode leave : leaves)
			result.add(leave.transform(transformer));
		return new Tableau(result);
	}
}
