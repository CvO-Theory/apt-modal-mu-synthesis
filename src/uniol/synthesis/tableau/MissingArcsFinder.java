package uniol.synthesis.tableau;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.ts.State;
import uniol.apt.util.Pair;

import uniol.synthesis.util.NonRecursive;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;

public class MissingArcsFinder {
	public Set<Set<Pair<State, String>>> getMissingArcs(Tableau tableau) {
		Finder finder = new Finder(tableau);
		new NonRecursive().run(finder);
		return Collections.unmodifiableSet(finder.missingArcs);
	}

	private class Finder implements NonRecursive.Walker {
		private final Tableau tableau;
		private final Map<Tableau, Finder> children = new HashMap<>();
		private final Set<Set<Pair<State, String>>> missingArcs = new HashSet<>();

		public Finder(Tableau tableau) {
			this.tableau = tableau;
		}

		@Override
		public void walk(NonRecursive engine) {
			boolean hadMissing = false;
			for (Tableau child : tableau.getChildren()) {
				if (children.containsKey(child))
					continue;

				if (!hadMissing) {
					engine.enqueue(this);
					hadMissing = true;
				}
				Finder finder = new Finder(child);
				children.put(child, finder);
				engine.enqueue(finder);
			}

			if (hadMissing)
				return;

			if (children.isEmpty()) {
				// We are a leaf! Is there some missing arc?
				if (tableau instanceof ExistentialTableau) {
					assert !tableau.isSuccessful();
					Formula formula = tableau.getFormula();
					assert formula instanceof ModalityFormula;
					ModalityFormula modalityFormula = (ModalityFormula) formula;
					String event = modalityFormula.getEvent().getLabel();

					Set<Pair<State, String>> missing = new HashSet<>();
					missing.add(new Pair<State, String>(tableau.getState(), event));
					missingArcs.add(Collections.unmodifiableSet(missing));
				}
			} else {
				// Collect missing arcs from children

				// Are we a disjunction or a conjunction?
				boolean disjunction = tableau instanceof ExistentialTableau;

				if (disjunction) {
					for (Finder finder : children.values()) {
						missingArcs.addAll(finder.missingArcs);
					}
				} else {
					List<Set<Set<Pair<State, String>>>> allMissingArcs = new ArrayList<>();
					for (Finder finder : children.values()) {
						if (!finder.missingArcs.isEmpty())
							allMissingArcs.add(new HashSet<>(finder.missingArcs));
					}
					missingArcs.addAll(handleConjunction(allMissingArcs));
				}
			}
		}

		private Set<Set<Pair<State, String>>> handleConjunction(List<Set<Set<Pair<State, String>>>> allMissingArcs) {
			if (allMissingArcs.isEmpty())
				return Collections.emptySet();
			if (allMissingArcs.size() == 1)
				return allMissingArcs.iterator().next();

			Iterator<Set<Set<Pair<State, String>>>> iterator = allMissingArcs.iterator();
			Set<Set<Pair<State, String>>> missing = iterator.next();
			iterator.remove();

			Set<Set<Pair<State, String>>> result = new HashSet<>();
			for (Set<Pair<State, String>> otherMissing : handleConjunction(allMissingArcs)) {
				for (Set<Pair<State, String>> own : missing) {
					Set<Pair<State, String>> adding = new HashSet<>();
					adding.addAll(otherMissing);
					adding.addAll(own);
					result.add(Collections.unmodifiableSet(adding));
				}
			}
			return result;
		}
	}
}
