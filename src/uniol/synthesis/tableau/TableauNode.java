/*-
 * APT - Analysis of Petri Nets and labeled Transition systems
 * Copyright (C) 2017  Uli Schlachter
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package uniol.synthesis.tableau;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.Transformer;

import uniol.apt.util.Pair;

import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class TableauNode<S> {
	final private FollowArcs<S> followArcs;
	final private S state;
	final private Formula formula;
	final private Map<VariableFormula, FixedPointFormula> constantDefinitions;
	final private Set<Pair<S, VariableFormula>> expansionsAbove;

	private TableauNode(FollowArcs<S> followArcs, S state, Formula formula, Map<VariableFormula,
			FixedPointFormula> constantDefinitions, Set<Pair<S, VariableFormula>> expansionsAbove) {
		this.followArcs = followArcs;
		this.state = state;
		this.formula = formula;
		this.constantDefinitions = constantDefinitions;
		this.expansionsAbove = expansionsAbove;
	}

	public TableauNode(FollowArcs<S> followArcs, S state, Formula formula) {
		this.followArcs = followArcs;
		this.state = state;
		this.formula = formula;
		this.constantDefinitions = Collections.emptyMap();
		this.expansionsAbove = Collections.emptySet();
	}

	protected FollowArcs<S> getFollowArcs() {
		return followArcs;
	}

	public S getState() {
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
				followArcs.followArcs(state, modf.getEvent()).isEmpty();
		}
		if (formula instanceof ConstantFormula) {
			return ((ConstantFormula) formula).getValue();
		}
		return false;
	}

	public TableauNode<S> transform(Transformer<S, S> transformer) {
		Set<Pair<S, VariableFormula>> newExpansions = new HashSet<>();
		for (Pair<S, VariableFormula> pair : this.expansionsAbove) {
			newExpansions.add(new Pair<S, VariableFormula>(
						transformer.transform(pair.getFirst()), pair.getSecond()));
		}
		newExpansions = Collections.unmodifiableSet(newExpansions);
		return new TableauNode<S>(followArcs, transformer.transform(state), formula, this.constantDefinitions,
				newExpansions);
	}

	public TableauNode<S> createChild(S st, Formula fm) {
		return new TableauNode<S>(followArcs, st, fm, constantDefinitions, expansionsAbove);
	}

	public TableauNode<S> createChild(Formula fm) {
		return new TableauNode<S>(followArcs, this.state, fm, constantDefinitions, expansionsAbove);
	}

	public TableauNode<S> addExpansion(VariableFormula var, FixedPointFormula inner) {
		Map<VariableFormula, FixedPointFormula> newConstantDefinitions = new HashMap<>(constantDefinitions);
		Formula old = newConstantDefinitions.put(var, inner);
		if (old != null)
			throw new IllegalArgumentException();
		newConstantDefinitions = Collections.unmodifiableMap(newConstantDefinitions);
		return new TableauNode<S>(followArcs, this.state, var, newConstantDefinitions, this.expansionsAbove);
	}

	public TableauNode<S> recordExpansion(VariableFormula var, Formula inner) {
		Set<Pair<S, VariableFormula>> newExpansions = new HashSet<>(expansionsAbove);
		Pair<S, VariableFormula> pair = new Pair<>(state, var);
		newExpansions.add(pair);
		newExpansions = Collections.unmodifiableSet(newExpansions);
		return new TableauNode<S>(followArcs, this.state, inner, this.constantDefinitions, newExpansions);
	}

	public boolean wasAlreadyExpanded() {
		if (!(formula instanceof VariableFormula))
			return false;
		VariableFormula var = (VariableFormula) formula;
		return expansionsAbove.contains(new Pair<S, VariableFormula>(state, var));
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
		@SuppressWarnings("unchecked")
		TableauNode<? extends Object> other = (TableauNode) o;
		return this.state.equals(other.state) &&
			this.formula.equals(other.formula) &&
			this.constantDefinitions.equals(other.constantDefinitions) &&
			this.expansionsAbove.equals(other.expansionsAbove);
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
