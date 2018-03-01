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
import static java.util.Arrays.asList;

import org.apache.commons.collections4.Transformer;

import uniol.apt.adt.ts.State;
import uniol.synthesis.adt.mu_calculus.Formula;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static uniol.synthesis.tableau.TableauMatchers.*;

@SuppressWarnings("unchecked")
public class TableauTest {
	static private final Map<State, Set<Formula>> EMPTY_MAP = Collections.emptyMap();

	@Test
	public void testEmpty() {
		Tableau<State> t = new Tableau<State>(Collections.<TableauNode<State>>emptySet(), EMPTY_MAP);
		assertThat(t.getLeaves(), empty());
	}

	@Test
	public void testNotEmpty() {
		TableauNode<State> n1 = mock(TableauNode.class);
		TableauNode<State> n2 = mock(TableauNode.class);
		Tableau<State> t = new Tableau<State>(asList(n1, n2), EMPTY_MAP);
		assertThat(t.getLeaves(), containsInAnyOrder(n1, n2));
	}

	@Test
	public void testSuccessfulEmpty() {
		Tableau<State> t = new Tableau<State>(Collections.<TableauNode<State>>emptySet(), EMPTY_MAP);
		assertThat(t, isSuccessfulTableau(true));
	}

	@Test
	public void testSuccessfulOneSuccess() {
		TableauNode<State> n1 = mock(TableauNode.class);
		when(n1.isSuccessful()).thenReturn(true);
		Tableau<State> t = new Tableau<State>(asList(n1), EMPTY_MAP);
		assertThat(t, isSuccessfulTableau(true));
	}

	@Test
	public void testSuccessfulOneFails() {
		TableauNode<State> n1 = mock(TableauNode.class);
		when(n1.isSuccessful()).thenReturn(false);
		Tableau<State> t = new Tableau<State>(asList(n1), EMPTY_MAP);
		assertThat(t, isSuccessfulTableau(false));
	}

	@Test
	public void testSuccessfulOneOutOfThreeFails() {
		TableauNode<State> n1 = mock(TableauNode.class);
		TableauNode<State> n2 = mock(TableauNode.class);
		TableauNode<State> n3 = mock(TableauNode.class);
		when(n1.isSuccessful()).thenReturn(true);
		when(n2.isSuccessful()).thenReturn(false);
		when(n3.isSuccessful()).thenReturn(true);
		Tableau<State> t = new Tableau<State>(asList(n1, n2, n3), EMPTY_MAP);
		assertThat(t, isSuccessfulTableau(false));
	}

	@Test
	public void testTransform() {
		TableauNode<State> n1 = mock(TableauNode.class);
		TableauNode<State> n2 = mock(TableauNode.class);
		TableauNode<State> n3 = mock(TableauNode.class);
		TableauNode<State> mapped1 = mock(TableauNode.class);
		TableauNode<State> mapped2 = mock(TableauNode.class);
		TableauNode<State> mapped3 = mock(TableauNode.class);
		Transformer<State, State> transformer = mock(Transformer.class);

		when(n1.transform(transformer)).thenReturn(mapped1);
		when(n2.transform(transformer)).thenReturn(mapped2);
		when(n3.transform(transformer)).thenReturn(mapped3);

		Tableau<State> t = new Tableau<State>(asList(n1, n2, n3), EMPTY_MAP);

		assertThat(t.transform(transformer).getLeaves(), containsInAnyOrder(mapped1, mapped2, mapped3));
	}

	@Test
	public void testAlreadyHandledNull() {
		State state = mock(State.class);
		Formula formula = mock(Formula.class);
		Tableau<State> t = new Tableau<State>(Collections.<TableauNode<State>>emptySet(), EMPTY_MAP);
		assertThat(t.alreadyHandled(state, formula), is(false));
	}

	@Test
	public void testAlreadyHandledNotFound() {
		State state = mock(State.class);
		Formula formula = mock(Formula.class);
		Map<State, Set<Formula>> map = new HashMap<>();
		map.put(state, Collections.<Formula>emptySet());

		Tableau<State> t = new Tableau<State>(Collections.<TableauNode<State>>emptySet(), map);
		assertThat(t.alreadyHandled(state, formula), is(false));
	}

	@Test
	public void testAlreadyHandledFound() {
		State state = mock(State.class);
		Formula formula = mock(Formula.class);
		Map<State, Set<Formula>> map = new HashMap<>();
		map.put(state, new HashSet<>(asList(formula)));

		Tableau<State> t = new Tableau<State>(Collections.<TableauNode<State>>emptySet(), map);
		assertThat(t.alreadyHandled(state, formula), is(true));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
