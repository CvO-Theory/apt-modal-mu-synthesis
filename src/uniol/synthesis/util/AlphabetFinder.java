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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uniol.synthesis.adt.mu_calculus.CallFormula;
import uniol.synthesis.adt.mu_calculus.ConjunctionFormula;
import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.DisjunctionFormula;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.NegationFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class AlphabetFinder extends FormulaTransformer<Set<String>> {
	@Override
	protected void enqueueWalker(NonRecursive engine, Formula formula) {
		engine.enqueue(new Worker(formula));
	}

	final private class Worker extends FormulaTransformer<Set<String>>.FillCache {
		private Worker(Formula formula) {
			super(formula);
		}

		private Set<String> union(List<Set<String>> sets) {
			Set<String> result = new HashSet<>();
			for (Set<String> set : sets)
				result.addAll(set);
			return result;
		}

		@Override
		public void walk(NonRecursive engine, ConstantFormula formula) {
			setCache(formula, Collections.<String>emptySet());
		}

		@Override
		public void walk(NonRecursive engine, VariableFormula formula) {
			setCache(formula, Collections.<String>emptySet());
		}

		@Override
		public Set<String> conjunction(ConjunctionFormula formula, List<Set<String>> transformedChildren) {
			return union(transformedChildren);
		}

		@Override
		public Set<String> disjunction(DisjunctionFormula formula, List<Set<String>> transformedChildren) {
			return union(transformedChildren);
		}

		@Override
		public Set<String> negate(NegationFormula formula, Set<String> transformedChild) {
			return transformedChild;
		}

		@Override
		public Set<String> modality(ModalityFormula formula, Set<String> transformedChild) {
			if (transformedChild.contains(formula.getEvent()))
				return transformedChild;
			Set<String> result = new HashSet<>(transformedChild);
			result.add(formula.getEvent());
			return result;
		}

		@Override
		public Set<String> fixedPoint(FixedPointFormula formula, Set<String> transformedChild) {
			return transformedChild;
		}

		@Override
		public void walk(NonRecursive engine, CallFormula formula) {
			boolean enqueuedSelf = false;
			Set<String> result = new HashSet<>();
			for (Formula arg : formula.getArguments()) {
				Set<String> transform = getCache(arg);
				if (transform != null) {
					result.addAll(transform);
					continue;
				}
				if (!enqueuedSelf)
					engine.enqueue(this);
				enqueuedSelf = true;
				enqueueWalker(engine, arg);
			}
			if (enqueuedSelf)
				return;
			setCache(formula, result);
		}
	}

	@Override
	public Set<String> transform(NonRecursive engine, Formula formula) {
		Set<String> result = super.transform(engine, formula);
		if (result != null)
			result = Collections.unmodifiableSet(result);
		return result;
	}

	static public Set<String> getAlphabet(Formula formula) {
		NonRecursive engine = new NonRecursive();
		AlphabetFinder worker = new AlphabetFinder();
		worker.transform(engine, formula);
		engine.run();
		return worker.transform(engine, formula);
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
