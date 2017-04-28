package uniol.synthesis.tableau;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.Transformer;

import uniol.apt.adt.ts.State;
import uniol.apt.util.Pair;

import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class TableauNode {
	final private State state;
	final private Formula formula;
	final private Map<VariableFormula, FixedPointFormula> constantDefinitions;
	final private Set<Pair<State, VariableFormula>> expansionsAbove;

	private TableauNode(State state, Formula formula, Map<VariableFormula, FixedPointFormula> constantDefinitions,
			Set<Pair<State, VariableFormula>> expansionsAbove) {
		this.state = state;
		this.formula = formula;
		this.constantDefinitions = Collections.unmodifiableMap(constantDefinitions);
		this.expansionsAbove = Collections.unmodifiableSet(expansionsAbove);
	}

	public TableauNode(State state, Formula formula) {
		this.state = state;
		this.formula = formula;
		this.constantDefinitions = Collections.emptyMap();
		this.expansionsAbove = Collections.emptySet();
	}

	public State getState() {
		return state;
	}

	public Formula getFormula() {
		return formula;
	}

	/// Return true if this node as a leave corresponds to a successful tableau
	public boolean isSuccessful() {
		if (formula instanceof ModalityFormula) {
			ModalityFormula modf = (ModalityFormula) formula;
			return modf.getModality().equals(Modality.UNIVERSAL) &&
				state.getPostsetEdgesByLabel(modf.getEvent()).isEmpty();
		}
		if (formula instanceof ConstantFormula) {
			return ((ConstantFormula) formula).getValue();
		}
		return false;
	}

	public TableauNode transform(Transformer<State, State> transformer) {
		Set<Pair<State, VariableFormula>> newExpansions = new HashSet<>();
		for (Pair<State, VariableFormula> pair : this.expansionsAbove) {
			newExpansions.add(new Pair<State, VariableFormula>(
						transformer.transform(pair.getFirst()), pair.getSecond()));
		}
		return new TableauNode(transformer.transform(state), formula, this.constantDefinitions, newExpansions);
	}

	public TableauNode createChild(State st, Formula fm) {
		return new TableauNode(st, fm, constantDefinitions, expansionsAbove);
	}

	public TableauNode createChild(Formula fm) {
		return new TableauNode(this.state, fm, constantDefinitions, expansionsAbove);
	}

	public TableauNode addExpansion(VariableFormula var, FixedPointFormula inner) {
		Map<VariableFormula, FixedPointFormula> newConstantDefinitions = new HashMap<>(constantDefinitions);
		Formula old = newConstantDefinitions.put(var, inner);
		if (old != null)
			throw new IllegalArgumentException();
		return new TableauNode(this.state, var, newConstantDefinitions, this.expansionsAbove);
	}

	public TableauNode recordExpansion(VariableFormula var, Formula inner) {
		Set<Pair<State, VariableFormula>> newExpansions = new HashSet<>(expansionsAbove);
		Pair<State, VariableFormula> pair = new Pair<>(state, var);
		newExpansions.add(pair);
		return new TableauNode(this.state, inner, this.constantDefinitions, newExpansions);
	}

	public boolean wasAlreadyExpanded() {
		if (!(formula instanceof VariableFormula))
			return false;
		VariableFormula var = (VariableFormula) formula;
		return expansionsAbove.contains(new Pair<State, VariableFormula>(state, var));
	}

	public FixedPointFormula getDefinition(VariableFormula var) {
		return constantDefinitions.get(var);
	}

	@Override
	public int hashCode() {
		int result = 0;
		result = result * 37 + state.hashCode();
		result = result * 37 + formula.hashCode();
		result = result * 37 + constantDefinitions.hashCode();
		result = result * 37 + expansionsAbove.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TableauNode))
			return false;
		TableauNode other = (TableauNode) o;
		return this.state.equals(other.state) &&
			this.formula.equals(other.formula) &&
			this.constantDefinitions.equals(other.constantDefinitions) &&
			this.expansionsAbove.equals(other.expansionsAbove);
	}
}
