package uniol.synthesis.tableau;

import uniol.apt.adt.ts.State;
import uniol.apt.adt.ts.TransitionSystem;

import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

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
		assertThat(node.getState(), sameInstance(state));
		assertThat(node.getFormula(), sameInstance(formula));
	}
}
