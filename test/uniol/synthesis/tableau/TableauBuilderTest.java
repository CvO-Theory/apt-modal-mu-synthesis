package uniol.synthesis.tableau;

import java.util.Set;

import uniol.apt.adt.ts.State;
import uniol.apt.adt.ts.TransitionSystem;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import uniol.synthesis.adt.mu_calculus.Event;
import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
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

	@Test
	public void testFalseNode() {
		State state = getABCState();
		Formula formula = new FormulaCreator().constant(false);
		TableauNode node = new TableauNode(state, formula);

		assertThat(TableauBuilder.expandNode(node), nullValue());
	}

	@Test
	public void testTrueNode() {
		State state = getABCState();
		Formula formula = new FormulaCreator().constant(true);
		TableauNode node = new TableauNode(state, formula);

		assertThat(TableauBuilder.expandNode(node), contains(empty()));
	}

	@Test
	public void testConjunctionNode() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula x = creator.variable("X");
		Formula y = creator.variable("Y");
		Formula formula = creator.conjunction(x, y);
		TableauNode node = new TableauNode(state, formula);

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
		TableauNode node = new TableauNode(state, formula);

		assertThat(TableauBuilder.expandNode(node),
				containsInAnyOrder(contains(hasFormula(x)), contains(hasFormula(y))));
	}

	@Test
	public void testFixedPointFormula() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.fixedPoint(FixedPoint.GREATEST, creator.variable("x"), creator.constant(true));
		TableauNode node = new TableauNode(state, formula);

		Set<Set<TableauNode>> expanded = TableauBuilder.expandNode(node);
		assertThat(expanded, contains(contains(hasFormula(instanceOf(VariableFormula.class)))));
		TableauNode expandedNode = expanded.iterator().next().iterator().next();
		assertThat(expandedNode.wasAlreadyExpanded(), is(true));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testNegationNode() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.negate(creator.constant(true));
		TableauNode node = new TableauNode(state, formula);

		TableauBuilder.expandNode(node);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testFreeVariable() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.variable("X");
		TableauNode node = new TableauNode(state, formula);

		TableauBuilder.expandNode(node);
	}

	@Test
	public void testBoundVariable() {
		State state = getABCState();
		State afterA = state.getPostsetNodesByLabel("a").iterator().next();
		FormulaCreator creator = new FormulaCreator();
		VariableFormula formula = creator.variable("X");
		FixedPointFormula fixedpoint = creator.fixedPoint(FixedPoint.LEAST, formula, formula);
		TableauNode node = new TableauNode(state, formula).addExpansion(formula, fixedpoint).createChild(afterA, formula);

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
		FixedPointFormula fixedpoint = creator.fixedPoint(FixedPoint.LEAST, formula, creator.modality(Modality.EXISTENTIAL, new Event("a"), formula));
		Formula innerFresh = creator.modality(Modality.EXISTENTIAL, new Event("a"), fresh);
		TableauNode node = new TableauNode(state, formula).addExpansion(fresh, fixedpoint).createChild(afterA, fresh);

		assertThat(TableauBuilder.expandNode(node), contains(contains(hasFormula(innerFresh))));
	}

	@Test
	public void testDoubleExpansionLeast() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		VariableFormula formula = creator.variable("X");
		FixedPointFormula fixedpoint = creator.fixedPoint(FixedPoint.LEAST, formula, formula);
		TableauNode node = new TableauNode(state, formula).addExpansion(formula, fixedpoint).createChild(formula);

		assertThat(TableauBuilder.expandNode(node), nullValue());
	}

	@Test
	public void testDoubleExpansionGreatest() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		VariableFormula formula = creator.variable("X");
		FixedPointFormula fixedpoint = creator.fixedPoint(FixedPoint.GREATEST, formula, formula);
		TableauNode node = new TableauNode(state, formula).addExpansion(formula, fixedpoint).createChild(formula);

		assertThat(TableauBuilder.expandNode(node), contains(empty()));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testModalityNonDeterministic() {
		TransitionSystem ts = new TransitionSystem();
		ts.createStates("s0", "s1", "s2");
		ts.setInitialState("s0");
		ts.createArc("s0", "s1", "a");
		ts.createArc("s0", "s2", "a");

		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.modality(Modality.UNIVERSAL, new Event("a"), creator.constant(true));
		TableauNode node = new TableauNode(ts.getNode("s0"), formula);

		TableauBuilder.expandNode(node);
	}

	@Test
	public void testModalityUniversalExisting() {
		State state = getABCState();
		State afterA = state.getPostsetNodesByLabel("a").iterator().next();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.modality(Modality.UNIVERSAL, new Event("a"), creator.constant(true));
		TableauNode node = new TableauNode(state, formula);

		assertThat(TableauBuilder.expandNode(node), contains(contains(
						both(hasFormula(creator.constant(true))).and(hasState(afterA)))));
	}

	@Test
	public void testModalityUniversalNonExisting() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.modality(Modality.UNIVERSAL, new Event("z"), creator.constant(true));
		TableauNode node = new TableauNode(state, formula);

		assertThat(TableauBuilder.expandNode(node), contains(empty()));
	}

	@Test
	public void testModalityExistentialExisting() {
		State state = getABCState();
		State afterA = state.getPostsetNodesByLabel("a").iterator().next();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.modality(Modality.EXISTENTIAL, new Event("a"), creator.constant(true));
		TableauNode node = new TableauNode(state, formula);

		assertThat(TableauBuilder.expandNode(node), contains(contains(
						both(hasFormula(creator.constant(true))).and(hasState(afterA)))));
	}

	@Test
	public void testModalityExistentialNonExisting() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.modality(Modality.EXISTENTIAL, new Event("z"), creator.constant(true));
		TableauNode node = new TableauNode(state, formula);

		assertThat(TableauBuilder.expandNode(node), contains(empty()));
	}


	/*
	@Test
	public void testFalse() {
		State state = getABCState();
		Formula formula = new FormulaCreator().constant(false);

		assertThat(new TableauBuilder().createTableaus(state, formula), hasNSuccessfulTableaus(0));
	}

	@Test
	public void testTrue() {
		State state = getABCState();
		Formula formula = new FormulaCreator().constant(true);

		assertThat(new TableauBuilder().createTableaus(state, formula), hasNSuccessfulTableaus(1));
	}
	*/
}
