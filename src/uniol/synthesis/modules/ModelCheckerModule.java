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

import java.util.ArrayList;
import java.util.List;
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
import uniol.synthesis.expansion.MissingArcsFinder;
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
		outputSpec.addReturnValue("missing_arcs", String.class);
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
		List<String> missingArcs = new ArrayList<>();
		for (Tableau tableau : tableaus) {
			success |= tableau.isSuccessful();
			callback.tableau(tableau);
			missingArcs.add(new MissingArcsFinder().findMissing(tableau).toString());
		}
		String dot = callback.toString();
		String missing = missingArcs.toString();

		output.setReturnValue("result", Boolean.class, success);
		output.setReturnValue("missing_arcs", String.class, missing);
		output.setReturnValue("dot", String.class, dot);
	}

	@Override
	public Category[] getCategories() {
		return new Category[] { Category.MISC };
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
