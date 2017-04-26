package uniol.synthesis.modules;

import java.util.Set;

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
import uniol.synthesis.tableau.GraphvizProgressCallback;
import uniol.synthesis.tableau.Tableau;
import uniol.synthesis.tableau.TableauBuilder;

@AptModule
public class ModelCheckerModule extends AbstractModule implements Module {
	@Override
	public String getShortDescription() {
		return "Check if an LTS satisfies a formula";
	}

	@Override
	public String getName() {
		return "model_check";
	}

	@Override
	public void require(ModuleInputSpec inputSpec) {
		inputSpec.addParameter("lts", TransitionSystem.class, "The LTS that should be checked");
		inputSpec.addParameter("formula", Formula.class, "The formula that should be checked");
	}

	@Override
	public void provide(ModuleOutputSpec outputSpec) {
		outputSpec.addReturnValue("result", Boolean.class, ModuleOutputSpec.PROPERTY_SUCCESS);
		outputSpec.addReturnValue("dot", String.class,
				ModuleOutputSpec.PROPERTY_FILE, ModuleOutputSpec.PROPERTY_RAW);
	}

	@Override
	public void run(ModuleInput input, ModuleOutput output) throws ModuleException {
		TransitionSystem lts = input.getParameter("lts", TransitionSystem.class);
		Formula formula = input.getParameter("formula", Formula.class);

		GraphvizProgressCallback callback = new GraphvizProgressCallback();
		Set<Tableau> tableaus = new TableauBuilder(callback)
			.createTableaus(lts.getInitialState(), formula);

		boolean success = false;
		for (Tableau tableau : tableaus) {
			success |= tableau.isSuccessful();
			callback.tableau(tableau);
		}
		String dot = callback.toString();

		output.setReturnValue("result", Boolean.class, success);
		output.setReturnValue("dot", String.class, dot);
	}

	@Override
	public Category[] getCategories() {
		return new Category[] { Category.MISC };
	}
}
