package uniol.synthesis.tableau;

import uniol.apt.adt.ts.State;
import uniol.apt.adt.ts.TransitionSystem;

import uniol.synthesis.adt.mu_calculus.Event;
import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
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

	private Matcher<TableauNode> isSuccessful(final boolean expected) {
		return new FeatureMatcher<TableauNode, Boolean>(equalTo(expected), "isSuccessful", "isSuccessful") {
			@Override
			protected Boolean featureValueOf(TableauNode node) {
				return node.isSuccessful();
			}
		};
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

	@Test
	public void testEquals() {
		FormulaCreator creator = new FormulaCreator();
		State state = getABCState();

		TableauNode node1 = new TableauNode(state, creator.constant(true));
		assertThat(node1, not(equalTo((Object) "foo")));
		assertThat(new TableauNode(state, creator.constant(true)), equalTo(node1));
		assertThat(new TableauNode(state, creator.constant(true)).hashCode(), equalTo(node1.hashCode()));

		assertThat(new TableauNode(state, creator.constant(false)), not(equalTo(node1)));

		state = getABCState();
		assertThat(new TableauNode(state, creator.constant(true)), not(equalTo(node1)));
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
}
