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

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import uniol.apt.adt.ts.TransitionSystem;
import uniol.apt.analysis.synthesize.AbstractSynthesizeModule;
import uniol.apt.analysis.synthesize.PNProperties;
import uniol.apt.io.renderer.RenderException;
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
import uniol.apt.util.Pair;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.expansion.RealiseFormula;
import uniol.synthesis.tableau.Tableau;
import uniol.synthesis.util.NonRecursive;

@AptModule
public class RealisationModule extends AbstractModule implements Module {
	@Override
	public String getShortDescription() {
		return "Compute Petri net realisations of a formula";
	}

	@Override
	public String getName() {
		return "realise_pn";
	}

	@Override
	public void require(ModuleInputSpec inputSpec) {
		inputSpec.addParameter("options", String.class, "Comma separated list of options");
		inputSpec.addParameter("formula", Formula.class, "The formula that should be checked");
	}

	@Override
	public void provide(ModuleOutputSpec outputSpec) {
		outputSpec.addReturnValue("result", String.class,
				ModuleOutputSpec.PROPERTY_FILE, ModuleOutputSpec.PROPERTY_RAW);
	}

	@Override
	public void run(ModuleInput input, ModuleOutput output) throws ModuleException {
		String options = input.getParameter("options", String.class);
		PNProperties properties = AbstractSynthesizeModule.Options.parseProperties(options).properties;
		Formula formula = input.getParameter("formula", Formula.class);

		if (!properties.isKBounded())
			System.err.println(
					"Warning: Without requiring k-boundedness this algorithm might not terminate!");

		final StringWriter resultString = new StringWriter();
		final String[] separator = new String[] { "" };

		new NonRecursive().run(new RealiseFormula(properties, formula, new RealiseFormula.RealisationCallback() {
			@Override
			public void foundRealisation(TransitionSystem ts, Tableau tableau) {
				resultString.append(separator[0]);
				try {
					new AptLTSRenderer().render(ts, resultString);
				} catch (RenderException e) {
					throw new AssertionError("The AptLTSRenderer should not throw RenderException", e);
				} catch (IOException e) {
					throw new AssertionError("A StringWriter should not throw IOException", e);
				}

				separator[0] = "\n\n===========\n\n";
			}
		}));

		output.setReturnValue("result", String.class, resultString.toString());
	}

	@Override
	public Category[] getCategories() {
		return new Category[] { Category.MISC };
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
