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
	static public Matcher<TableauNode<State>> isSuccessfulNode(final boolean expected) {
		return new FeatureMatcher<TableauNode<State>, Boolean>(equalTo(expected), "isSuccessful", "isSuccessful") {
			@Override
			protected Boolean featureValueOf(TableauNode<State> node) {
				return node.isSuccessful();
			}
		};
	}

	static public Matcher<TableauNode<State>> hasState(final Matcher<? super State> stateMatcher) {
		return new FeatureMatcher<TableauNode<State>, State>(stateMatcher, "getState", "getState") {
			@Override
			protected State featureValueOf(TableauNode<State> node) {
				return node.getState();
			}
		};
	}

	static public Matcher<TableauNode<State>> hasState(State state) {
		return hasState(equalTo(state));
	}

	static public Matcher<TableauNode<State>> hasFormula(final Matcher<? super Formula> formulaMatcher) {
		return new FeatureMatcher<TableauNode<State>, Formula>(formulaMatcher, "getFormula", "getFormula") {
			@Override
			protected Formula featureValueOf(TableauNode<State> node) {
				return node.getFormula();
			}
		};
	}

	static public Matcher<TableauNode<State>> hasFormula(Formula formula) {
		return hasFormula(equalTo(formula));
	}

	static public Matcher<TableauNode<State>> hasStateAndFormula(State state, Formula formula) {
		Matcher<TableauNode<State>> formulaMatcher = hasFormula(formula);
		return both(hasState(state)).and(formulaMatcher);
	}

	static public Matcher<Tableau<State>> hasLeaves(final Matcher<Iterable<? extends TableauNode<State>>> nodesMatcher) {
		return new FeatureMatcher<Tableau<State>, Set<TableauNode<State>>>(nodesMatcher, "getLeaves", "getLeaves") {
			@Override
			protected Set<TableauNode<State>> featureValueOf(Tableau<State> tableau) {
				return tableau.getLeaves();
			}
		};
	}

	static public Matcher<Tableau<State>> isSuccessfulTableau(final boolean expected) {
		return new FeatureMatcher<Tableau<State>, Boolean>(equalTo(expected), "isSuccessful", "isSuccessful") {
			@Override
			protected Boolean featureValueOf(Tableau<State> tableau) {
				return tableau.isSuccessful();
			}
		};
	}

	static public Matcher<Set<Tableau<State>>> hasNSuccessfulTableaus(final int n) {
		return new TypeSafeDiagnosingMatcher<Set<Tableau<State>>>() {
			@Override
			public void describeTo(Description description) {
				description.appendValue(n).appendText(" successful tableaus");
			}

			@Override
			protected boolean matchesSafely(Set<Tableau<State>> tableaus, Description mismatch) {
				boolean result = true;

				if (tableaus.size() != n) {
					mismatch.appendText(" was ").appendValue(tableaus.size()).appendText(" tableaus");
					result = false;
				}

				Set<Tableau<State>> error = new HashSet<>();
				for (Tableau<State> tableau : tableaus) {
					for (TableauNode<?> leave : tableau.getLeaves())
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

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
