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
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;

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

public abstract class FormulaTransformer<C> {
	private final Bag<VariableFormula> currentlyBoundVariables = new HashBag<>();
	private final Deque<Map<Formula, C>> cache = new ArrayDeque<>();

	public FormulaTransformer() {
		reset();
	}

	protected void reset() {
		currentlyBoundVariables.clear();
		cache.clear();
		cache.addLast(new HashMap<Formula, C>());
	}

	public C transform(NonRecursive engine, Formula formula) {
		C result = getCache(formula);
		if (result == null)
			enqueueWalker(engine, formula);
		return result;
	}

	protected C getCache(Formula formula) {
		return cache.getLast().get(formula);
	}

	protected void setCache(Formula formula, C result) {
		cache.getLast().put(formula, result);
	}

	protected void pushScope() {
		cache.addLast(new HashMap<Formula, C>());
	}

	protected void popScope() {
		cache.removeLast();
	}

	protected boolean isCurrentlyBound(VariableFormula formula) {
		return currentlyBoundVariables.contains(formula);
	}

	protected void enterScope(VariableFormula formula) {
		currentlyBoundVariables.add(formula);
	}

	protected void exitScope(VariableFormula formula) {
		currentlyBoundVariables.remove(formula, 1);
	}

	protected abstract void enqueueWalker(NonRecursive engine, Formula formula);

	protected abstract class FillCache extends FormulaWalker {
		public FillCache(Formula formula) {
			super(formula);
		}

		// TODO: Make these abstract
		public C conjunction(ConjunctionFormula formula, List<C> transformed) {
			assert transformed.size() == 2;
			return conjunction(formula, transformed.get(0), transformed.get(1));
		}

		public C disjunction(DisjunctionFormula formula, List<C> transformed) {
			assert transformed.size() == 2;
			return disjunction(formula, transformed.get(0), transformed.get(1));
		}

		abstract public C negate(NegationFormula formula, C transformedChild);
		abstract public C modality(ModalityFormula formula, C transformedChild);
		abstract public C fixedPoint(FixedPointFormula formula, C transformedChild);

		@Deprecated
		public C conjunction(ConjunctionFormula formula, C transformedLeft, C transformedRight) {
			throw new AssertionError("This code should not be reached");
		}

		@Deprecated
		public C disjunction(DisjunctionFormula formula, C transformedLeft, C transformedRight) {
			throw new AssertionError("This code should not be reached");
		}

		@Override
		public void walk(NonRecursive engine, ConjunctionFormula formula) {
			List<C> transformed = new ArrayList<>();
			boolean enqueuedSelf = false;
			for (Formula child : formula.getFormulas()) {
				C transformedChild = getCache(child);
				if (transformedChild == null) {
					if (!enqueuedSelf)
						engine.enqueue(this);
					enqueuedSelf = true;
					enqueueWalker(engine, child);
				} else
					transformed.add(transformedChild);
			}
			if (enqueuedSelf)
				return;

			setCache(formula, conjunction(formula, transformed));
		}

		@Override
		public void walk(NonRecursive engine, DisjunctionFormula formula) {
			List<C> transformed = new ArrayList<>();
			boolean enqueuedSelf = false;
			for (Formula child : formula.getFormulas()) {
				C transformedChild = getCache(child);
				if (transformedChild == null) {
					if (!enqueuedSelf)
						engine.enqueue(this);
					enqueuedSelf = true;
					enqueueWalker(engine, child);
				} else
					transformed.add(transformedChild);
			}
			if (enqueuedSelf)
				return;

			setCache(formula, disjunction(formula, transformed));
		}

		@Override
		public void walk(NonRecursive engine, NegationFormula formula) {
			Formula child = formula.getFormula();
			C transformedChild = getCache(child);
			if (transformedChild == null) {
				engine.enqueue(this);
				enqueueWalker(engine, child);
				return;
			}
			setCache(formula, negate(formula, transformedChild));
		}

		@Override
		public void walk(NonRecursive engine, ModalityFormula formula) {
			Formula child = formula.getFormula();
			C transformedChild = getCache(child);
			if (transformedChild == null) {
				engine.enqueue(this);
				enqueueWalker(engine, child);
				return;
			}
			setCache(formula, modality(formula, transformedChild));
		}

		@Override
		public void walk(NonRecursive engine, final FixedPointFormula formula) {
			enterScope(formula.getVariable());
			engine.enqueue(new NonRecursive.Walker() {
				@Override
				public void walk(NonRecursive engine) {
					C transformedChild = getCache(formula.getFormula());
					assert transformedChild != null;

					exitScope(formula.getVariable());

					setCache(formula, fixedPoint(formula, transformedChild));
				}
			});
			if (getCache(formula.getFormula()) == null)
				enqueueWalker(engine, formula.getFormula());
		}

		@Override
		public void walk(NonRecursive engine, LetFormula formula) {
			throw new IllegalArgumentException("Let formulas are not supported");
		}
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
