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

package uniol.synthesis.util;

import java.util.HashMap;
import java.util.Map;

import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class SubstitutionTransformer extends FormulaFormulaTransformer {
	private final Map<VariableFormula, Formula> substitution;

	public SubstitutionTransformer(Map<VariableFormula, Formula> substitution) {
		this.substitution = substitution;
	}

	@Override
	protected void enqueueWalker(NonRecursive engine, Formula formula) {
		engine.enqueue(new Worker(formula));
	}

	private class Worker extends FormulaFormulaTransformer.FillCache {
		private Worker(Formula formula) {
			super(formula);
		}

		@Override
		public void walk(NonRecursive engine, VariableFormula formula) {
			Formula expansion = null;
			if (!isCurrentlyBound(formula))
				expansion = substitution.get(formula);
			if (expansion == null)
				setCache(formula, formula);
			else
				setCache(formula, expansion);
		}
	}

	static public Formula substitute(Formula formula, Map<VariableFormula, Formula> substitution) {
		NonRecursive engine = new NonRecursive();
		SubstitutionTransformer transformer = new SubstitutionTransformer(substitution);
		transformer.transform(engine, formula);
		engine.run();
		Formula result = transformer.transform(engine, formula);
		assert result != null;
		return result;
	}

	static public Formula substitute(Formula formula, VariableFormula variable, Formula substitution) {
		Map<VariableFormula, Formula> sub = new HashMap<>();
		sub.put(variable, substitution);
		return substitute(formula, sub);
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
