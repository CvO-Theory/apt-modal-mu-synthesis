package uniol.synthesis.tableau;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
}
