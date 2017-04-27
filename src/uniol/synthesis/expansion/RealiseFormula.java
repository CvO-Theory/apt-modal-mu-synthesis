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
import uniol.synthesis.tableau.Tableau;
import uniol.synthesis.tableau.TableauBuilder;
import uniol.synthesis.tableau.TableauNode;
import uniol.synthesis.util.NonRecursive;

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
		public Set<Tableau> continueTableau(Tableau tableau);
	}

	static class DefaultContinueTableauFactory implements ContinueTableauFactory {
		@Override
		public Set<Tableau> continueTableau(Tableau tableau) {
			return new TableauBuilder().continueTableau(tableau);
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
		public void foundRealisation(TransitionSystem ts, Tableau tableau);
	}

	private final TransitionSystem ts;
	private final Tableau tableau;
	private final RealisationCallback realisationCallback;
	private final MissingArcsFinder missingArcsFinder;
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
		this(getEmptyTS(), formula, realisationCallback, new MissingArcsFinder(),
				new DefaultReachingWordTransformerFactory(), new DefaultContinueTableauFactory(),
				new DefaultOverapproximateTS(properties));
	}

	private RealiseFormula(TransitionSystem ts, Formula formula, RealisationCallback realisationCallback,
			MissingArcsFinder missingArcsFinder, ReachingWordTransformerFactory
			reachingWordTransformerFactory, ContinueTableauFactory continueTableauFactory,
			OverapproximateTS overapproximateTS) {
		// Create a tableau that relates the initial state to the whole formula and call next constructor
		this(ts, new Tableau(Collections.singleton(new TableauNode(ts.getInitialState(), formula))),
				realisationCallback, missingArcsFinder, reachingWordTransformerFactory,
				continueTableauFactory, overapproximateTS);
	}

	RealiseFormula(TransitionSystem ts, Tableau tableau, RealisationCallback realisationCallback,
			MissingArcsFinder missingArcsFinder, ReachingWordTransformerFactory
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

	private RealiseFormula(RealiseFormula parent, TransitionSystem ts, Tableau tableau) {
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
		Tableau transformedTableau = tableau.transform(reachingWordTransformerFactory.create(overapproxTS));
		for (Tableau newTableau : continueTableauFactory.continueTableau(transformedTableau))
			engine.enqueue(new RealiseFormula(this, overapproxTS, newTableau));
	}
}
