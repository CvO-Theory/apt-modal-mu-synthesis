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

import static java.util.Collections.singleton;

import uniol.apt.adt.ts.State;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static uniol.apt.util.matcher.Matchers.pairWith;

import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.tableau.Tableau;
import uniol.synthesis.tableau.TableauNode;

@SuppressWarnings("unchecked")
public class MissingArcsFinderTest {
	@Test
	public void testSimpleNoMissingArcs() {
		FormulaCreator creator = new FormulaCreator();

		TableauNode<State> node = mock(TableauNode.class);
		when(node.getFormula()).thenReturn(creator.constant(true));

		Tableau<State> tableau = mock(Tableau.class);
		when(tableau.getLeaves()).thenReturn(singleton(node));

		assertThat(new MissingArcsFinder<State>().findMissing(tableau), empty());
	}

	@Test
	public void testUniversalMissingArc() {
		FormulaCreator creator = new FormulaCreator();
		State state = mock(State.class);

		TableauNode<State> node = mock(TableauNode.class);
		when(node.getState()).thenReturn(state);
		when(node.getFormula()).thenReturn(
				creator.modality(Modality.UNIVERSAL, "a", creator.constant(true)));

		Tableau<State> tableau = mock(Tableau.class);
		when(tableau.getLeaves()).thenReturn(singleton(node));

		assertThat(new MissingArcsFinder<State>().findMissing(tableau), empty());
	}

	@Test
	public void testExistentialMissingArc() {
		FormulaCreator creator = new FormulaCreator();
		State state = mock(State.class);

		TableauNode<State> node = mock(TableauNode.class);
		when(node.getState()).thenReturn(state);
		when(node.getFormula()).thenReturn(
				creator.modality(Modality.EXISTENTIAL, "a", creator.constant(true)));

		Tableau<State> tableau = mock(Tableau.class);
		when(tableau.getLeaves()).thenReturn(singleton(node));

		assertThat(new MissingArcsFinder<State>().findMissing(tableau), contains(pairWith(state, "a")));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
