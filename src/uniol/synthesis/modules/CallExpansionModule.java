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
		Formula ex1 = creator.conjunction(
				creator.call("hide", creator.modality(Modality.EXISTENTIAL, "a", creator.variable("P"))),
				creator.modality(Modality.UNIVERSAL, "b", creator.constant(true)));
		Formula ex1hidden = handleHide(ex1);
		return getShortDescription() + ". The supported functions are:\n" +
			"- hide(beta) hides the environment in beta.\n" +
			"\nIn hide(beta), a local and a global alphabet is computed by collecting the events that " +
			"appear inside of modalities in beta and in the full formula. Each modality inside of beta " +
			"is replaced so that all events which are not local are hidden, e.g. " + ex1 + ", which " +
			"means 'after a, P holds' is replaced with " + ex1hidden + ", which means that after a " +
			"finite sequence of b's, there is an a after which P holds.";

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
		output.setReturnValue("formula", Formula.class, handleHide(formula));
	}

	// Get the alphabet and use HidingFindingTransformer to find & transform all hide invocations
	public static Formula handleHide(Formula formula) {
		formula = unLet(formula);
		Set<String> alphabet = AlphabetFinder.getAlphabet(formula);
		HidingFindingTransformer hide = new HidingFindingTransformer(alphabet);
		NonRecursive engine = new NonRecursive();
		hide.transform(engine, formula);
		engine.run();
		return hide.transform(engine, formula);
	}

	private static class HidingFindingTransformer extends FormulaFormulaTransformer {
		private final Set<String> fullAlphabet;

		public HidingFindingTransformer(Set<String> fullAlphabet) {
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
				Formula replacement = formula;
				if (formula.getFunction().equals("hide")) {
					if (formula.getArguments().size() != 1)
						throw new RuntimeException("Need exactly one argument, but found: " + formula);
					Formula arg = formula.getArguments().get(0);
					Formula expanded = getCache(arg);
					if (expanded == null) {
						engine.enqueue(this);
						enqueueWalker(engine, arg);
						return;
					}
					replacement = expandOneHide(arg, fullAlphabet);
				}
				setCache(formula, replacement);
			}
		}
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
						result = creator.conjunction(sub, result);
					else
						result = creator.disjunction(sub, result);
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
