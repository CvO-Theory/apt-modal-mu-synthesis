package uniol.synthesis.tableau;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.collections4.BidiMap;

import uniol.synthesis.util.NonRecursive;

public class TableauPrinter {
	private int counter;
	private BidiMap<Tableau, String> identifiers = new DualHashBidiMap<>();

	public String print(Tableau tableau) {
		StringBuilder sb = new StringBuilder();
		print(sb, tableau);
		return sb.toString();
	}

	public void print(StringBuilder sb, Tableau tableau) {
		counter = 0;
		identifiers.clear();

		sb.append("digraph tableau {");
		NonRecursive engine = new NonRecursive();
		getIdentifier(engine, sb, tableau);
		engine.run();
		sb.append("}");
	}

	private String getIdentifier(NonRecursive engine, StringBuilder sb, Tableau tableau) {
		String result = identifiers.get(tableau);
		if (result == null) {
			do {
				result = "t" + ++counter;
			} while (identifiers.getKey(result) != null);
			identifiers.put(tableau, result);
			engine.enqueue(new Printer(sb, tableau));
		}
		return result;
	}

	private class Printer implements NonRecursive.Walker {
		final private StringBuilder sb;
		final private Tableau tableau;

		public Printer(StringBuilder sb, Tableau tableau) {
			this.sb = sb;
			this.tableau = tableau;
		}

		@Override
		public void walk(NonRecursive engine) {
			String id = getIdentifier(engine, sb, tableau);

			// Set up this node
			sb.append(id);
			sb.append("[label=\"");
			sb.append(tableau.getFormula());
			sb.append("\", xlabel=\"");
			sb.append(tableau.getState().getId());
			sb.append(", ");
			sb.append(tableau.isSuccessful());
			sb.append("\"]\n");

			for (Tableau child : tableau.getChildren()) {
				String otherId = getIdentifier(engine, sb, child);
				sb.append(id);
				sb.append(" -> ");
				sb.append(otherId);
				sb.append(";\n");
			}
		}
	}
}
