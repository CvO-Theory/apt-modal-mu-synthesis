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

public class RealiseFormula {
	interface ReachingWordTransformerFactory {
		Transformer<State, State> create(TransitionSystem target);
	}

	static class DefaultReachingWordTransformerFactory implements ReachingWordTransformerFactory {
		@Override
		public Transformer<State, State> create(TransitionSystem target) {
			return new ReachingWordTransformer(target);
		}
	}

	interface ContinueTableauFactory {
		Set<Tableau<State>> continueTableau(Tableau<State> tableau);
	}

	static class DefaultContinueTableauFactory implements ContinueTableauFactory {
		@Override
		public Set<Tableau<State>> continueTableau(Tableau<State> tableau) {
			final Set<Tableau<State>> result = new HashSet<>();
			TableauBuilder.ResultCallback<State> cb = new TableauBuilder.ResultCallback<State>() {
				@Override
				public void foundTableau(NonRecursive engine, Tableau<State> tableau) {
					result.add(tableau);
				}
			};
			NonRecursive engine = new NonRecursive();
			new TableauBuilder<State>(new StateFollowArcs()).continueTableau(engine, cb, tableau,
					TableauBuilder.TableauSelection.ALL);
			engine.run();
			return result;
		}
	}

	interface OverapproximateTS {
		TransitionSystem overapproximate(TransitionSystem ts);
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

	public interface RealisationCallback {
		void foundRealisation(TransitionSystem ts, Tableau<State> tableau);
	}

	private final RealisationCallback realisationCallback;
	private final MissingArcsFinder<State> missingArcsFinder;
	private final ReachingWordTransformerFactory reachingWordTransformerFactory;
	private final ContinueTableauFactory continueTableauFactory;
	private final OverapproximateTS overapproximateTS;
	private final int maxStepsWithoutApproximations;

	public RealiseFormula(PNProperties properties, RealisationCallback realisationCallback) {
		this(properties, realisationCallback, properties.isKBounded() ? 2 * properties.getKForKBounded() : 2);
	}

	public RealiseFormula(PNProperties properties, RealisationCallback realisationCallback,
			int maxStepsWithoutApproximations) {
		this(realisationCallback, new MissingArcsFinder<State>(), new DefaultReachingWordTransformerFactory(),
				new DefaultContinueTableauFactory(), new DefaultOverapproximateTS(properties),
				maxStepsWithoutApproximations);
	}

	RealiseFormula(RealisationCallback realisationCallback, MissingArcsFinder<State> missingArcsFinder,
			ReachingWordTransformerFactory reachingWordTransformerFactory,
			ContinueTableauFactory continueTableauFactory, OverapproximateTS overapproximateTS,
			int maxStepsWithoutApproximations) {
		assert maxStepsWithoutApproximations > 0;
		this.realisationCallback = realisationCallback;
		this.missingArcsFinder = missingArcsFinder;
		this.reachingWordTransformerFactory = reachingWordTransformerFactory;
		this.continueTableauFactory = continueTableauFactory;
		this.overapproximateTS = overapproximateTS;
		this.maxStepsWithoutApproximations = maxStepsWithoutApproximations;
	}

	static class Worker implements NonRecursive.Walker {
		private final RealiseFormula rf;
		private final TransitionSystem ts;
		private final Tableau<State> tableau;

		public Worker(RealiseFormula rf, TransitionSystem ts, Tableau<State> tableau) {
			this.rf = rf;
			this.ts = ts;
			this.tableau = tableau;
		}

		TransitionSystem getTsForTest() {
			return ts;
		}

		@Override
		public void walk(NonRecursive engine) {
			// Overapproximate the current ts and transform the tableau to the overapproximated ts
			TransitionSystem overapproxTS = rf.overapproximateTS.overapproximate(ts);
			Tableau<State> transformedTableau =
				tableau.transform(rf.reachingWordTransformerFactory.create(overapproxTS));

			// For each possible way to continue the tableau, expand the ts
			for (Tableau<State> currentTableau :
					rf.continueTableauFactory.continueTableau(transformedTableau)) {
				if (currentTableau.isSuccessful()) {
					rf.realisationCallback.foundRealisation(overapproxTS, currentTableau);
					continue;
				}

				// The ts that is expanded according to the tableau; since following iterations still
				// need overapproxTS we cannot modify that directly. However, if no arcs are missing at
				// all, we do not want to copy it at all, so this is done lazily.
				TransitionSystem currentTs = null;

				// The set of tableaus that were produced as extensions of currentTableau for currentTs
				Set<Tableau<State>> nextTableaus = null;

				// Do up to maxStepsWithoutApproximations iterations...
				for (int i = 0; i < rf.maxStepsWithoutApproximations; i++) {
					// ...extending the TS with missing arcs...
					Set<Pair<State, String>> missing =
						rf.missingArcsFinder.findMissing(currentTableau);
					if (missing.isEmpty()) {
						debug("Aborting extension in iteration ", i,
								" since there were no missing arcs");
						break;
					}
					// Make sure that we do not modify the original overapproximated TS
					if (currentTs == null)
						currentTs = new TransitionSystem(overapproxTS);
					for (Pair<State, String> missingArc : missing) {
						currentTs.createArc(missingArc.getFirst(), currentTs.createState(),
								missingArc.getSecond());
					}

					// ...and then continuing the tableau for the extended ts.
					nextTableaus = rf.continueTableauFactory.continueTableau(currentTableau);
					if (nextTableaus.size() == 1) {
						currentTableau = nextTableaus.iterator().next();
						nextTableaus = null;
					} else {
						// But if we hit a disjunction (= multiple new tableaus are created),
						// stop the iteration.
						debug("Aborting extension in iteration ", i, " due to a disjunction");
						break;
					}
				}

				if (currentTs == null)
					// The above loop did not create a copy; we can just use the TS without copying
					currentTs = overapproxTS;

				if (nextTableaus == null)
					// The above loop aborted because it found no missing arcs or hit the iteration
					// limit; continue with its last tableau
					nextTableaus = Collections.singleton(currentTableau);

				// Create child instances for continuing where needed
				for (Tableau<State> newTableau : nextTableaus)
					engine.enqueue(new Worker(rf, currentTs, newTableau));
			}
		}
	}

	static private TransitionSystem getEmptyTS() {
		TransitionSystem ts = new TransitionSystem();
		ts.setInitialState(ts.createState());
		return ts;
	}

	public void realise(Formula formula) {
		TransitionSystem ts = getEmptyTS();
		formula = positiveForm(unLet(formula));

		Tableau<State> tableau = new Tableau<State>(Collections.singleton(new TableauNode<State>(
						new StateFollowArcs(), ts.getInitialState(), formula)));
		new NonRecursive().run(new Worker(this, ts, tableau));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
