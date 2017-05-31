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

import org.apache.commons.collections4.TransformerUtils;

import uniol.apt.adt.ts.State;
import uniol.apt.adt.ts.TransitionSystem;

import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static uniol.synthesis.tableau.TableauMatchers.*;

public class TableauNodeTest {
	private State getABCState() {
		TransitionSystem ts = new TransitionSystem();
		ts.createStates("s0", "s1", "s2", "s3");
		ts.setInitialState("s0");
		ts.createArc("s0", "s1", "a");
		ts.createArc("s1", "s2", "b");
		ts.createArc("s2", "s3", "c");
		return ts.getNode("s0");
	}

	@Test
	public void testConstructor() {
		FormulaCreator creator = new FormulaCreator();
		State state = getABCState();
		Formula formula = creator.constant(true);

		TableauNode node = new TableauNode(null, state, formula);
		assertThat(node, hasStateAndFormula(state, formula));
	}

	@Test
	public void testEquals() {
		FormulaCreator creator = new FormulaCreator();
		State state = getABCState();

		TableauNode node1 = new TableauNode(null, state, creator.constant(true));
		assertThat(node1, equalTo(node1));
		assertThat(node1, not(equalTo((Object) "foo")));
		assertThat(new TableauNode(null, state, creator.constant(true)), equalTo(node1));
		assertThat(new TableauNode(null, state, creator.constant(true)).hashCode(), equalTo(node1.hashCode()));

		assertThat(new TableauNode(null, state, creator.constant(false)), not(equalTo(node1)));

		state = getABCState();
		assertThat(new TableauNode(null, state, creator.constant(true)), not(equalTo(node1)));

		TableauNode node2 = node1.recordExpansion(creator.variable("X"), creator.constant(true));
		assertThat(node1, not(equalTo(node2)));
	}

	@Test
	public void testEqualsExpansion() {
		FormulaCreator creator = new FormulaCreator();
		State state = getABCState();
		VariableFormula x = creator.variable("x");
		FixedPointFormula fp = creator.fixedPoint(FixedPoint.GREATEST, x, x);

		TableauNode node1 = new TableauNode(null, state, x);
		assertThat(node1.addExpansion(x, fp), not(equalTo(node1)));
	}

	@Test
	public void testSuccessful() {
		FormulaCreator creator = new FormulaCreator();
		State state = getABCState();
		VariableFormula someFormula = creator.variable("X");

		assertThat(new TableauNode(null, state, creator.constant(false)), isSuccessfulNode(false));
		assertThat(new TableauNode(null, state, creator.constant(true)), isSuccessfulNode(true));
		assertThat(new TableauNode(null, state, creator.negate(someFormula)), isSuccessfulNode(false));
		assertThat(new TableauNode(null, state, creator.conjunction(someFormula, someFormula)), isSuccessfulNode(false));
		assertThat(new TableauNode(null, state, creator.disjunction(someFormula, someFormula)), isSuccessfulNode(false));
		assertThat(new TableauNode(null, state, creator.variable("X")), isSuccessfulNode(false));
		assertThat(new TableauNode(null, state, creator.fixedPoint(FixedPoint.LEAST, someFormula, someFormula)), isSuccessfulNode(false));

		@SuppressWarnings("unchecked")
		FollowArcs<State> followArcsZ = mock(FollowArcs.class);

		when(followArcsZ.followArcs(state, "z")).thenReturn(Collections.<State>emptySet());
		assertThat(new TableauNode(null, state, creator.modality(Modality.EXISTENTIAL, "z", someFormula)), isSuccessfulNode(false));
		assertThat(new TableauNode(followArcsZ, state, creator.modality(Modality.UNIVERSAL,   "z", someFormula)), isSuccessfulNode(true));

		@SuppressWarnings("unchecked")
		FollowArcs<State> followArcsA = mock(FollowArcs.class);
		when(followArcsA.followArcs(state, "a")).thenReturn(Collections.<State>singleton(state));

		assertThat(new TableauNode(null, state, creator.modality(Modality.EXISTENTIAL, "a", someFormula)), isSuccessfulNode(false));
		assertThat(new TableauNode(followArcsA, state, creator.modality(Modality.UNIVERSAL,   "a", someFormula)), isSuccessfulNode(false));
	}

	@Test
	public void testCreateChild() {
		FormulaCreator creator = new FormulaCreator();
		State state1 = getABCState();
		State state2 = getABCState();
		Formula formula1 = creator.constant(true);
		Formula formula2 = creator.constant(true);

		TableauNode node1 = new TableauNode(null, state1, formula1);
		TableauNode node2 = node1.createChild(state2, formula2);
		assertThat(node2, hasStateAndFormula(state2, formula2));

		TableauNode node3 = node1.createChild(formula2);
		assertThat(node3, hasStateAndFormula(state1, formula2));
	}

	@Test
	public void testExpandFixedPoint() {
		FormulaCreator creator = new FormulaCreator();
		State state = getABCState();
		VariableFormula x = creator.variable("X");
		VariableFormula fresh = creator.freshVariable("X");
		FixedPointFormula formula = creator.fixedPoint(FixedPoint.GREATEST, x, creator.constant(true));

		TableauNode node = new TableauNode(null, state, formula);
		assertThat(node.getDefinition(fresh), nullValue());
		assertThat(node.getDefinition(x), nullValue());
		assertThat(node.wasAlreadyExpanded(), is(false));

		TableauNode next = node.addExpansion(fresh, formula);
		assertThat(next, not(equalTo(node)));
		assertThat(next, hasStateAndFormula(state, fresh));
		assertThat(next.getDefinition(fresh), equalTo(formula));
		assertThat(next.getDefinition(x), nullValue());
		assertThat(next.createChild(formula).wasAlreadyExpanded(), is(false));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testDoubleExpansion() {
		FormulaCreator creator = new FormulaCreator();
		State state = getABCState();
		VariableFormula x = creator.variable("X");
		VariableFormula fresh = creator.freshVariable("X");
		FixedPointFormula formula = creator.fixedPoint(FixedPoint.GREATEST, x, creator.constant(true));
		FixedPointFormula otherFp = creator.fixedPoint(FixedPoint.LEAST, x, creator.constant(true));

		TableauNode node = new TableauNode(null, state, formula);
		Object o = node.addExpansion(fresh, formula).addExpansion(fresh, otherFp);
		throw new RuntimeException(o.toString() + "this code should not be reached");
	}

	@Test
	public void testWasAlreadyExpanded() {
		FormulaCreator creator = new FormulaCreator();
		State state = getABCState();
		State afterA = state.getPostsetNodesByLabel("a").iterator().next();
		VariableFormula x = creator.variable("X");
		FixedPointFormula fp = creator.fixedPoint(FixedPoint.LEAST, x, x);

		TableauNode node0 = new TableauNode(null, state, x);
		assertThat(node0.wasAlreadyExpanded(), is(false));

		TableauNode node1 = node0.addExpansion(x, fp).createChild(x);
		assertThat(node1.wasAlreadyExpanded(), is(false));

		TableauNode node2 = node1.recordExpansion(x, x);
		assertThat(node2.wasAlreadyExpanded(), is(true));

		TableauNode node3 = node2.createChild(afterA, x);
		assertThat(node3.wasAlreadyExpanded(), is(false));

		TableauNode node4 = node3.recordExpansion(x, fp).createChild(state, x);
		assertThat(node4.wasAlreadyExpanded(), is(true));

		TableauNode node5 = node4.createChild(state, x);
		assertThat(node5.wasAlreadyExpanded(), is(true));
	}

	@Test
	public void testTransformSimple() {
		Formula formula = new FormulaCreator().constant(true);
		State oldState = getABCState();
		State newState = getABCState();

		assertThat(oldState, not(equalTo(newState)));
		TableauNode oldNode = new TableauNode(null, oldState, formula);
		TableauNode newNode = oldNode.transform(TransformerUtils.<State, State>constantTransformer(newState));

		assertThat(newNode, hasStateAndFormula(newState, formula));
	}

	@Test
	public void testTransformWithExpansion() {
		VariableFormula formula = new FormulaCreator().variable("X");
		State oldState = getABCState();
		State newState = getABCState();

		assertThat(oldState, not(equalTo(newState)));
		TableauNode oldNode = new TableauNode(null, oldState, formula).recordExpansion(formula, formula);
		TableauNode newNode = oldNode.transform(TransformerUtils.<State, State>constantTransformer(newState));

		assertThat(newNode, hasStateAndFormula(newState, formula));
		assertThat(newNode.wasAlreadyExpanded(), is(true));
		assertThat(newNode.createChild(oldState, formula).wasAlreadyExpanded(), is(false));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
