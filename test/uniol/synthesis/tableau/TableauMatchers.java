package uniol.synthesis.tableau;

import java.util.HashSet;
import java.util.Set;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Description;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import static org.hamcrest.Matchers.*;

import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.apt.adt.ts.State;

public class TableauMatchers {
	static public Matcher<TableauNode> isSuccessfulNode(final boolean expected) {
		return new FeatureMatcher<TableauNode, Boolean>(equalTo(expected), "isSuccessful", "isSuccessful") {
			@Override
			protected Boolean featureValueOf(TableauNode node) {
				return node.isSuccessful();
			}
		};
	}

	static public Matcher<TableauNode> hasState(final Matcher<? super State> stateMatcher) {
		return new FeatureMatcher<TableauNode, State>(stateMatcher, "getState", "getState") {
			@Override
			protected State featureValueOf(TableauNode node) {
				return node.getState();
			}
		};
	}

	static public Matcher<TableauNode> hasState(State state) {
		return hasState(equalTo(state));
	}

	static public Matcher<TableauNode> hasFormula(final Matcher<? super Formula> formulaMatcher) {
		return new FeatureMatcher<TableauNode, Formula>(formulaMatcher, "getFormula", "getFormula") {
			@Override
			protected Formula featureValueOf(TableauNode node) {
				return node.getFormula();
			}
		};
	}

	static public Matcher<TableauNode> hasFormula(Formula formula) {
		return hasFormula(equalTo(formula));
	}

	static public Matcher<TableauNode> hasStateAndFormula(State state, Formula formula) {
		return both(hasState(state)).and(hasFormula(formula));
	}

	static public Matcher<Tableau> hasLeaves(final Matcher<Iterable<? extends TableauNode>> nodesMatcher) {
		return new FeatureMatcher<Tableau, Set<TableauNode>>(nodesMatcher, "getLeaves", "getLeaves") {
			@Override
			protected Set<TableauNode> featureValueOf(Tableau tableau) {
				return tableau.getLeaves();
			}
		};
	}

	static public Matcher<Tableau> isSuccessfulTableau(final boolean expected) {
		return new FeatureMatcher<Tableau, Boolean>(equalTo(expected), "isSuccessful", "isSuccessful") {
			@Override
			protected Boolean featureValueOf(Tableau tableau) {
				return tableau.isSuccessful();
			}
		};
	}

	static public Matcher<Set<Tableau>> hasNSuccessfulTableaus(final int n) {
		return new TypeSafeDiagnosingMatcher<Set<Tableau>>() {
			@Override
			public void describeTo(Description description) {
				description.appendValue(n).appendText(" successful tableaus");
			}

			@Override
			protected boolean matchesSafely(Set<Tableau> tableaus, Description mismatch) {
				boolean result = true;

				if (tableaus.size() != n) {
					mismatch.appendText(" was ").appendValue(tableaus.size()).appendText(" tableaus");
					result = false;
				}

				Set<Tableau> error = new HashSet<>();
				for (Tableau tableau : tableaus) {
					for (TableauNode leave : tableau.getLeaves())
						if (!leave.isSuccessful()) {
							error.add(tableau);
							break;
						}
				}
				if (!error.isEmpty()) {
					mismatch.appendText(" not successful: ").appendValue(error);
					result = false;
				}

				return result;
			}
		};
	}
}
