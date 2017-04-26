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
		assertThat(expandedNode.getDefinition((VariableFormula) expandedNode.getFormula()), equalTo(formula));
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
		TableauNode node = new TableauNode(state, formula).addExpansion(formula, fixedpoint)
			.recordExpansion(formula, formula);

		assertThat(TableauBuilder.expandNode(node), nullValue());
	}

	@Test
	public void testDoubleExpansionGreatest() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		VariableFormula formula = creator.variable("X");
		FixedPointFormula fixedpoint = creator.fixedPoint(FixedPoint.GREATEST, formula, formula);
		TableauNode node = new TableauNode(state, formula).addExpansion(formula, fixedpoint)
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

		assertThat(new TableauBuilder().createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(new TableauNode(state, formula))))));
	}

	@Test
	public void testConjuncton1() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);
		Formula formula = creator.conjunction(True, False);

		assertThat(new TableauBuilder().createTableaus(state, formula), hasNSuccessfulTableaus(0));
	}

	@Test
	public void testConjuncton2() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula formula = creator.conjunction(True, True);

		assertThat(new TableauBuilder().createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(new TableauNode(state, True))))));
	}

	@Test
	public void testConjuncton3() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula right = creator.modality(Modality.UNIVERSAL, new Event("z"), True);
		Formula formula = creator.conjunction(True, right);
		assertThat(new TableauBuilder().createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(containsInAnyOrder(new TableauNode(state, True),
								new TableauNode(state, right))))));
	}

	@Test
	public void testDisjunction0() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula False = creator.constant(false);
		Formula formula = creator.disjunction(False, False);

		assertThat(new TableauBuilder().createTableaus(state, formula), hasNSuccessfulTableaus(0));
	}

	@Test
	public void testDisjunction1() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);
		Formula formula = creator.disjunction(True, False);

		assertThat(new TableauBuilder().createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(new TableauNode(state, True))))));
	}

	@Test
	public void testDisjunction2() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula formula = creator.disjunction(True, True);

		assertThat(new TableauBuilder().createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(new TableauNode(state, True))))));
	}

	@Test
	public void testDisjunction3() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula right = creator.modality(Modality.UNIVERSAL, new Event("z"), True);
		Formula formula = creator.disjunction(True, right);
		assertThat(new TableauBuilder().createTableaus(state, formula), containsInAnyOrder(
					both(isSuccessfulTableau(true)).and(hasLeaves(contains(
								new TableauNode(state, True)))),
					both(isSuccessfulTableau(true)).and(hasLeaves(contains(
								new TableauNode(state, right))))));
	}

	@Test
	public void testFixedPointFormula1() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.fixedPoint(FixedPoint.GREATEST, creator.variable("X"), creator.constant(true));

		assertThat(new TableauBuilder().createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(both(hasState(state))
								.and(hasFormula(creator.constant(true))))))));
	}

	@Test
	public void testFixedPointFormula2() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.fixedPoint(FixedPoint.LEAST, creator.variable("X"), creator.constant(true));

		assertThat(new TableauBuilder().createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(both(hasState(state))
								.and(hasFormula(creator.constant(true))))))));
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
				creator.modality(Modality.EXISTENTIAL, new Event("a"), x));

		assertThat(new TableauBuilder().createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(both(hasState(state))
								.and(hasFormula(creator.constant(true))))))));
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
				creator.modality(Modality.EXISTENTIAL, new Event("a"), x));

		assertThat(new TableauBuilder().createTableaus(state, formula), hasNSuccessfulTableaus(0));
	}

	@Test
	public void testModality1() {
		State state = getABCState();
		State afterA = state.getPostsetNodesByLabel("a").iterator().next();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.modality(Modality.EXISTENTIAL, new Event("a"), creator.constant(true));

		assertThat(new TableauBuilder().createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(new TableauNode(afterA, creator.constant(true)))))));
	}

	@Test
	public void testModality2() {
		State state = getABCState();
		State afterA = state.getPostsetNodesByLabel("a").iterator().next();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.modality(Modality.UNIVERSAL, new Event("a"), creator.constant(true));

		assertThat(new TableauBuilder().createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(new TableauNode(afterA, creator.constant(true)))))));
	}

	@Test
	public void testModality3() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.modality(Modality.EXISTENTIAL, new Event("z"), creator.constant(true));

		assertThat(new TableauBuilder().createTableaus(state, formula), contains(both(isSuccessfulTableau(false))
					.and(hasLeaves(contains(new TableauNode(state, formula))))));
	}

	@Test
	public void testModality4() {
		State state = getABCState();
		FormulaCreator creator = new FormulaCreator();
		Formula formula = creator.modality(Modality.UNIVERSAL, new Event("z"), creator.constant(true));

		assertThat(new TableauBuilder().createTableaus(state, formula), contains(both(isSuccessfulTableau(true))
					.and(hasLeaves(contains(new TableauNode(state, formula))))));
	}
}
