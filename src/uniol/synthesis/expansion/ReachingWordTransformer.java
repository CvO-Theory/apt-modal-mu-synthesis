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
