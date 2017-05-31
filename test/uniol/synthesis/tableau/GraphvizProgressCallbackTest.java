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
import static java.util.Collections.singleton;

import uniol.apt.adt.ts.State;

import uniol.synthesis.adt.mu_calculus.Formula;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class GraphvizProgressCallbackTest {
	static private TableauNode<State> createTableau(String node, String formula) {
		TableauNode<State> result = mock(TableauNode.class);
		State state = mock(State.class);
		Formula form = mock(Formula.class);

		when(state.toString()).thenReturn(node);
		when(form.toString()).thenReturn(formula);
		when(result.getState()).thenReturn(state);
		when(result.getFormula()).thenReturn(form);

		return result;
	}

	@Test
	public void testNoCallsOutput() {
		GraphvizProgressCallback callback = new GraphvizProgressCallback();
		assertThat(callback, hasToString("digraph img {\n}\n"));
	}

	@Test
	public void testOneFailingCallOutput() {
		GraphvizProgressCallback callback = new GraphvizProgressCallback();

		callback.children(createTableau("state", "formula"), null);

		assertThat(callback, hasToString("digraph img {\ns0[label=\"state, formula\"];\ns0 -> fail;\n}\n"));
	}

	@Test
	public void testTwoFailingCallOutput() {
		GraphvizProgressCallback callback = new GraphvizProgressCallback();

		callback.children(createTableau("state", "formula"), null);
		callback.children(createTableau("state2", "formula2"), null);

		assertThat(callback, hasToString("digraph img {\ns0[label=\"state, formula\"];\ns0 -> fail;\n"
				+ "s1[label=\"state2, formula2\"];\ns1 -> fail;\n}\n"));
	}

	@Test
	public void testSuccess() {
		GraphvizProgressCallback callback = new GraphvizProgressCallback();

		callback.children(createTableau("state", "formula"), singleton(Collections.<TableauNode<State>>emptySet()));

		assertThat(callback, hasToString("digraph img {\ns0[label=\"state, formula\"];\n}\n"));
	}

	@Test
	public void testOneEdge() {
		TableauNode<State> node0 = createTableau("state0", "formula0");
		TableauNode<State> node1 = createTableau("state1", "formula1");

		GraphvizProgressCallback callback = new GraphvizProgressCallback();
		callback.children(node0, singleton(singleton(node1)));

		assertThat(callback, hasToString("digraph img {\ns0[label=\"state0, formula0\"];\n"
				+ "s1[label=\"state1, formula1\"];\ns0 -> s1;\n}\n"));
	}

	@Test
	public void testMapTableau() {
		TableauNode<State> node = createTableau("state", "formula");
		Tableau<State> tableau = mock(Tableau.class);
		when(tableau.getLeaves()).thenReturn(singleton(node));
		when(tableau.isSuccessful()).thenReturn(false);

		GraphvizProgressCallback callback = new GraphvizProgressCallback();
		callback.tableau(tableau);

		assertThat(callback, hasToString("digraph img {\ns0[label=\"state, formula\"];\nt0fail -> s0;\n}\n"));
	}

	@Test
	public void testMapTableaus() {
		TableauNode<State> node = createTableau("state", "formula");
		Tableau<State> tableau0 = mock(Tableau.class);
		when(tableau0.getLeaves()).thenReturn(singleton(node));
		when(tableau0.isSuccessful()).thenReturn(false);
		Tableau<State> tableau1 = mock(Tableau.class);
		when(tableau1.getLeaves()).thenReturn(singleton(node));
		when(tableau1.isSuccessful()).thenReturn(true);

		GraphvizProgressCallback callback = new GraphvizProgressCallback();
		callback.tableau(tableau0);
		callback.tableau(tableau1);

		assertThat(callback, hasToString("digraph img {\ns0[label=\"state, formula\"];\n"
					+ "t0fail -> s0;\nt1success -> s0;\n}\n"));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
