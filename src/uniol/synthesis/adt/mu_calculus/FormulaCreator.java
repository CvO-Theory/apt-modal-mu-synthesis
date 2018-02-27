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

package uniol.synthesis.adt.mu_calculus;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class FormulaCreator {
	final private ReferenceQueue<Formula> queue = new ReferenceQueue<>();
	final private Map<Integer, Set<MyWeakReference>> objects = new HashMap<>();
	private int freshVariableCounter = 0;

	public ConstantFormula constant(boolean value) {
		synchronized(objects) {
			return ConstantFormula.constant(this, value);
		}
	}

	public NegationFormula negate(Formula formula) {
		synchronized(objects) {
			return NegationFormula.negate(this, formula);
		}
	}

	public ConjunctionFormula conjunction(Formula... children) {
		return conjunction(Arrays.asList(children));
	}

	public ConjunctionFormula conjunction(List<Formula> children) {
		synchronized(objects) {
			return ConjunctionFormula.conjunction(this, children);
		}
	}

	public DisjunctionFormula disjunction(Formula... children) {
		return disjunction(Arrays.asList(children));
	}

	public DisjunctionFormula disjunction(List<Formula> children) {
		synchronized(objects) {
			return DisjunctionFormula.disjunction(this, children);
		}
	}

	public VariableFormula variable(String var) {
		synchronized(objects) {
			return VariableFormula.variable(this, var, false);
		}
	}

	public VariableFormula freshVariable(String prefix) {
		synchronized(objects) {
			while (true) {
				String name = prefix + freshVariableCounter++;
				VariableFormula var = VariableFormula.variable(this, name, true);
				if (var != null)
					return var;
			}
		}
	}

	public FixedPointFormula fixedPoint(FixedPoint fixedPoint, VariableFormula var, Formula formula) {
		synchronized(objects) {
			return FixedPointFormula.fixedPoint(this, fixedPoint, var, formula);
		}
	}

	public ModalityFormula modality(Modality modality, String event, Formula formula) {
		synchronized(objects) {
			return ModalityFormula.modality(this, modality, event, formula);
		}
	}

	public LetFormula let(VariableFormula variable, Formula expansion, Formula formula) {
		synchronized(objects) {
			return LetFormula.let(this, variable, expansion, formula);
		}
	}

	public CallFormula call(String function, Formula... arguments) {
		return call(function, Arrays.asList(arguments));
	}

	public CallFormula call(String function, List<Formula> arguments) {
		synchronized(objects) {
			return CallFormula.call(this, function, arguments);
		}
	}

	Iterable<Formula> getFormulasWithHashCode(int hashCode) {
		cleanup();
		final Set<MyWeakReference> set = objects.get(hashCode);
		if (set == null)
			return Collections.emptySet();
		return new Iterable<Formula>() {
			@Override
			public Iterator<Formula> iterator() {
				return new FormulaIterator(set);
			}
		};
	}

	void addFormulaInternal(int hashCode, Formula formula) {
		cleanup();
		Set<MyWeakReference> objs = objects.get(hashCode);
		if (objs == null) {
			objs = new HashSet<>();
			objects.put(hashCode, objs);
		}
		objs.add(new MyWeakReference(hashCode, formula, queue));
	}

	private void cleanup() {
		while (true) {
			Reference<? extends Formula> ref = queue.poll();
			if (ref == null)
				break;

			MyWeakReference reference = (MyWeakReference) ref;
			Set<MyWeakReference> objs = objects.get(reference.getHashCode());
			boolean removed = objs.remove(reference);
			assert removed;
			if (objs.isEmpty()) {
				Set<MyWeakReference> set = objects.remove(reference.getHashCode());
				assert set == objs;
			}
		}
	}

	static private class FormulaIterator implements Iterator<Formula> {
		final private Iterator<MyWeakReference> iterator;
		private Formula nextFormula;

		public FormulaIterator(Set<MyWeakReference> set) {
			iterator = set.iterator();
			nextFormula = null;
		}

		@Override
		public boolean hasNext() {
			while (nextFormula == null && iterator.hasNext()) {
				nextFormula = iterator.next().get();
			}
			return nextFormula != null;
		}

		@Override
		public Formula next() {
			if (!hasNext())
				throw new NoSuchElementException();
			Formula result = nextFormula;
			nextFormula = null;
			return result;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	static private class MyWeakReference extends WeakReference<Formula> {
		final private int hashCode;

		public MyWeakReference(int hashCode, Formula formula, ReferenceQueue<Formula> queue) {
			super(formula, queue);
			this.hashCode = hashCode;
		}

		public int getHashCode() {
			return hashCode;
		}
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
