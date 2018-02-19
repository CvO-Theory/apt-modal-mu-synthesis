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

package uniol.synthesis.expansion;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.collections4.Transformer;

import uniol.apt.adt.ts.State;
import uniol.apt.adt.ts.TransitionSystem;
import uniol.apt.analysis.coverability.CoverabilityGraph;
import uniol.apt.analysis.exception.UnboundedException;
import uniol.apt.analysis.synthesize.MissingLocationException;
import uniol.apt.analysis.synthesize.OverapproximatePN;
import uniol.apt.analysis.synthesize.PNProperties;
import uniol.apt.analysis.synthesize.separation.UnsupportedPNPropertiesException;
import uniol.apt.util.Pair;

import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.tableau.StateFollowArcs;
import uniol.synthesis.tableau.Tableau;
import uniol.synthesis.tableau.TableauBuilder;
import uniol.synthesis.tableau.TableauNode;
import uniol.synthesis.util.NonRecursive;

import static uniol.apt.util.DebugUtil.debug;
import static uniol.synthesis.util.PositiveFormFormulaTransformer.positiveForm;
import static uniol.synthesis.util.UnLetTransformer.unLet;

public class RealiseFormula implements NonRecursive.Walker {
	static interface ReachingWordTransformerFactory {
		public Transformer<State, State> create(TransitionSystem target);
	}

	static class DefaultReachingWordTransformerFactory implements ReachingWordTransformerFactory {
		@Override
		public Transformer<State, State> create(TransitionSystem target) {
			return new ReachingWordTransformer(target);
		}
	}

	static interface ContinueTableauFactory {
		public Set<Tableau<State>> continueTableau(Tableau<State> tableau);
	}

	static class DefaultContinueTableauFactory implements ContinueTableauFactory {
		@Override
		public Set<Tableau<State>> continueTableau(Tableau<State> tableau) {
			final Set<Tableau<State>> result = new HashSet<>();
			TableauBuilder.ResultCallback<State> cb = new TableauBuilder.ResultCallback<State>() {
				@Override
				public void foundTableau(Tableau<State> tableau) {
					result.add(tableau);
				}
			};
			new TableauBuilder<State>(new StateFollowArcs()).continueTableau(cb, tableau,
					TableauBuilder.TableauSelection.ALL);
			return result;
		}
	}

	static interface OverapproximateTS {
		public TransitionSystem overapproximate(TransitionSystem ts);
	}

	static class DefaultOverapproximateTS implements OverapproximateTS {
		private final PNProperties properties;

		public DefaultOverapproximateTS(PNProperties properties) {
			this.properties = properties;
		}

		@Override
		public TransitionSystem overapproximate(TransitionSystem ts) {
			try {
				return CoverabilityGraph.get(OverapproximatePN.overapproximate(ts, properties))
					.toReachabilityLTS();
			} catch (MissingLocationException | UnboundedException | UnsupportedPNPropertiesException e) {
				throw new RuntimeException(e);
			}
		}
	}

	static public interface RealisationCallback {
		public void foundRealisation(TransitionSystem ts, Tableau<State> tableau);
	}

	private final TransitionSystem ts;
	private final Tableau<State> tableau;
	private final RealisationCallback realisationCallback;
	private final MissingArcsFinder<State> missingArcsFinder;
	private final ReachingWordTransformerFactory reachingWordTransformerFactory;
	private final ContinueTableauFactory continueTableauFactory;
	private final OverapproximateTS overapproximateTS;
	private final int maxStepsWithoutApproximations;

	static private TransitionSystem getEmptyTS() {
		TransitionSystem ts = new TransitionSystem();
		ts.setInitialState(ts.createState());
		return ts;
	}

	public RealiseFormula(PNProperties properties, Formula formula, RealisationCallback realisationCallback) {
		this(properties, formula, realisationCallback,
				properties.isKBounded() ? 2 * properties.getKForKBounded() : 2);
	}

	public RealiseFormula(PNProperties properties, Formula formula, RealisationCallback realisationCallback,
			int maxStepsWithoutApproximations) {
		// Create an empty TS, the default implementations for factories and forward to next constructor
		this(getEmptyTS(), positiveForm(unLet(formula)), realisationCallback, new MissingArcsFinder<State>(),
				new DefaultReachingWordTransformerFactory(), new DefaultContinueTableauFactory(),
				new DefaultOverapproximateTS(properties), maxStepsWithoutApproximations);
	}

	private RealiseFormula(TransitionSystem ts, Formula formula, RealisationCallback realisationCallback,
			MissingArcsFinder<State> missingArcsFinder, ReachingWordTransformerFactory
			reachingWordTransformerFactory, ContinueTableauFactory continueTableauFactory,
			OverapproximateTS overapproximateTS, int maxStepsWithoutApproximations) {
		// Create a tableau that relates the initial state to the whole formula and call next constructor
		this(ts, new Tableau<State>(Collections.singleton(new TableauNode<State>(new StateFollowArcs(),
							ts.getInitialState(), formula))), realisationCallback,
				missingArcsFinder, reachingWordTransformerFactory, continueTableauFactory,
				overapproximateTS, maxStepsWithoutApproximations);
	}

	RealiseFormula(TransitionSystem ts, Tableau<State> tableau, RealisationCallback realisationCallback,
			MissingArcsFinder<State> missingArcsFinder, ReachingWordTransformerFactory
			reachingWordTransformerFactory, ContinueTableauFactory continueTableauFactory,
			OverapproximateTS overapproximateTS, int maxStepsWithoutApproximations) {
		assert maxStepsWithoutApproximations > 0;
		this.ts = ts;
		this.tableau = tableau;
		this.realisationCallback = realisationCallback;
		this.missingArcsFinder = missingArcsFinder;
		this.reachingWordTransformerFactory = reachingWordTransformerFactory;
		this.continueTableauFactory = continueTableauFactory;
		this.overapproximateTS = overapproximateTS;
		this.maxStepsWithoutApproximations = maxStepsWithoutApproximations;
	}

	private RealiseFormula(RealiseFormula parent, TransitionSystem ts, Tableau<State> tableau) {
		this(ts, tableau, parent.realisationCallback, parent.missingArcsFinder,
				parent.reachingWordTransformerFactory, parent.continueTableauFactory,
				parent.overapproximateTS, parent.maxStepsWithoutApproximations);
	}

	@Override
	public void walk(NonRecursive engine) {
		if (tableau.isSuccessful()) {
			realisationCallback.foundRealisation(ts, tableau);
			return;
		}

		TransitionSystem currentTs = new TransitionSystem(ts);
		Tableau<State> currentTableau = tableau.transform(reachingWordTransformerFactory.create(currentTs));
		Set<Tableau<State>> nextTableaus = null;

		// Do up to maxStepsWithoutApproximations iterations...
		for (int i = 0; i < maxStepsWithoutApproximations; i++) {
			// ...extending the TS with missing arcs...
			Set<Pair<State, String>> missing = missingArcsFinder.findMissing(currentTableau);
			if (missing.isEmpty()) {
				debug("Aborting extension in iteration ", i, " since there were no missing arcs");
				break;
			}
			for (Pair<State, String> missingArc : missing) {
				currentTs.createArc(missingArc.getFirst(), currentTs.createState(), missingArc.getSecond());
			}

			// ...and then continuing the tableau for the extended ts.
			nextTableaus = continueTableauFactory.continueTableau(currentTableau);
			if (nextTableaus.size() == 1) {
				currentTableau = nextTableaus.iterator().next();
			} else {
				// But if we hit a disjunction (= multiple new tableaus are created),
				// stop the iteration.
				debug("Aborting extension in iteration ", i, " due to a disjunction");
				break;
			}
		}

		// Overapproximate the current ts, transform the tableau to the overapproximated ts and create child
		// instances for continuing there.
		TransitionSystem overapproxTS = overapproximateTS.overapproximate(currentTs);
		Tableau<State> transformedTableau = currentTableau.transform(reachingWordTransformerFactory.create(overapproxTS));
		for (Tableau<State> newTableau : continueTableauFactory.continueTableau(transformedTableau))
			engine.enqueue(new RealiseFormula(this, overapproxTS, newTableau));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
