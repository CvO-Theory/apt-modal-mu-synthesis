package uniol.synthesis.experiment;

import java.util.Set;
import java.util.Deque;
import java.util.ArrayDeque;

import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.parser.FormulaParser;
import uniol.synthesis.tableau.TableauBuilder;
import uniol.synthesis.tableau.TableauPrinter;
import uniol.synthesis.tableau.Tableau;
import uniol.synthesis.tableau.MissingArcsFinder;
import uniol.apt.adt.ts.TransitionSystem;
import uniol.apt.io.parser.LTSParsers;
import uniol.apt.io.renderer.LTSRenderers;
import uniol.apt.adt.ts.State;
import uniol.apt.util.Pair;

import uniol.apt.analysis.synthesize.AbstractSynthesizeModule.Options;
import uniol.apt.analysis.synthesize.PNProperties;
import uniol.apt.analysis.coverability.CoverabilityGraph;
import uniol.apt.analysis.synthesize.OverapproximatePN;
import uniol.apt.adt.pn.PetriNet;

import static uniol.synthesis.util.PositiveFormFormulaTransformer.positiveForm;
import static uniol.synthesis.util.CleanFormFormulaTransformer.cleanForm;

public class Main3 {
	public static void main(String[] args) throws Exception {
		if (args.length != 2)
			throw new Exception("Need two arguments");

		PNProperties props = Options.parseProperties(args[0]).properties;
		if (!props.isKBounded())
			throw new Exception("Need to be k-bounded");

		FormulaCreator creator = new FormulaCreator();
		Formula formula = FormulaParser.parse(creator, args[1]);

		System.out.println("Input: " + formula);
		formula = cleanForm(positiveForm(formula));
		System.out.println("formula: " + formula);

		synthesize(props, formula);
	}

	public static void synthesize(PNProperties properties, Formula formula) throws Exception {
		Deque<TransitionSystem> todo = new ArrayDeque<>();
		TransitionSystem ts = new TransitionSystem();
		ts.setInitialState(ts.createState());
		todo.add(ts);

		while (!todo.isEmpty()) {
			ts = todo.remove();
			Tableau tableau = new TableauBuilder().getTableau(ts.getInitialState(), formula);
			if (tableau.isSuccessful())
				handleImplementation(ts);

			for (Set<Pair<State, String>> set : new MissingArcsFinder().getMissingArcs(tableau)) {
				todo.add(overapproximate(handleMissingArcs(ts, set), properties));
			}
		}
	}

	public static TransitionSystem overapproximate(TransitionSystem ts, PNProperties props) throws Exception {
		PetriNet pn = OverapproximatePN.overapproximate(ts, props);
		return CoverabilityGraph.get(pn).toReachabilityLTS();
	}

	public static TransitionSystem handleMissingArcs(TransitionSystem ts, Set<Pair<State, String>> missing) {
		ts = new TransitionSystem(ts);
		for (Pair<State, String> pair : missing) {
			State source = ts.getNode(pair.getFirst().getId());
			State target = ts.createState();
			ts.createArc(source, target, pair.getSecond());
		}
		return ts;
	}

	public static void handleImplementation(TransitionSystem ts) throws Exception {
		System.out.println(LTSRenderers.INSTANCE.getRenderer("APT").render(ts));
	}
}
