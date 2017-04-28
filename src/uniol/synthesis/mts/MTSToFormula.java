package uniol.synthesis.mts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.ts.Arc;
import uniol.apt.adt.ts.State;
import uniol.apt.adt.ts.TransitionSystem;

import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
import uniol.synthesis.util.SolveEquationSystem;

public class MTSToFormula {
	protected Map<State, VariableFormula> createVariables(FormulaCreator creator, TransitionSystem mts) {
		Map<State, VariableFormula> result = new HashMap<>();
		for (State state : mts.getNodes())
			result.put(state, creator.variable(state.getId()));
		return result;
	}

	protected Collection<Arc> filterMustArcs(Collection<Arc> arcs) {
		Collection<Arc> result = new ArrayList<>(arcs.size());
		for (Arc arc : arcs)
			if (!arc.hasExtension("may"))
				result.add(arc);
		return result;
	}

	protected Formula conjunction(Formula a, Formula b) {
		Formula True = a.getCreator().constant(true);
		if (a.equals(True))
			return b;
		if (b.equals(True))
			return a;
		return a.getCreator().conjunction(a, b);
	}

	protected Formula disjunction(Formula a, Formula b) {
		Formula False = a.getCreator().constant(false);
		if (a.equals(False))
			return b;
		if (b.equals(False))
			return a;
		return a.getCreator().disjunction(a, b);
	}

	protected Formula stateToFormula(FormulaCreator creator, State state, Map<State, VariableFormula> variables) {
		Formula result = creator.constant(true);
		for (String event : state.getGraph().getAlphabet()) {
			Collection<Arc> mayArcs = state.getPostsetEdgesByLabel(event);
			Collection<Arc> mustArcs = filterMustArcs(mayArcs);

			// Each must arc must have an implementation, so we have a conjunction of existential modalities
			Formula mustFormula = creator.constant(true);
			for (Arc arc : mustArcs) {
				VariableFormula target = variables.get(arc.getTarget());
				Formula part = creator.modality(Modality.EXISTENTIAL, event, target);
				mustFormula = conjunction(mustFormula, part);
			}

			// Each arc must be allowed by a may arc, so we have a disjunction inside a universal modality
			Formula mayFormula = creator.constant(false);
			for (Arc arc : mayArcs) {
				Formula part = variables.get(arc.getTarget());
				mayFormula = disjunction(mayFormula, part);
			}
			mayFormula = creator.modality(Modality.UNIVERSAL, event, mayFormula);

			Formula formula = conjunction(mustFormula, mayFormula);
			result = conjunction(result, formula);
		}
		return result;
	}

	protected Map<VariableFormula, Formula> mtsToEquationSystem(FormulaCreator creator, TransitionSystem mts,
			Map<State, VariableFormula> variables) {
		Map<VariableFormula, Formula> result = new HashMap<>();
		for (State state : mts.getNodes()) {
			result.put(variables.get(state), stateToFormula(creator, state, variables));
		}
		return result;
	}

	public Formula mtsToFormula(FormulaCreator creator, TransitionSystem mts) {
		Map<State, VariableFormula> variables = createVariables(creator, mts);
		return new SolveEquationSystem().solve(FixedPoint.GREATEST,
				mtsToEquationSystem(creator, mts, variables))
			.get(variables.get(mts.getInitialState()));
	}
}
