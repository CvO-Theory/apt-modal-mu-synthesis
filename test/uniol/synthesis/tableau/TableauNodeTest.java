package uniol.synthesis.tableau;

import uniol.apt.adt.ts.State;
import uniol.apt.adt.ts.TransitionSystem;

import uniol.synthesis.adt.mu_calculus.Event;
import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
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

		TableauNode node = new TableauNode(state, formula);
		assertThat(node, hasState(sameInstance(state)));
		assertThat(node, hasFormula(sameInstance(formula)));
	}

	@Test
	public void testEquals() {
		FormulaCreator creator = new FormulaCreator();
		State state = getABCState();

		TableauNode node1 = new TableauNode(state, creator.constant(true));
		assertThat(node1, equalTo(node1));
		assertThat(node1, not(equalTo((Object) "foo")));
		assertThat(new TableauNode(state, creator.constant(true)), equalTo(node1));
		assertThat(new TableauNode(state, creator.constant(true)).hashCode(), equalTo(node1.hashCode()));

		assertThat(new TableauNode(state, creator.constant(false)), not(equalTo(node1)));

		state = getABCState();
		assertThat(new TableauNode(state, creator.constant(true)), not(equalTo(node1)));

		State afterA = state.getPostsetNodesByLabel("a").iterator().next();
		FixedPointFormula fp = creator.fixedPoint(FixedPoint.LEAST, creator.variable("x"), creator.constant(false));
		TableauNode node2 = node1.addExpansion(creator.variable("x"), fp).createChild(state, fp);
		TableauNode node3 = node1.createChild(afterA, fp).addExpansion(creator.variable("x"), fp).createChild(state, fp);
		assertThat(node2, not(equalTo(node3)));
	}

	@Test
	public void testEqualsExpansion() {
		FormulaCreator creator = new FormulaCreator();
		State state = getABCState();
		State afterA = state.getPostsetNodesByLabel("a").iterator().next();
		VariableFormula x = creator.variable("x");
		FixedPointFormula fp = creator.fixedPoint(FixedPoint.GREATEST, x, x);

		TableauNode node1 = new TableauNode(state, x);
		assertThat(node1.addExpansion(x, fp), not(equalTo(node1)));
	}

	@Test
	public void testSuccessful() {
		FormulaCreator creator = new FormulaCreator();
		State state = getABCState();
		VariableFormula someFormula = creator.variable("X");

		assertThat(new TableauNode(state, creator.constant(false)), isSuccessful(false));
		assertThat(new TableauNode(state, creator.constant(true)), isSuccessful(true));
		assertThat(new TableauNode(state, creator.negate(someFormula)), isSuccessful(false));
		assertThat(new TableauNode(state, creator.conjunction(someFormula, someFormula)), isSuccessful(false));
		assertThat(new TableauNode(state, creator.disjunction(someFormula, someFormula)), isSuccessful(false));
		assertThat(new TableauNode(state, creator.variable("X")), isSuccessful(false));
		assertThat(new TableauNode(state, creator.fixedPoint(FixedPoint.LEAST, someFormula, someFormula)), isSuccessful(false));

		assertThat(new TableauNode(state, creator.modality(Modality.EXISTENTIAL, new Event("z"), someFormula)), isSuccessful(false));
		assertThat(new TableauNode(state, creator.modality(Modality.UNIVERSAL,   new Event("z"), someFormula)), isSuccessful(true));

		assertThat(new TableauNode(state, creator.modality(Modality.EXISTENTIAL, new Event("a"), someFormula)), isSuccessful(false));
		assertThat(new TableauNode(state, creator.modality(Modality.UNIVERSAL,   new Event("a"), someFormula)), isSuccessful(false));
	}

	@Test
	public void testCreateChild() {
		FormulaCreator creator = new FormulaCreator();
		State state1 = getABCState();
		State state2 = getABCState();
		Formula formula1 = creator.constant(true);
		Formula formula2 = creator.constant(true);

		TableauNode node1 = new TableauNode(state1, formula1);
		TableauNode node2 = node1.createChild(state2, formula2);
		assertThat(node2, hasState(sameInstance(state2)));
		assertThat(node2, hasFormula(sameInstance(formula2)));

		TableauNode node3 = node1.createChild(formula2);
		assertThat(node3, hasState(sameInstance(state1)));
		assertThat(node3, hasFormula(sameInstance(formula2)));
	}

	@Test
	public void testExpandFixedPoint() {
		FormulaCreator creator = new FormulaCreator();
		State state = getABCState();
		VariableFormula x = creator.variable("X");
		VariableFormula fresh = creator.freshVariable("X");
		FixedPointFormula formula = creator.fixedPoint(FixedPoint.GREATEST, x, creator.constant(true));

		TableauNode node = new TableauNode(state, formula);
		assertThat(node.getDefinition(fresh), nullValue());
		assertThat(node.getDefinition(x), nullValue());
		assertThat(node.wasAlreadyExpanded(), is(false));

		TableauNode next = node.addExpansion(fresh, formula);
		assertThat(next, not(equalTo(node)));
		assertThat(next, hasState(equalTo(state)));
		assertThat(next, hasFormula(equalTo((Formula) fresh)));
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

		TableauNode node = new TableauNode(state, formula);
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

		TableauNode node0 = new TableauNode(state, x);
		assertThat(node0.wasAlreadyExpanded(), is(false));

		TableauNode node1 = node0.addExpansion(x, fp).createChild(x);
		assertThat(node1.wasAlreadyExpanded(), is(true));

		TableauNode node2 = node1.createChild(afterA, x);
		assertThat(node2.wasAlreadyExpanded(), is(false));

		TableauNode node3 = node2.recordExpansion(x, fp).createChild(state, x);
		assertThat(node3.wasAlreadyExpanded(), is(true));

		TableauNode node4 = node3.createChild(state, x);
		assertThat(node4.wasAlreadyExpanded(), is(true));
	}
}
