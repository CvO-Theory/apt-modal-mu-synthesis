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

import static uniol.synthesis.util.CleanFormFormulaTransformer.cleanForm;
import static uniol.synthesis.util.PositiveFormFormulaTransformer.positiveForm;

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
			return new TableauBuilder<State>(new StateFollowArcs()).continueTableau(tableau);
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

	static private TransitionSystem getEmptyTS() {
		TransitionSystem ts = new TransitionSystem();
		ts.setInitialState(ts.createState());
		return ts;
	}

	public RealiseFormula(PNProperties properties, Formula formula, RealisationCallback realisationCallback) {
		// Create an empty TS, the default implementations for factories and forward to next constructor
		this(getEmptyTS(), cleanForm(positiveForm(formula)), realisationCallback, new MissingArcsFinder<State>(),
				new DefaultReachingWordTransformerFactory(), new DefaultContinueTableauFactory(),
				new DefaultOverapproximateTS(properties));
	}

	private RealiseFormula(TransitionSystem ts, Formula formula, RealisationCallback realisationCallback,
			MissingArcsFinder<State> missingArcsFinder, ReachingWordTransformerFactory
			reachingWordTransformerFactory, ContinueTableauFactory continueTableauFactory,
			OverapproximateTS overapproximateTS) {
		// Create a tableau that relates the initial state to the whole formula and call next constructor
		this(ts, new Tableau<State>(Collections.singleton(new TableauNode<State>(new StateFollowArcs(),
							ts.getInitialState(), formula))), realisationCallback,
				missingArcsFinder, reachingWordTransformerFactory, continueTableauFactory,
				overapproximateTS);
	}

	RealiseFormula(TransitionSystem ts, Tableau<State> tableau, RealisationCallback realisationCallback,
			MissingArcsFinder<State> missingArcsFinder, ReachingWordTransformerFactory
			reachingWordTransformerFactory, ContinueTableauFactory continueTableauFactory,
			OverapproximateTS overapproximateTS) {
		this.ts = ts;
		this.tableau = tableau;
		this.realisationCallback = realisationCallback;
		this.missingArcsFinder = missingArcsFinder;
		this.reachingWordTransformerFactory = reachingWordTransformerFactory;
		this.continueTableauFactory = continueTableauFactory;
		this.overapproximateTS = overapproximateTS;
	}

	private RealiseFormula(RealiseFormula parent, TransitionSystem ts, Tableau<State> tableau) {
		this(ts, tableau, parent.realisationCallback, parent.missingArcsFinder,
				parent.reachingWordTransformerFactory, parent.continueTableauFactory,
				parent.overapproximateTS);
	}

	@Override
	public void walk(NonRecursive engine) {
		if (tableau.isSuccessful()) {
			realisationCallback.foundRealisation(ts, tableau);
			return;
		}
		TransitionSystem newTS = new TransitionSystem(ts);
		for (Pair<State, String> missingArc : missingArcsFinder.findMissing(tableau)) {
			newTS.createArc(missingArc.getFirst(), newTS.createState(), missingArc.getSecond());
		}

		TransitionSystem overapproxTS = overapproximateTS.overapproximate(newTS);
		Tableau<State> transformedTableau = tableau.transform(reachingWordTransformerFactory.create(overapproxTS));
		for (Tableau<State> newTableau : continueTableauFactory.continueTableau(transformedTableau))
			engine.enqueue(new RealiseFormula(this, overapproxTS, newTableau));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
