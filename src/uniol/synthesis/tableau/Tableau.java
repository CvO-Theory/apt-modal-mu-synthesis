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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.Transformer;

import uniol.synthesis.adt.mu_calculus.Formula;

public class Tableau<S> {
	private final Collection<TableauNode<S>> leaves;
	private final Map<S, Set<Formula>> handledClosedFormulas;

	Tableau(Collection<TableauNode<S>> leaves, Map<S, Set<Formula>> handledClosedFormulas) {
		this.leaves = Collections.unmodifiableCollection(new ArrayList<TableauNode<S>>(leaves));
		// We assume that the sets in the Map are already unmodifiable
		this.handledClosedFormulas = Collections.unmodifiableMap(handledClosedFormulas);
	}

	public Collection<TableauNode<S>> getLeaves() {
		return leaves;
	}

	public boolean isSuccessful() {
		for (TableauNode<S> node : leaves)
			if (!node.isSuccessful())
				return false;
		return true;
	}

	public boolean alreadyHandled(S state, Formula formula) {
		Set<Formula> handled = handledClosedFormulas.get(state);
		if (handled == null)
			return false;
		return handled.contains(formula);
	}

	public Tableau<S> transform(final Transformer<S, S> transformer) {
		Collection<TableauNode<S>> result = new ArrayList<>(leaves.size());
		for (TableauNode<S> leave : leaves)
			result.add(leave.transform(transformer));

		Map<S, Set<Formula>> handled = new HashMap<>();
		for (Map.Entry<S, Set<Formula>> entry : handledClosedFormulas.entrySet()) {
			handled.put(transformer.transform(entry.getKey()), entry.getValue());
		}

		return new Tableau<S>(result, handled);
	}

	static public <S> Tableau<S> createInitialTableau(FollowArcs<S> followArcs, S state, Formula formula) {
		return new Tableau<S>(Collections.singleton(new TableauNode<S>(followArcs, state, formula)),
				Collections.<S, Set<Formula>>emptyMap());
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
