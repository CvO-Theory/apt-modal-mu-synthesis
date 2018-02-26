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
import java.util.Set;

import uniol.apt.adt.ts.State;
import uniol.apt.adt.ts.TransitionSystem;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.LetFormula;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
import uniol.synthesis.util.NonRecursive;
import static uniol.synthesis.tableau.TableauMatchers.*;

@SuppressWarnings("unchecked")
public class TableauBuilderTest {
	private State getABCState() {
		TransitionSystem ts = new TransitionSystem();
		ts.createStates("s0", "s1", "s2", "s3");
		ts.setInitialState("s0");
		ts.createArc("s0", "s1", "a");
		ts.createArc("s1", "s2", "b");
		ts.createArc("s2", "s3", "c");
		return ts.getNode("s0");
	}

	private TableauBuilder.ResultCallback<State> nopResultCallback() {
		return new TableauBuilder.ResultCallback<State>() {
			@Override
			public void foundTableau(Tableau<State> tableau) {
			}
		};
	}

	private Collection<Tableau<State>> createTableaus(State state, Formula formula) {
		return createTableaus(state, formula, TableauBuilder.TableauSelection.ALL);
	}

	private Collection<Tableau<State>> createTableaus(State state, Formula formula,
			TableauBuilder.TableauSelection selection) {
		final Collection<Tableau<State>> result = new ArrayList<>();
		TableauBuilder.ResultCallback<State> cb = new TableauBuilder.ResultCallback<State>() {
			@Override
			public void foundTableau(Tableau<State> tableau) {
				result.add(tableau);
			}
		};
		NonRecursive engine = new NonRecursive();
		new TableauBuilder<State>(new StateFollowArcs()).createTableaus(engine, cb, state, formula, selection);
		engine.run();
		return result;
	}

	private Collection<Tableau<State>> continueTableau(Tableau<State> tableau) {
		final Collection<Tableau<State>> result = new ArrayList<>();
		TableauBuilder.ResultCallback<State> cb = new TableauBuilder.ResultCallback<State>() {
			@Override
			public void foundTableau(Tableau<State> tableau) {
				result.add(tableau);
			}
		};
		NonRecursive engine = new NonRecursive();
		new TableauBuilder<State>(new StateFollowArcs()).continueTableau(engine, cb, tableau,
				TableauBuilder.TableauSelection.ALL);
		engine.run();
		return result;
	}

	@Test
	public void testFalseNode() {
		State state = getABCState();
		Formula formula = new FormulaCreator().constant(false);
		TableauNode<State> node = new TableauNode<State>(null, state, formula);

		assertThat(TableauBuilder.expandNode(node), nullValue());
	}

	@Test
	public void testTrueNode() {
		State state = getABCState();
		Formula formula = new FormulaCreator().constant(true);
		TableauNode<State> node = new TableauNode<State>(null, state, formula);

		assertThat(TableauBuilder.expandNode(node), contains(empty()));
	}

	@Test
	public void testConjunctionNode() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula x = creator.variable("X");
		Formula y = creator.variable("Y");
		Formula formula = creator.conjunction(x, y);
		TableauNode<State> node = new TableauNode<State>(null, state, formula);

		assertThat(TableauBuilder.expandNode(node),
				contains(containsInAnyOrder(hasFormula(x), hasFormula(y))));
	}

	@Test
	public void testDisjunctionNode() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula x = creator.variable("X");
		Formula y = creator.variable("Y");
		Formula formula = creator.disjunction(x, y);
		TableauNode<State> node = new TableauNode<State>(null, state, formula);

		assertThat(TableauBuilder.expandNode(node),
				containsInAnyOrder(contains(hasFormula(x)), contains(hasFormula(y))));
	}

	@Test
	public void testFixedPointNode() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.fixedPoint(FixedPoint.GREATEST, creator.variable("x"), creator.constant(true));
		TableauNode<State> node = new TableauNode<State>(null, state, formula);

		Collection<? extends Collection<TableauNode<State>>> expanded = TableauBuilder.expandNode(node);
		assertThat(expanded, contains(contains(hasFormula(instanceOf(VariableFormula.class)))));
		TableauNode<State> expandedNode = expanded.iterator().next().iterator().next();
		assertThat(expandedNode.getDefinition((VariableFormula) expandedNode.getFormula()), equalTo(formula));
	}

	@Test
	public void testLetNode() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("var");
		VariableFormula expansion = creator.variable("exp");
		Formula formula = creator.let(var, expansion, creator.modality(Modality.EXISTENTIAL, "a", var));
		Formula expected = creator.modality(Modality.EXISTENTIAL, "a", expansion);
		TableauNode<State> node = new TableauNode<State>(null, state, formula);

		Collection<? extends Collection<TableauNode<State>>> expanded = TableauBuilder.expandNode(node);
		assertThat(expanded, contains(contains(hasFormula(expected))));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testNegationNode() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.negate(creator.constant(true));
		TableauNode<State> node = new TableauNode<State>(null, state, formula);

		TableauBuilder.expandNode(node);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testFreeVariable() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.variable("X");
		TableauNode<State> node = new TableauNode<State>(null, state, formula);

		TableauBuilder.expandNode(node);
	}

	@Test
	public void testBoundVariable() {
		State state = getABCState();
		State afterA = state.getPostsetNodesByLabel("a").iterator().next();
		FormulaCreator creator = new FormulaCreator();
		VariableFormula formula = creator.variable("X");
		FixedPointFormula fixedpoint = creator.fixedPoint(FixedPoint.LEAST, formula, formula);
		TableauNode<State> node = new TableauNode<State>(null, state, formula).addExpansion(formula, fixedpoint).createChild(afterA, formula);

		assertThat(TableauBuilder.expandNode(node), contains(contains(
						both(hasFormula(instanceOf(VariableFormula.class))).and(hasState(afterA)))));
	}

	@Test
	public void testBoundVariableFresh() {
		State state = getABCState();
		State afterA = state.getPostsetNodesByLabel("a").iterator().next();
		FormulaCreator creator = new FormulaCreator();
		VariableFormula formula = creator.variable("X");
		VariableFormula fresh = creator.freshVariable("X");
		FixedPointFormula fixedpoint = creator.fixedPoint(FixedPoint.LEAST, formula, creator.modality(Modality.EXISTENTIAL, "a", formula));
		Formula innerFresh = creator.modality(Modality.EXISTENTIAL, "a", fresh);
		TableauNode<State> node = new TableauNode<State>(null, state, formula).addExpansion(fresh, fixedpoint).createChild(afterA, fresh);

		assertThat(TableauBuilder.expandNode(node), contains(contains(hasFormula(innerFresh))));
	}

	@Test
	public void testDoubleExpansionLeast() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		VariableFormula formula = creator.variable("X");
		FixedPointFormula fixedpoint = creator.fixedPoint(FixedPoint.LEAST, formula, formula);
		TableauNode<State> node = new TableauNode<State>(null, state, formula).addExpansion(formula, fixedpoint)
			.recordExpansion(formula, formula);

		assertThat(TableauBuilder.expandNode(node), nullValue());
	}

	@Test
	public void testDoubleExpansionGreatest() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		VariableFormula formula = creator.variable("X");
		FixedPointFormula fixedpoint = creator.fixedPoint(FixedPoint.GREATEST, formula, formula);
		TableauNode<State> node = new TableauNode<State>(null, state, formula).addExpansion(formula, fixedpoint)
			.recordExpansion(formula, formula);

		assertThat(TableauBuilder.expandNode(node), contains(contains(hasFormula(creator.constant(true)))));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testModalityNonDeterministic() {
		TransitionSystem ts = new TransitionSystem();
		ts.createStates("s0", "s1", "s2");
		ts.setInitialState("s0");
		ts.createArc("s0", "s1", "a");
		ts.createArc("s0", "s2", "a");

		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.modality(Modality.UNIVERSAL, "a", creator.constant(true));
		TableauNode<State> node = new TableauNode<State>(new StateFollowArcs(), ts.getNode("s0"), formula);

		TableauBuilder.expandNode(node);
	}

	@Test
	public void testModalityUniversalExisting() {
		State state = getABCState();
		State afterA = state.getPostsetNodesByLabel("a").iterator().next();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.modality(Modality.UNIVERSAL, "a", creator.constant(true));
		TableauNode<State> node = new TableauNode<State>(new StateFollowArcs(), state, formula);

		assertThat(TableauBuilder.expandNode(node), contains(contains(
						hasStateAndFormula(afterA, creator.constant(true)))));
	}

	@Test
	public void testModalityUniversalNonExisting() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.modality(Modality.UNIVERSAL, "z", creator.constant(true));
		TableauNode<State> node = new TableauNode<State>(new StateFollowArcs(), state, formula);

		assertThat(TableauBuilder.expandNode(node), contains(empty()));
	}

	@Test
	public void testModalityExistentialExisting() {
		State state = getABCState();
		State afterA = state.getPostsetNodesByLabel("a").iterator().next();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.modality(Modality.EXISTENTIAL, "a", creator.constant(true));
		TableauNode<State> node = new TableauNode<State>(new StateFollowArcs(), state, formula);

		assertThat(TableauBuilder.expandNode(node), contains(contains(
						hasStateAndFormula(afterA, creator.constant(true)))));
	}

	@Test
	public void testModalityExistentialNonExisting() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.modality(Modality.EXISTENTIAL, "z", creator.constant(true));
		TableauNode<State> node = new TableauNode<State>(new StateFollowArcs(), state, formula);

		assertThat(TableauBuilder.expandNode(node), contains(empty()));
	}

	@Test
	public void testFalse() {
		State state = getABCState();
		Formula formula = new FormulaCreator().constant(false);

		assertThat(createTableaus(state, formula), hasNSuccessfulTableaus(0));
	}

	@Test
	public void testTrue() {
		State state = getABCState();
		Formula formula = new FormulaCreator().constant(true);

		assertThat(createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(new TableauNode<State>(null, state, formula))))));
	}

	@Test
	public void testNegation() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.negate(creator.constant(false));

		assertThat(createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(new TableauNode<State>(null, state, creator.constant(true)))))));
	}

	@Test
	public void testConjuncton1() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);
		Formula formula = creator.conjunction(True, False);

		assertThat(createTableaus(state, formula), hasNSuccessfulTableaus(0));
	}

	@Test
	public void testConjuncton2() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula formula = creator.conjunction(True, True);

		TableauNode<State> leave = new TableauNode<State>(null, state, True);
		assertThat(createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(leave, leave)))));
	}

	@Test
	public void testConjuncton3() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula right = creator.modality(Modality.UNIVERSAL, "z", True);
		Formula formula = creator.conjunction(True, right);
		assertThat(createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(containsInAnyOrder(new TableauNode<State>(null, state, True),
								new TableauNode<State>(null, state, right))))));
	}

	@Test
	public void testDisjunction0() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula False = creator.constant(false);
		Formula formula = creator.disjunction(False, False);

		assertThat(createTableaus(state, formula), hasNSuccessfulTableaus(0));
	}

	@Test
	public void testDisjunction1() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);
		Formula formula = creator.disjunction(True, False);

		assertThat(createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(new TableauNode<State>(null, state, True))))));
	}

	@Test
	public void testDisjunction2() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula formula = creator.disjunction(True, True);

		assertThat(createTableaus(state, formula), containsInAnyOrder(
					both(isSuccessfulTableau(true)).and(hasLeaves(contains(
								new TableauNode<State>(null, state, True)))),
					both(isSuccessfulTableau(true)).and(hasLeaves(contains(
								new TableauNode<State>(null, state, True))))));
	}

	@Test
	public void testDisjunction3() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula right = creator.modality(Modality.UNIVERSAL, "z", True);
		Formula formula = creator.disjunction(True, right);
		assertThat(createTableaus(state, formula), containsInAnyOrder(
					both(isSuccessfulTableau(true)).and(hasLeaves(contains(
								new TableauNode<State>(null, state, True)))),
					both(isSuccessfulTableau(true)).and(hasLeaves(contains(
								new TableauNode<State>(null, state, right))))));
	}

	@Test
	public void testFixedPointFormula1() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.fixedPoint(FixedPoint.GREATEST, creator.variable("X"), creator.constant(true));

		assertThat(createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(hasStateAndFormula(state, creator.constant(true)))))));
	}

	@Test
	public void testFixedPointFormula2() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, creator.variable("X"), creator.constant(true));

		assertThat(createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(hasStateAndFormula(state, creator.constant(true)))))));
	}

	@Test
	public void testFixedPointFormula3() {
		TransitionSystem ts = new TransitionSystem();
		ts.createStates("s0");
		ts.setInitialState("s0");
		ts.createArc("s0", "s0", "a");
		State state = ts.getNode("s0");

		FormulaCreator creator = new FormulaCreator();
		VariableFormula x = creator.variable("X");
		Formula formula = creator.fixedPoint(FixedPoint.GREATEST, x,
				creator.modality(Modality.EXISTENTIAL, "a", x));

		assertThat(createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(hasStateAndFormula(state, creator.constant(true)))))));
	}

	@Test
	public void testFixedPointFormula4() {
		TransitionSystem ts = new TransitionSystem();
		ts.createStates("s0");
		ts.setInitialState("s0");
		ts.createArc("s0", "s0", "a");
		State state = ts.getNode("s0");

		FormulaCreator creator = new FormulaCreator();
		VariableFormula x = creator.variable("X");
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, x,
				creator.modality(Modality.EXISTENTIAL, "a", x));

		assertThat(createTableaus(state, formula), hasNSuccessfulTableaus(0));
	}

	@Test
	public void testModality1() {
		State state = getABCState();
		State afterA = state.getPostsetNodesByLabel("a").iterator().next();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.modality(Modality.EXISTENTIAL, "a", creator.constant(true));

		assertThat(createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(new TableauNode<State>(null, afterA, creator.constant(true)))))));
	}

	@Test
	public void testModality2() {
		State state = getABCState();
		State afterA = state.getPostsetNodesByLabel("a").iterator().next();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.modality(Modality.UNIVERSAL, "a", creator.constant(true));

		assertThat(createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(new TableauNode<State>(null, afterA, creator.constant(true)))))));
	}

	@Test
	public void testModality3() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.modality(Modality.EXISTENTIAL, "z", creator.constant(true));

		assertThat(createTableaus(state, formula), contains(both(isSuccessfulTableau(false))
					.and(hasLeaves(contains(new TableauNode<State>(null, state, formula))))));
	}

	@Test
	public void testModality4() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.modality(Modality.UNIVERSAL, "z", creator.constant(true));

		assertThat(createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(new TableauNode<State>(null, state, formula))))));
	}

	@Test
	public void testLet() {
		State state = getABCState();
		State afterA = state.getPostsetNodesByLabel("a").iterator().next();
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		VariableFormula var = creator.variable("var");
		Formula formula = creator.let(var, True,
				creator.modality(Modality.EXISTENTIAL, "a", var));

		assertThat(createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(new TableauNode<State>(null, afterA, True))))));
	}

	@Test
	public void testContinueTableau() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula right = creator.modality(Modality.UNIVERSAL, "z", True);
		Formula formula = creator.conjunction(True, right);
		TableauNode<State> node = new TableauNode<State>(new StateFollowArcs(), state, formula);
		Tableau<State> tableau = new Tableau<State>(Collections.singleton(node));

		assertThat(continueTableau(tableau), contains(hasLeaves(containsInAnyOrder(
						hasStateAndFormula(state, True), hasStateAndFormula(state, right)))));
	}

	private State getExampleFromPaper() {
		// See Stirling, Walker, "Local model checking in the modal mu-calculus", Theoretical Computer Science
		// 89 (1991) 161-177, Elsevier. The examples from page 168f were slightly modified.
		TransitionSystem ts = new TransitionSystem();
		ts.createStates("s", "t", "u");
		ts.setInitialState("s");
		ts.createArc("s", "t", "a");
		ts.createArc("t", "s", "a");
		ts.createArc("t", "u", "b");
		return ts.getNode("s");
	}

	@Test
	public void testExampleFromPaper1() {
		State s = getExampleFromPaper();
		FormulaCreator creator = new FormulaCreator();
		Formula f = creator.fixedPoint(FixedPoint.GREATEST, creator.variable("Z"),
				creator.fixedPoint(FixedPoint.LEAST, creator.variable("Y"),
					creator.modality(Modality.UNIVERSAL, "a", creator.disjunction(
							creator.conjunction(
								creator.modality(Modality.EXISTENTIAL, "b",
									creator.constant(true)),
								creator.variable("Z")), creator.variable("Y")))));

		// There are a total of five tableaus according to the definition of the paper, one of which is
		// successful. However, this implementation 'optimises' and does not generate tableaus which fail
		// because a least fixed point recursed to itself. That leaves one successful and two failing tableaus.

		assertThat(createTableaus(s, f), containsInAnyOrder(isSuccessfulTableau(true),
					isSuccessfulTableau(false), isSuccessfulTableau(false)));
	}

	@Test
	public void testExampleFromPaper2() {
		State s = getExampleFromPaper();
		FormulaCreator creator = new FormulaCreator();
		Formula f = creator.fixedPoint(FixedPoint.LEAST, creator.variable("Y"),
				creator.fixedPoint(FixedPoint.GREATEST, creator.variable("Z"),
					creator.modality(Modality.UNIVERSAL, "a", creator.conjunction(
							creator.disjunction(
								creator.modality(Modality.EXISTENTIAL, "b",
									creator.constant(true)),
								creator.variable("Y")), creator.variable("Z")))));

		assertThat(createTableaus(s, f), containsInAnyOrder(
					isSuccessfulTableau(false), isSuccessfulTableau(false)));
	}

	@Test
	public void testABCWord() {
		State s0 = getABCState();
		State s1 = s0.getPostsetNodesByLabel("a").iterator().next();
		State s2 = s1.getPostsetNodesByLabel("b").iterator().next();
		State s3 = s2.getPostsetNodesByLabel("c").iterator().next();

		FormulaCreator creator = new FormulaCreator();
		Formula inner = creator.modality(Modality.UNIVERSAL, "z", creator.constant(false));
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, creator.variable("X"),
				creator.disjunction(creator.modality(Modality.EXISTENTIAL, "a", creator.variable("X")),
					creator.disjunction(creator.modality(Modality.EXISTENTIAL, "b", creator.variable("X")),
						creator.disjunction(creator.modality(Modality.EXISTENTIAL, "c", creator.variable("X")),
							inner))));

		assertThat(createTableaus(s0, formula), containsInAnyOrder(
					isSuccessfulTableau(false), isSuccessfulTableau(false),
					isSuccessfulTableau(false), isSuccessfulTableau(false),
					isSuccessfulTableau(false), isSuccessfulTableau(false),
					isSuccessfulTableau(false), isSuccessfulTableau(false),
					isSuccessfulTableau(false),
					both(isSuccessfulTableau(true)).and(hasLeaves(contains(
								hasStateAndFormula(s0, inner)))),
					both(isSuccessfulTableau(true)).and(hasLeaves(contains(
								hasStateAndFormula(s1, inner)))),
					both(isSuccessfulTableau(true)).and(hasLeaves(contains(
								hasStateAndFormula(s2, inner)))),
					both(isSuccessfulTableau(true)).and(hasLeaves(contains(
								hasStateAndFormula(s3, inner))))));
	}

	@Test
	public void testABCWordOnlySuccessful() {
		State s0 = getABCState();
		State s1 = s0.getPostsetNodesByLabel("a").iterator().next();
		State s2 = s1.getPostsetNodesByLabel("b").iterator().next();
		State s3 = s2.getPostsetNodesByLabel("c").iterator().next();

		FormulaCreator creator = new FormulaCreator();
		Formula inner = creator.modality(Modality.UNIVERSAL, "z", creator.constant(false));
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, creator.variable("X"),
				creator.disjunction(creator.modality(Modality.EXISTENTIAL, "a", creator.variable("X")),
					creator.disjunction(creator.modality(Modality.EXISTENTIAL, "b", creator.variable("X")),
						creator.disjunction(creator.modality(Modality.EXISTENTIAL, "c", creator.variable("X")),
							inner))));

		assertThat(createTableaus(s0, formula, TableauBuilder.TableauSelection.SUCCESSFUL), containsInAnyOrder(
					both(isSuccessfulTableau(true)).and(hasLeaves(contains(
								hasStateAndFormula(s0, inner)))),
					both(isSuccessfulTableau(true)).and(hasLeaves(contains(
								hasStateAndFormula(s1, inner)))),
					both(isSuccessfulTableau(true)).and(hasLeaves(contains(
								hasStateAndFormula(s2, inner)))),
					both(isSuccessfulTableau(true)).and(hasLeaves(contains(
								hasStateAndFormula(s3, inner))))));
	}

	@Test
	public void testProgressCallback1() {
		final State s0 = getABCState();
		final State s1 = s0.getPostsetNodesByLabel("a").iterator().next();
		final State s2 = s1.getPostsetNodesByLabel("b").iterator().next();
		final State s3 = s2.getPostsetNodesByLabel("c").iterator().next();

		FormulaCreator creator = new FormulaCreator();
		final Formula formula3 = creator.modality(Modality.UNIVERSAL, "z", creator.constant(false));
		final Formula formula2 = creator.modality(Modality.EXISTENTIAL, "c", formula3);
		final Formula formula1 = creator.modality(Modality.EXISTENTIAL, "b", formula2);
		final Formula formula0 = creator.modality(Modality.EXISTENTIAL, "a", formula1);

		final int callCount[] = new int[1];
		TableauBuilder.ProgressCallback<State> callback = new TableauBuilder.ProgressCallback<State>() {
			@Override
			public void children(TableauNode<State> node, Collection<? extends Collection<TableauNode<State>>> children) {
				switch (callCount[0]++) {
					case 0:
						assertThat(node, hasStateAndFormula(s0, formula0));
						assertThat(children, contains(contains(hasStateAndFormula(s1, formula1))));
						break;
					case 1:
						assertThat(node, hasStateAndFormula(s1, formula1));
						assertThat(children, contains(contains(hasStateAndFormula(s2, formula2))));
						break;
					case 2:
						assertThat(node, hasStateAndFormula(s2, formula2));
						assertThat(children, contains(contains(hasStateAndFormula(s3, formula3))));
						break;
					case 3:
						assertThat(node, hasStateAndFormula(s3, formula3));
						assertThat(children, contains(empty()));
						break;
					default:
						throw new AssertionError("Too many calls to callback");
				}
			}
		};
		NonRecursive engine = new NonRecursive();
		new TableauBuilder<State>(new StateFollowArcs(), callback).createTableaus(engine, nopResultCallback(),
				s0, formula0, TableauBuilder.TableauSelection.ALL);
		engine.run();

		assertThat(callCount[0], equalTo(4));
	}

	@Test
	public void testProgressCallback2() {
		final State s = getABCState();
		FormulaCreator creator = new FormulaCreator();
		final Formula formula = creator.constant(false);

		final int callCount[] = new int[1];
		TableauBuilder.ProgressCallback<State> callback = new TableauBuilder.ProgressCallback<State>() {
			@Override
			public void children(TableauNode<State> node, Collection<? extends Collection<TableauNode<State>>> children) {
				switch (callCount[0]++) {
					case 0:
						assertThat(node, hasStateAndFormula(s, formula));
						assertThat(children, nullValue());
						break;
					default:
						throw new AssertionError("Too many calls to callback");
				}
			}
		};
		NonRecursive engine = new NonRecursive();
		new TableauBuilder<State>(null, callback).createTableaus(engine, nopResultCallback(), s, formula,
				TableauBuilder.TableauSelection.ALL);
		engine.run();

		assertThat(callCount[0], equalTo(1));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
