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

package uniol.synthesis.expansion;

import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.Transformer;

import uniol.apt.adt.ts.Arc;
import uniol.apt.adt.ts.State;
import uniol.apt.adt.ts.TransitionSystem;
import uniol.apt.util.SpanningTree;

public class ReachingWordTransformer implements Transformer<State, State> {
	private final TransitionSystem to;

	public ReachingWordTransformer(TransitionSystem to) {
		this.to = to;
	}

	@Override
	public State transform(State state) {
		TransitionSystem from = state.getGraph();
		State currentState = to.getInitialState();

		if (state.equals(from.getInitialState()))
			return currentState;

		SpanningTree<TransitionSystem, Arc, State> fromTree = SpanningTree.get(from, from.getInitialState());
		List<Arc> edges = fromTree.getEdgePathFromStart(state);

		if (edges.isEmpty())
			throw new IllegalArgumentException("Node " + state + " is unreachable");

		for (Arc arc : fromTree.getEdgePathFromStart(state)) {
			Set<State> next = currentState.getPostsetNodesByLabel(arc.getLabel());
			if (next.size() != 1)
				throw new IllegalArgumentException("State " + currentState +
						" does not have exactly one outgoing arc with label " + arc.getLabel());
			currentState = next.iterator().next();
		}
		return currentState;
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
