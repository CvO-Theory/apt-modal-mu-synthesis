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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Executor;
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

	private final RealisationCallback realisationCallback;
	private final MissingArcsFinder<State> missingArcsFinder;
	private final ReachingWordTransformerFactory reachingWordTransformerFactory;
	private final ContinueTableauFactory continueTableauFactory;
	private final OverapproximateTS overapproximateTS;
	private final int maxStepsWithoutApproximations;

	private final AtomicInteger unfinishedWorkers = new AtomicInteger(0);
	final Collection<Pair<TransitionSystem, Tableau<State>>> pendingRealisations = new ArrayDeque<>();

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

	static class Worker implements Runnable {
		private final RealiseFormula rf;
		private final TransitionSystem ts;
		private final Tableau<State> tableau;
		private final Executor executor;

		public Worker(RealiseFormula rf, TransitionSystem ts, Tableau<State> tableau, Executor executor) {
			this.rf = rf;
			this.ts = ts;
			this.tableau = tableau;
			this.executor = executor;

			rf.unfinishedWorkers.incrementAndGet();
		}

		@Override
		public void run() {
			if (tableau.isSuccessful()) {
				rf.unfinishedWorkers.decrementAndGet();
				synchronized(rf.pendingRealisations) {
					rf.pendingRealisations.add(new Pair<TransitionSystem, Tableau<State>>(ts, tableau));
					rf.pendingRealisations.notify();
				}
				return;
			}

			TransitionSystem currentTs = new TransitionSystem(ts);
			Tableau<State> currentTableau = tableau.transform(rf.reachingWordTransformerFactory.create(currentTs));
			Set<Tableau<State>> nextTableaus = null;

			// Do up to maxStepsWithoutApproximations iterations...
			for (int i = 0; i < rf.maxStepsWithoutApproximations; i++) {
				// ...extending the TS with missing arcs...
				Set<Pair<State, String>> missing = rf.missingArcsFinder.findMissing(currentTableau);
				if (missing.isEmpty()) {
					debug("Aborting extension in iteration ", i, " since there were no missing arcs");
					break;
				}
				for (Pair<State, String> missingArc : missing) {
					currentTs.createArc(missingArc.getFirst(), currentTs.createState(), missingArc.getSecond());
				}

				// ...and then continuing the tableau for the extended ts.
				nextTableaus = rf.continueTableauFactory.continueTableau(currentTableau);
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
			TransitionSystem overapproxTS = rf.overapproximateTS.overapproximate(currentTs);
			Tableau<State> transformedTableau = currentTableau.transform(rf.reachingWordTransformerFactory.create(overapproxTS));
			for (Tableau<State> newTableau : rf.continueTableauFactory.continueTableau(transformedTableau))
				executor.execute(new Worker(rf, overapproxTS, newTableau, executor));

			// Signal the main thread if we were the last worker
			if (rf.unfinishedWorkers.decrementAndGet() == 0) {
				synchronized(rf.pendingRealisations) {
					rf.pendingRealisations.notify();
				}
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
		ForkJoinPool pool = new ForkJoinPool();
		try {
			pool.execute(new Worker(this, ts, tableau, pool));

			// Wait for all tasks to be done
			synchronized(pendingRealisations) {
				while (unfinishedWorkers.get() > 0) {
					for (Pair<TransitionSystem, Tableau<State>> pair : pendingRealisations)
						realisationCallback.foundRealisation(pair.getFirst(), pair.getSecond());
					pendingRealisations.clear();
					try {
						pendingRealisations.wait();
					} catch (InterruptedException e) {
						// Hm... What to do with this?
						throw new AssertionError(e);
					}
				}
			}
		} finally {
			pool.shutdownNow();
		}

		for (Pair<TransitionSystem, Tableau<State>> pair : pendingRealisations)
			realisationCallback.foundRealisation(pair.getFirst(), pair.getSecond());
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
