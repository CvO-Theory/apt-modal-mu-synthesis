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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uniol.synthesis.adt.mu_calculus.CallFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.LetFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class UnLetTransformer extends FormulaFormulaTransformer {
	private final Map<VariableFormula, Formula> substitution = new HashMap<>();

	protected void enqueueWalker(NonRecursive engine, Formula formula) {
		engine.enqueue(new Worker(formula));
	}

	final private class Worker extends FormulaFormulaTransformer.FillCache {
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

		@Override
		public void walk(NonRecursive engine, final LetFormula formula) {
			Formula expansion = formula.getExpansion();
			final Formula transformedExpansion = getCache(expansion);
			if (transformedExpansion == null) {
				engine.enqueue(this);
				enqueueWalker(engine, expansion);
				return;
			}
			final VariableFormula variable = formula.getVariable();
			final Formula old = substitution.put(variable, transformedExpansion);
			engine.enqueue(new NonRecursive.Walker() {
				@Override
				public void walk(NonRecursive engine) {
					Formula result = getCache(formula.getFormula());
					assert result != null;

					Formula changed;
					if (old == null) {
						changed = substitution.remove(variable);
					} else {
						changed = substitution.put(variable, old);
					}
					assert transformedExpansion.equals(changed);
					popScope();

					setCache(formula, result);
				}
			});
			enqueueWalker(engine, formula.getFormula());
			pushScope();
		}

		@Override
		public void walk(NonRecursive engine, CallFormula formula) {
			List<Formula> transformedArguments = new ArrayList<>(formula.getArguments().size());
			boolean hadUnhandled = false;
			for (Formula argument : formula.getArguments()) {
				Formula transformed = getCache(argument);
				if (transformed != null) {
					transformedArguments.add(transformed);
				} else {
					if (!hadUnhandled)
						engine.enqueue(this);
					hadUnhandled = true;
					enqueueWalker(engine, argument);
				}
			}
			if (hadUnhandled)
				return;
			setCache(formula, formula.getCreator().
					call(formula.getFunction(), transformedArguments));
		}
	}

	static public Formula unLet(Formula formula) {
		NonRecursive engine = new NonRecursive();
		UnLetTransformer transformer = new UnLetTransformer();
		Formula result = transformer.transform(engine, formula);
		if (result == null) {
			engine.run();
			result = transformer.transform(engine, formula);
			assert result != null;
		}
		return result;
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
