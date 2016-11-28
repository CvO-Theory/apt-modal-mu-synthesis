package uniol.synthesis.tableau;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

import uniol.apt.adt.ts.State;
import uniol.apt.util.Pair;

import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class DefinitionList {
	private final Map<VariableFormula, FixedPointFormula> mappings = new HashMap<>();
	private final Set<Pair<State, VariableFormula>> expansions = new HashSet<>();

	public DefinitionList() {
	}

	private DefinitionList(DefinitionList other) {
		mappings.putAll(other.mappings);
		expansions.addAll(other.expansions);
	}

	public FixedPointFormula getFixedPointFormula(VariableFormula formula) {
		return mappings.get(formula);
	}

	public boolean wasAlreadyExpanded(State state, VariableFormula formula) {
		return expansions.contains(new Pair<State, VariableFormula>(state, formula));
	}

	public DefinitionList noteExpansion(State state, VariableFormula formula) {
		DefinitionList result = new DefinitionList(this);
		Pair<State, VariableFormula> pair = new Pair<>(state, formula);
		boolean changed = result.expansions.add(pair);
		if (!changed)
			throw new IllegalArgumentException();
		return result;
	}

	public DefinitionList expand(VariableFormula constant, FixedPointFormula formula) {
		DefinitionList result = new DefinitionList(this);
		Object old = result.mappings.put(constant, formula);
		if (old != null)
			throw new IllegalArgumentException();
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof DefinitionList))
			return false;
		DefinitionList other = (DefinitionList) o;
		return mappings.equals(other.mappings) && expansions.equals(other.expansions);
	}

	@Override
	public int hashCode() {
		return mappings.hashCode() ^ expansions.hashCode();
	}
}
