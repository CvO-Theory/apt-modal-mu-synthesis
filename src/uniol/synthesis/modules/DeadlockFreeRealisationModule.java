/*-
 * APT - Analysis of Petri Nets and labeled Transition systems
 * Copyright (C) 2018  Uli Schlachter
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

package uniol.synthesis.modules;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

import uniol.apt.adt.ts.State;
import uniol.apt.adt.ts.TransitionSystem;
import uniol.apt.analysis.synthesize.AbstractSynthesizeModule;
import uniol.apt.analysis.synthesize.PNProperties;
import uniol.apt.io.renderer.impl.AptLTSRenderer;
import uniol.apt.module.AbstractModule;
import uniol.apt.module.AptModule;
import uniol.apt.module.Category;
import uniol.apt.module.Module;
import uniol.apt.module.ModuleInput;
import uniol.apt.module.ModuleInputSpec;
import uniol.apt.module.ModuleOutput;
import uniol.apt.module.ModuleOutputSpec;
import uniol.apt.module.exception.ModuleException;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.expansion.ReachingWordTransformer;
import uniol.synthesis.expansion.RealiseFormula;
import uniol.synthesis.tableau.StateFollowArcs;
import uniol.synthesis.tableau.Tableau;
import uniol.synthesis.util.NonRecursive;

import static uniol.synthesis.util.AlphabetFinder.getAlphabet;
import static uniol.synthesis.util.PositiveFormFormulaTransformer.positiveForm;
import static uniol.synthesis.util.UnLetTransformer.unLet;

/**
 * This module is similar to {@link RealisationModule}, but enforces that realisations do not have deadlock states,
 * which are states without outgoing arcs. The module does so by realising the formula similar to {@link
 * RealisationModule}. When a realisation with deadlocks is found, one deadlock state is picked and all possibilities to
 * continue in it are generated. The result is then further realised.
 *
 * Compared to the {@link RealisationModule} and a formula that forbids deadlocks, this approach has some advantages.
 *
 * 1. State space explosion is avoided since deadlocks are eliminated one after another instead of all together. Since
 *    many possibilities to eliminate the deadlock are likely not allowed by the formula, this means that some of the
 *    branches are not generated. In {@link RealisationModule}, all deadlock states are eliminated at the same step, so
 *    an exponential number of possibilities are generated, even though most of them are inconsistent with the formula.
 *
 * 2. If the minimal over-approximation generates a new state that is not a deadlock, a formula-based approach would
 *    still try to eliminate the non-existing deadlock. This means that a formula like <code>Global(&lt;a&gt;true || ...
 *    &lt;z&gt;true)</code> would still generate all 26 possibilities to continue in this state, even though the
 *    over-approximation already made it clear that one of them is required.
 */
@AptModule
public class DeadlockFreeRealisationModule extends AbstractModule implements Module {
	@Override
	public String getShortDescription() {
		return "Computed deadlock-free Petri net realisations of a formula";
	}

	@Override
	public String getName() {
		return "deadlock_free_realise_pn";
	}

	@Override
	public void require(ModuleInputSpec inputSpec) {
		inputSpec.addParameter("options", String.class, "Comma separated list of options");
		inputSpec.addParameter("formula", Formula.class, "The formula that should be checked");
	}

	@Override
	public void provide(ModuleOutputSpec outputSpec) {
		outputSpec.addReturnValue("result", DelayedWork.class,
				ModuleOutputSpec.PROPERTY_FILE, ModuleOutputSpec.PROPERTY_RAW);
	}

	@Override
	public void run(ModuleInput input, ModuleOutput output) throws ModuleException {
		String options = input.getParameter("options", String.class);
		final PNProperties properties = AbstractSynthesizeModule.Options.parseProperties(options).properties;
		final Formula formula = input.getParameter("formula", Formula.class);

		if (!properties.isKBounded())
			System.err.println(
					"Warning: Without requiring k-boundedness this algorithm might not terminate!");

		DelayedWork result = new DelayedWork() {
			@Override
			public void generateOutput(final Writer writer) throws IOException, ModuleException {
				try {
					NonRecursive engine = new NonRecursive();
					findDeadlockFreeRealisations(engine, properties,
							new RealiseFormula.RealisationCallback() {
						private boolean first = true;

						@Override
						public void foundRealisation(TransitionSystem ts,
								Tableau<State> tableau) {
							try {
								if (!first)
									writer.write("\n\n===========\n\n");
								first = false;

								new AptLTSRenderer().render(ts, writer);
								writer.flush();
							} catch (IOException | ModuleException e) {
								throw new RuntimeException(e);
							}
						}
					}, formula);
					engine.run();
				} catch (RuntimeException e) {
					Throwable cause = e.getCause();
					if (cause instanceof IOException)
						throw (IOException) cause;
					if (cause instanceof ModuleException)
						throw (ModuleException) cause;
					throw e;
				}
			}
		};

		output.setReturnValue("result", DelayedWork.class, result);
	}

	@Override
	public Category[] getCategories() {
		return new Category[] { Category.MISC };
	}

	static public void findDeadlockFreeRealisations(NonRecursive engine, PNProperties properties,
			RealiseFormula.RealisationCallback callback, Formula formula) {
		TransitionSystem ts = getEmptyTS();
		formula = positiveForm(unLet(formula));
		Tableau<State> tableau = Tableau.<State>createInitialTableau(
				new StateFollowArcs(), ts.getInitialState(), formula);
		engine.enqueue(new Worker(properties, callback, ts, tableau, getAlphabet(formula)));
	}

	static private final class Worker implements NonRecursive.Walker {
		final private PNProperties properties;
		final private RealiseFormula.RealisationCallback callback;
		final private TransitionSystem ts;
		final private Tableau<State> tableau;
		final private Collection<String> alphabet;

		private Worker(PNProperties properties, RealiseFormula.RealisationCallback callback,
				TransitionSystem ts, Tableau<State> tableau, Collection<String> alphabet) {
			this.properties = properties;
			this.callback = callback;
			this.ts = ts;
			this.tableau = tableau;
			this.alphabet = alphabet;
		}

		@Override
		public void walk(final NonRecursive engine) {
			new RealiseFormula(properties, new RealiseFormula.RealisationCallback() {
				@Override
				public void foundRealisation(TransitionSystem realisationTs,
						Tableau<State> realisationTableau) {
					State deadlockState = findDeadlockState(realisationTs);
					if (deadlockState == null) {
						// No deadlock, so this is a deadlock-free realisation
						callback.foundRealisation(realisationTs, realisationTableau);
						return;
					}

					// Found a deadlock. Add all possibilities for continuing here to the engine.
					for (String label : alphabet) {
						// ...which means we add a new outgoing arc with label "label" to
						// deadlockState:
						TransitionSystem newTs = new TransitionSystem(realisationTs);
						State source = newTs.getNode(deadlockState.getId());
						State target = newTs.createState();
						newTs.createArc(source, target, label);

						Tableau<State> newTableau = realisationTableau.transform(
								new ReachingWordTransformer(newTs));

						engine.enqueue(new Worker(properties, callback, newTs, newTableau,
									alphabet));
					}
				}
			}).realise(ts, tableau);
		}
	}

	static private State findDeadlockState(TransitionSystem ts) {
		for (State state : ts.getNodes())
			if (state.getPostsetEdges().isEmpty())
				return state;
		return null;
	}

	static private TransitionSystem getEmptyTS() {
		TransitionSystem ts = new TransitionSystem();
		ts.setInitialState(ts.createState());
		return ts;
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
