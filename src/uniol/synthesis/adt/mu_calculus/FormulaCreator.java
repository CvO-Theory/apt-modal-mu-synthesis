package uniol.synthesis.adt.mu_calculus;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

	public ConjunctionFormula conjunction(Formula left, Formula right) {
		synchronized(objects) {
			return ConjunctionFormula.conjunction(this, left, right);
		}
	}

	public DisjunctionFormula disjunction(Formula left, Formula right) {
		synchronized(objects) {
			return DisjunctionFormula.disjunction(this, left, right);
		}
	}

	public VariableFormula variable(String var) {
		synchronized(objects) {
			return VariableFormula.variable(this, var);
		}
	}

	public VariableFormula freshVariable(String prefix) {
		synchronized(objects) {
			return variable("{" + prefix + freshVariableCounter++ + "}");
		}
	}

	public FixedPointFormula fixedPoint(FixedPoint fixedPoint, VariableFormula var, Formula formula) {
		synchronized(objects) {
			return FixedPointFormula.fixedPoint(this, fixedPoint, var, formula);
		}
	}

	public ModalityFormula modality(Modality modality, Event event, Formula formula) {
		synchronized(objects) {
			return ModalityFormula.modality(this, modality, event, formula);
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
