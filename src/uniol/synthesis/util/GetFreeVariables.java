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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import uniol.synthesis.adt.mu_calculus.ConjunctionFormula;
import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.DisjunctionFormula;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.LetFormula;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.NegationFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class GetFreeVariables extends FormulaTransformer<Map<VariableFormula, Integer>> {
	@Override
	protected void enqueueWalker(NonRecursive engine, Formula formula) {
		engine.enqueue(new Worker(formula));
	}

	private class Worker extends FormulaTransformer<Map<VariableFormula, Integer>>.FillCache {
		private Worker(Formula formula) {
			super(formula);
		}

		private Map<VariableFormula, Integer> union(List<Map<VariableFormula, Integer>> children) {
			// TODO: Optimise
			Map<VariableFormula, Integer> result = null;
			for (Map<VariableFormula, Integer> map : children) {
				if (result == null) {
					result = new HashMap<>();
					result.putAll(map);
				} else {
					for (Map.Entry<VariableFormula, Integer> entry : map.entrySet()) {
						Integer count = result.get(entry.getKey());
						if (count == null)
							count = entry.getValue();
						else
							count = entry.getValue() + count;
						result.put(entry.getKey(), count);
					}
				}
			}
			return result;
		}

		@Override
		public void walk(NonRecursive engine, ConstantFormula formula) {
			setCache(formula,Collections.<VariableFormula, Integer>emptyMap());
		}

		@Override
		public void walk(NonRecursive engine, VariableFormula formula) {
			Map<VariableFormula, Integer> map = new HashMap<>();
			map.put(formula, 1);
			setCache(formula, map);
		}

		@Override
		public Map<VariableFormula, Integer> conjunction(ConjunctionFormula formula,
				List<Map<VariableFormula, Integer>> transformedChildren) {
			return union(transformedChildren);
		}

		@Override
		public Map<VariableFormula, Integer> disjunction(DisjunctionFormula formula,
				List<Map<VariableFormula, Integer>> transformedChildren) {
			return union(transformedChildren);
		}

		@Override
		public Map<VariableFormula, Integer> negate(NegationFormula formula,
				Map<VariableFormula, Integer> transformedChild) {
			return transformedChild;
		}

		@Override
		public Map<VariableFormula, Integer> modality(ModalityFormula formula,
				Map<VariableFormula, Integer> transformedChild) {
			return transformedChild;
		}

		@Override
		public Map<VariableFormula, Integer> fixedPoint(FixedPointFormula formula,
				Map<VariableFormula, Integer> transformedChild) {
			Map<VariableFormula, Integer> result = transformedChild;
			if (result.containsKey(formula.getVariable())) {
				result = new HashMap<>(result);
				result.remove(formula.getVariable());
			}
			return result;
		}
	}

	@Override
	public Map<VariableFormula, Integer> transform(NonRecursive engine, Formula formula) {
		Map<VariableFormula, Integer> result = super.transform(engine, formula);
		if (result != null)
			result = Collections.unmodifiableMap(result);
		return result;
	}

	static public Map<VariableFormula, Integer> getFreeVariablesCounts(Formula formula) {
		NonRecursive engine = new NonRecursive();
		GetFreeVariables worker = new GetFreeVariables();
		worker.transform(engine, formula);
		engine.run();
		return worker.transform(engine, formula);
	}

	static public Set<VariableFormula> getFreeVariables(Formula formula) {
		return getFreeVariablesCounts(formula).keySet();
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
