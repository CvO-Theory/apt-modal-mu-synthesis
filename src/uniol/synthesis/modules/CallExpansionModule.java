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

package uniol.synthesis.modules;

import java.util.HashSet;
import java.util.Set;

import uniol.apt.module.AbstractModule;
import uniol.apt.module.AptModule;
import uniol.apt.module.Category;
import uniol.apt.module.Module;
import uniol.apt.module.ModuleInput;
import uniol.apt.module.ModuleInputSpec;
import uniol.apt.module.ModuleOutput;
import uniol.apt.module.ModuleOutputSpec;
import uniol.apt.module.exception.ModuleException;
import uniol.synthesis.adt.mu_calculus.CallFormula;
import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
import uniol.synthesis.util.AlphabetFinder;
import uniol.synthesis.util.FormulaFormulaTransformer;
import uniol.synthesis.util.NonRecursive;

import static uniol.synthesis.util.UnLetTransformer.unLet;

@AptModule
public class CallExpansionModule extends AbstractModule implements Module {
	@Override
	public String getShortDescription() {
		return "Expand some function call operators in a formula";
	}

	@Override
	public String getLongDescription() {
		FormulaCreator creator = new FormulaCreator();
		Formula addEvents = creator.conjunction(
				creator.modality(Modality.UNIVERSAL, "b", creator.constant(true)),
				creator.modality(Modality.UNIVERSAL, "c", creator.constant(true)));
		Formula exGlobal = creator.conjunction(
				creator.call("global", creator.modality(Modality.EXISTENTIAL, "a", creator.constant(true))),
				addEvents);
		Formula exEventually = creator.conjunction(
				creator.call("eventually", creator.variable("a")),
				addEvents);
		Formula exHide1 = creator.conjunction(
				creator.call("hide", creator.modality(Modality.EXISTENTIAL, "a", creator.variable("P"))),
				addEvents);
		Formula exHide2 = creator.conjunction(
				creator.call("hide", creator.modality(Modality.UNIVERSAL, "a", creator.variable("P"))),
				addEvents);
		Formula exGlobalExpanded = handleCalls(exGlobal);
		Formula exEventuallyExpanded = handleCalls(exEventually);
		Formula exHide1Expanded = handleCalls(exHide1);
		Formula exHide2Expanded = handleCalls(exHide2);
		return getShortDescription() + ". The supported functions are:\n" +
			"- global(beta): beta holds in all reachable state.\n" +
			"- eventually(e): every path is either finite or contains event e.\n" +
			"- hide(beta): hide environment actions in beta.\n" +
			"\nAll these expansion rely on a global alphabet. That is the alphabet containing all the " +
			"events which appear inside of modalities in the full formula. The following examples use the " +
			"subformula " + addEvents + ", which is equivalent to true, to add events b and c to this " +
			"alphabet.\n" +
			"\n" + exGlobal + ", which expresses that in all reachable states event a is enabled, " +
			"expands to " + exGlobalExpanded + ".\n" +
			"\n" + exEventually + ", which expresses that eventually a deadlock is reached or event a is " +
			"possible, expands to " + exEventuallyExpanded + ".\n" +
			"\nIn hide(beta), the local alphabet of events appearing in beta is computed. The meaning of " +
			"modalities is changes so that events of the environment are hidden. For existential " +
			"modalities this means that instead of 'a is possible next', the meaning is 'a is possible " +
			"after a finite sequence of external events'. For example " + exHide1 + ", which means " +
			"'after one a, P holds' is replaced with " + exHide1Expanded + ", which means that there is " +
			"a path so that after a finite sequence of b's and c's, there is an 'a' after which P " +
			"holds.\n" +
			"The hide function handles universal modalities differently: " + exHide2 + ", which expresses" +
			"that if 'a' is possible, afterwards P holds, expands to " + exHide2Expanded + " which " +
			"expresses that on all paths either the environment diverges (does an infinite sequence of" +
			"steps), a deadlock is reached, or an 'a' is possible after which P holds.";
	}

	@Override
	public String getName() {
		return "call_expansion";
	}

	@Override
	public void require(ModuleInputSpec inputSpec) {
		inputSpec.addParameter("formula", Formula.class, "The formula that should be modified");
	}

	@Override
	public void provide(ModuleOutputSpec outputSpec) {
		outputSpec.addReturnValue("formula", Formula.class);
	}

	@Override
	public void run(ModuleInput input, ModuleOutput output) throws ModuleException {
		Formula formula = input.getParameter("formula", Formula.class);
		output.setReturnValue("formula", Formula.class, handleCalls(formula));
	}

	// Get the alphabet and use CallFindingTransformer to find & transform all supported invocations
	public static Formula handleCalls(Formula formula) {
		formula = unLet(formula);
		Set<String> alphabet = AlphabetFinder.getAlphabet(formula);
		CallFindingTransformer hide = new CallFindingTransformer(alphabet);
		NonRecursive engine = new NonRecursive();
		hide.transform(engine, formula);
		engine.run();
		return hide.transform(engine, formula);
	}

	private static class CallFindingTransformer extends FormulaFormulaTransformer {
		private final Set<String> fullAlphabet;

		public CallFindingTransformer(Set<String> fullAlphabet) {
			this.fullAlphabet = fullAlphabet;
		}

		@Override
		protected void enqueueWalker(NonRecursive engine, Formula formula) {
			engine.enqueue(new Worker(formula));
		}

		class Worker extends FormulaFormulaTransformer.FillCache {
			public Worker(Formula formula) {
				super(formula);
			}

			@Override
			public void walk(NonRecursive engine, CallFormula formula) {
				// Currently, all supported calls have just one argument
				if (formula.getArguments().size() != 1)
					throw new RuntimeException("Need exactly one argument, but found: " + formula);

				// First expand the argument
				Formula argument = getCache(formula.getArguments().get(0));
				if (argument == null) {
					engine.enqueue(this);
					enqueueWalker(engine, formula.getArguments().get(0));
					return;
				}

				// Then handle the actual call
				Formula replacement;
				switch (formula.getFunction()) {
					case "hide":
						replacement = expandOneHide(argument, fullAlphabet);
						break;
					case "global":
						replacement = expandOneGlobal(argument, fullAlphabet);
						break;
					case "eventually":
						replacement = expandOneEventually(argument, fullAlphabet);
						break;
					default:
						throw new RuntimeException("Unsupported function call '" + formula.getFunction() + "'");
				}
				setCache(formula, replacement);
			}
		}
	}

	// Expand the formula inside of a global application.
	public static Formula expandOneGlobal(Formula formula, Set<String> fullAlphabet) {
		FormulaCreator creator = formula.getCreator();
		VariableFormula var = creator.freshVariable("g");
		for (String event : fullAlphabet) {
			formula = creator.conjunction(formula,
					creator.modality(Modality.UNIVERSAL, event, var));
		}
		return creator.fixedPoint(FixedPoint.GREATEST, var, formula);
	}

	public static Formula expandOneEventually(Formula formula, Set<String> fullAlphabet) {
		if (!(formula instanceof VariableFormula))
			throw new RuntimeException("Eventually needs a variable as its argument that is then "
					+ "interpreted as an event, but got: " + formula);
		FormulaCreator creator = formula.getCreator();
		VariableFormula var = creator.freshVariable("e");
		String eventually = ((VariableFormula) formula).getVariable();
		formula = creator.modality(Modality.UNIVERSAL, eventually, creator.constant(true));
		for (String event : fullAlphabet) {
			if (event.equals(eventually))
				continue;
			formula = creator.conjunction(formula,
					creator.modality(Modality.UNIVERSAL, event, var));
		}
		return creator.fixedPoint(FixedPoint.LEAST, var, formula);
	}

	// Expand the formula inside of a hide application. This replaces all modalities so that events that do not
	// appear inside of this formula are ignored.
	public static Formula expandOneHide(Formula formula, Set<String> fullAlphabet) {
		Set<String> subAlphabet = AlphabetFinder.getAlphabet(formula);
		assert subAlphabet != null;
		Set<String> remainingAlphabet = new HashSet<>(fullAlphabet);
		remainingAlphabet.removeAll(subAlphabet);

		HidingExpandModalityTransformer hide = new HidingExpandModalityTransformer(remainingAlphabet);
		NonRecursive engine = new NonRecursive();
		hide.transform(engine, formula);
		engine.run();
		return hide.transform(null, formula);
	}

	private static class HidingExpandModalityTransformer extends FormulaFormulaTransformer {
		private final Set<String> expansionAlphabet;

		public HidingExpandModalityTransformer(Set<String> expansionAlphabet) {
			this.expansionAlphabet = expansionAlphabet;
		}

		@Override
		protected void enqueueWalker(NonRecursive engine, Formula formula) {
			engine.enqueue(new Walker(formula));
		}

		class Walker extends FormulaFormulaTransformer.FillCache {
			public Walker(Formula formula) {
				super(formula);
			}

			@Override
			public Formula modality(ModalityFormula formula, Formula transformedChild) {
				FormulaCreator creator = formula.getCreator();
				VariableFormula var = creator.freshVariable("h");
				Formula result = creator.modality(formula.getModality(), formula.getEvent(), transformedChild);
				boolean conj = formula.getModality().equals(Modality.UNIVERSAL);
				for (String event : expansionAlphabet) {
					Formula sub = creator.modality(formula.getModality(), event, var);
					if (conj)
						result = creator.conjunction(result, sub);
					else
						result = creator.disjunction(result, sub);
				}
				if (conj)
					result = creator.fixedPoint(FixedPoint.GREATEST, var, result);
				else
					result = creator.fixedPoint(FixedPoint.LEAST, var, result);
				return result;
			}
		}
	}

	@Override
	public Category[] getCategories() {
		return new Category[] { Category.MISC };
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
