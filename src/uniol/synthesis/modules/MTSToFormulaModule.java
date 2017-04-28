package uniol.synthesis.modules;

import uniol.apt.adt.ts.TransitionSystem;
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
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.mts.MTSToFormula;

@AptModule
public class MTSToFormulaModule extends AbstractModule implements Module {
	@Override
	public String getShortDescription() {
		return "Transform a MTS into a formula";
	}

	@Override
	public String getLongDescription() {
		return getShortDescription() + ". A MTS has may and must arcs. For this module, a may arc is an edge"
			+ " with an extension called 'may'. For example, write 's1 a s2 [may]' in a file for a may arc"
			+ " from s1 to s2. Any other arc is both a may and a must arc.";
	}

	@Override
	public String getName() {
		return "mts_to_formula";
	}

	@Override
	public void require(ModuleInputSpec inputSpec) {
		inputSpec.addParameter("mts", TransitionSystem.class, "The MTS to transform");
	}

	@Override
	public void provide(ModuleOutputSpec outputSpec) {
		outputSpec.addReturnValue("formula", Formula.class);
	}

	@Override
	public void run(ModuleInput input, ModuleOutput output) throws ModuleException {
		TransitionSystem mts = input.getParameter("mts", TransitionSystem.class);
		Formula formula = new MTSToFormula().mtsToFormula(new FormulaCreator(), mts);
		output.setReturnValue("formula", Formula.class, formula);
	}

	@Override
	public Category[] getCategories() {
		return new Category[] { Category.MISC };
	}
}
