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
import static java.util.Arrays.asList;

import org.apache.commons.collections4.Transformer;

import uniol.apt.adt.ts.State;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static uniol.synthesis.tableau.TableauMatchers.*;

public class TableauTest {
	@Test
	public void testEmpty() {
		Tableau t = new Tableau(Collections.<TableauNode>emptySet());
		assertThat(t.getLeaves(), empty());
	}

	@Test
	public void testNotEmpty() {
		TableauNode n1 = mock(TableauNode.class);
		TableauNode n2 = mock(TableauNode.class);
		Tableau t = new Tableau(asList(n1, n2));
		assertThat(t.getLeaves(), containsInAnyOrder(n1, n2));
	}

	@Test
	public void testSuccessfulEmpty() {
		Tableau t = new Tableau(Collections.<TableauNode>emptySet());
		assertThat(t, isSuccessfulTableau(true));
	}

	@Test
	public void testSuccessfulOneSuccess() {
		TableauNode n1 = mock(TableauNode.class);
		when(n1.isSuccessful()).thenReturn(true);
		Tableau t = new Tableau(asList(n1));
		assertThat(t, isSuccessfulTableau(true));
	}

	@Test
	public void testSuccessfulOneFails() {
		TableauNode n1 = mock(TableauNode.class);
		when(n1.isSuccessful()).thenReturn(false);
		Tableau t = new Tableau(asList(n1));
		assertThat(t, isSuccessfulTableau(false));
	}

	@Test
	public void testSuccessfulOneOutOfThreeFails() {
		TableauNode n1 = mock(TableauNode.class);
		TableauNode n2 = mock(TableauNode.class);
		TableauNode n3 = mock(TableauNode.class);
		when(n1.isSuccessful()).thenReturn(true);
		when(n2.isSuccessful()).thenReturn(false);
		when(n3.isSuccessful()).thenReturn(true);
		Tableau t = new Tableau(asList(n1, n2, n3));
		assertThat(t, isSuccessfulTableau(false));
	}

	@Test
	public void testTransform() {
		TableauNode n1 = mock(TableauNode.class);
		TableauNode n2 = mock(TableauNode.class);
		TableauNode n3 = mock(TableauNode.class);
		TableauNode mapped1 = mock(TableauNode.class);
		TableauNode mapped2 = mock(TableauNode.class);
		TableauNode mapped3 = mock(TableauNode.class);
		@SuppressWarnings("unchecked")
		Transformer<State, State> transformer = mock(Transformer.class);

		when(n1.transform(transformer)).thenReturn(mapped1);
		when(n2.transform(transformer)).thenReturn(mapped2);
		when(n3.transform(transformer)).thenReturn(mapped3);

		Tableau t = new Tableau(asList(n1, n2, n3));

		assertThat(t.transform(transformer).getLeaves(), containsInAnyOrder(mapped1, mapped2, mapped3));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
