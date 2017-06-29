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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import uniol.synthesis.adt.mu_calculus.ConjunctionFormula;
import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.DisjunctionFormula;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.LetFormula;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.NegationFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class GetFreeVariables {
	private final static Map<Formula, Map<VariableFormula, Integer>> freeVariables
		= Collections.synchronizedMap(new WeakHashMap<Formula, Map<VariableFormula, Integer>>());

	private static class FillCache extends FormulaWalker {
		private FillCache(Formula formula) {
			super(formula);
		}

		@Override
		public void walk(NonRecursive engine, ConstantFormula formula) {
			freeVariables.put(formula, Collections.<VariableFormula, Integer>emptyMap());
		}

		static Map<VariableFormula, Integer> union(Map<VariableFormula, Integer> map1, Map<VariableFormula, Integer> map2) {
			// TODO: Optimise
			Map<VariableFormula, Integer> result = new HashMap<>();
			result.putAll(map1);
			for (Map.Entry<VariableFormula, Integer> entry : map2.entrySet()) {
				Integer count = result.get(entry.getKey());
				if (count == null)
					count = entry.getValue();
				else
					count = entry.getValue() + count;
				result.put(entry.getKey(), count);
			}
			return result;
		}

		private void handleChild(NonRecursive engine, Formula formula, Formula child) {
			Map<VariableFormula, Integer> map = freeVariables.get(child);
			if (map == null) {
				engine.enqueue(this);
				engine.enqueue(new FillCache(child));
				return;
			}
			freeVariables.put(formula, map);
		}

		private void handleChildren(NonRecursive engine, Formula formula, Formula child1, Formula child2) {
			Map<VariableFormula, Integer> map1 = freeVariables.get(child1);
			Map<VariableFormula, Integer> map2 = freeVariables.get(child2);
			if (map1 == null || map2 == null) {
				engine.enqueue(this);
				if (map1 == null)
					engine.enqueue(new FillCache(child1));
				if (map2 == null)
					engine.enqueue(new FillCache(child2));
				return;
			}
			freeVariables.put(formula, union(map1, map2));
		}

		@Override
		public void walk(NonRecursive engine, ConjunctionFormula formula) {
			handleChildren(engine, formula, formula.getLeft(), formula.getRight());
		}

		@Override
		public void walk(NonRecursive engine, DisjunctionFormula formula) {
			handleChildren(engine, formula, formula.getLeft(), formula.getRight());
		}

		@Override
		public void walk(NonRecursive engine, NegationFormula formula) {
			handleChild(engine, formula, formula.getFormula());
		}

		@Override
		public void walk(NonRecursive engine, VariableFormula formula) {
			Map<VariableFormula, Integer> map = new HashMap<>();
			map.put(formula, 1);
			freeVariables.put(formula, map);
		}

		@Override
		public void walk(NonRecursive engine, ModalityFormula formula) {
			handleChild(engine, formula, formula.getFormula());
		}

		@Override
		public void walk(NonRecursive engine, FixedPointFormula formula) {
			Map<VariableFormula, Integer> map = freeVariables.get(formula.getFormula());
			if (map == null) {
				engine.enqueue(this);
				engine.enqueue(new FillCache(formula.getFormula()));
				return;
			}
			if (map.containsKey(formula.getVariable())) {
				map = new HashMap<>(map);
				map.remove(formula.getVariable());
			}
			freeVariables.put(formula, map);
		}

		@Override
		public void walk(NonRecursive engine, LetFormula formula) {
			Map<VariableFormula, Integer> mapExpansion = freeVariables.get(formula.getExpansion());
			Map<VariableFormula, Integer> mapInner = freeVariables.get(formula.getFormula());
			if (mapExpansion == null || mapInner == null) {
				engine.enqueue(this);
				if (mapExpansion == null)
					engine.enqueue(new FillCache(formula.getExpansion()));
				if (mapInner == null)
					engine.enqueue(new FillCache(formula.getFormula()));
				return;
			}
			if (mapInner.containsKey(formula.getVariable())) {
				mapInner = new HashMap<>(mapInner);
				mapInner.remove(formula.getVariable());
				freeVariables.put(formula, union(mapInner, mapExpansion));
			} else
				freeVariables.put(formula, mapInner);
		}
	}

	static public Map<VariableFormula, Integer> getFreeVariablesCounts(Formula formula) {
		Map<VariableFormula, Integer> result = freeVariables.get(formula);
		if (result == null) {
			new NonRecursive().run(new FillCache(formula));
			result = freeVariables.get(formula);
		}
		return Collections.unmodifiableMap(result);
	}

	static public Set<VariableFormula> getFreeVariables(Formula formula) {
		return getFreeVariablesCounts(formula).keySet();
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
