package uniol.synthesis.tableau;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GraphvizProgressCallback implements TableauBuilder.ProgressCallback {
	private final StringBuilder builder = new StringBuilder("digraph img {\n");
	private final Map<TableauNode, String> mapping = new HashMap<>();
	private int nodeCounter = 0;
	private int tableauCounter = 0;

	private String mapNode(TableauNode node) {
		String result = mapping.get(node);
		if (result == null) {
			result = "s" + (nodeCounter++);
			mapping.put(node, result);
			builder.append(result).append("[label=\"").append(node.getState()).append(", ");
			builder.append(node.getFormula()).append("\"];\n");
		}
		return result;
	}

	@Override
	public void children(TableauNode node, Set<Set<TableauNode>> children) {
		String from = mapNode(node);
		if (children == null) {
			builder.append(from).append(" -> fail;\n");
		} else {
			for (Set<TableauNode> set : children) {
				for (TableauNode child : set) {
					String target = mapNode(child);
					builder.append(from).append(" -> ").append(target).append(";\n");
				}
			}
		}
	}

	public void tableau(Tableau tableau) {
		String id = "t" + (tableauCounter++) + (tableau.isSuccessful() ? "success" : "fail");
		for (TableauNode leave : tableau.getLeaves()) {
			String target = mapNode(leave);
			builder.append(id).append(" -> ").append(target).append(";\n");
		}
	}

	@Override
	public String toString() {
		return builder.toString() + "}\n";
	}
}
