package uniol.synthesis.tableau;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import static org.hamcrest.Matchers.*;

import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.apt.adt.ts.State;

public class TableauMatchers {
	static public Matcher<TableauNode> isSuccessful(final boolean expected) {
		return new FeatureMatcher<TableauNode, Boolean>(equalTo(expected), "isSuccessful", "isSuccessful") {
			@Override
			protected Boolean featureValueOf(TableauNode node) {
				return node.isSuccessful();
			}
		};
	}

	static public Matcher<TableauNode> hasState(final Matcher<State> stateMatcher) {
		return new FeatureMatcher<TableauNode, State>(stateMatcher, "getState", "getState") {
			@Override
			protected State featureValueOf(TableauNode node) {
				return node.getState();
			}
		};
	}

	static public Matcher<TableauNode> hasFormula(final Matcher<Formula> formulaMatcher) {
		return new FeatureMatcher<TableauNode, Formula>(formulaMatcher, "getFormula", "getFormula") {
			@Override
			protected Formula featureValueOf(TableauNode node) {
				return node.getFormula();
			}
		};
	}
}
