package uniol.synthesis.expansion;

import java.util.HashSet;
import java.util.Set;

import uniol.apt.adt.ts.State;
import uniol.apt.util.Pair;

import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.tableau.Tableau;
import uniol.synthesis.tableau.TableauNode;

public class MissingArcsFinder {
	public Set<Pair<State, String>> findMissing(Tableau tableau) {
		Set<Pair<State, String>> result = new HashSet<>();

		for (TableauNode node : tableau.getLeaves()) {
			Formula formula = node.getFormula();
			if (!(formula instanceof ModalityFormula))
				continue;

			ModalityFormula modality = (ModalityFormula) formula;
			if (modality.getModality().equals(Modality.UNIVERSAL))
				continue;

			Pair<State, String> pair = new Pair<>(node.getState(), modality.getEvent());
			result.add(pair);
		}

		return result;
	}
}
