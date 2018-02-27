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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.testng.SkipException;
import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.fail;

@SuppressWarnings("unchecked")
public class FormulaCreatorTest {
	static private void clearSoftReferences() {
		WeakReference<Object> ref = new WeakReference<>(new Object());
		try {
			List<Object> list = new ArrayList<>();
			long maxSize = Runtime.getRuntime().freeMemory();
			int size = maxSize <= Integer.MAX_VALUE ? (int) maxSize : Integer.MAX_VALUE;

			// Too large allocations apparently cause a throw without clearing references. Work around that.
			size /= 2;

			while (true) {
				list.add(new byte[size]);
			}
		} catch (OutOfMemoryError err) {
			assert ref.get() == null;
			return;
		}
	}

	@Test
	public void testNonExistingGet() {
		FormulaCreator creator = new FormulaCreator();
		assertThat(creator.getFormulasWithHashCode(42), emptyIterable());
	}

	private boolean runningOnCI() {
		return "true".equals(System.getenv("CI"));
	}

	@Test
	public void testAdd() {
		if (runningOnCI())
			throw new SkipException("Running this test would cause the OOM killer to be invoked");

		final FormulaCreator creator = new FormulaCreator();
		Formula formula1 = new Formula() {
			@Override
			public FormulaCreator getCreator() {
				return creator;
			}
		};
		Formula formula2 = new Formula() {
			@Override
			public FormulaCreator getCreator() {
				return creator;
			}
		};

		creator.addFormulaInternal(42, formula1);
		creator.addFormulaInternal(42, formula2);
		assertThat(creator.getFormulasWithHashCode(42), containsInAnyOrder(formula1, formula2));

		formula1 = null;
		clearSoftReferences();
		assertThat(creator.getFormulasWithHashCode(42), contains(formula2));

		formula2 = null;
		clearSoftReferences();
		assertThat(creator.getFormulasWithHashCode(42), emptyIterable());
	}

	@Test
	public void testNoSuchElement() {
		final FormulaCreator creator = new FormulaCreator();
		Formula formula1 = new Formula() {
			@Override
			public FormulaCreator getCreator() {
				return creator;
			}
		};
		creator.addFormulaInternal(42, formula1);
		Iterator<Formula> iterator = creator.getFormulasWithHashCode(42).iterator();

		assertThat(iterator.hasNext(), is(true));
		assertThat(iterator.next(), sameInstance(formula1));

		assertThat(iterator.hasNext(), is(false));
		try {
			iterator.next();
			fail();
		} catch (NoSuchElementException e) {
			// Good!
		}
	}

	@Test
	public void testConstant() {
		FormulaCreator creator = new FormulaCreator();
		ConstantFormula True = creator.constant(true);
		ConstantFormula False = creator.constant(false);
		assertThat(True.getValue(), is(true));
		assertThat(False.getValue(), is(false));
		assertThat(True, sameInstance(creator.constant(true)));
		assertThat(False, sameInstance(creator.constant(false)));
	}

	@Test
	public void testNegate() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		NegationFormula negate = creator.negate(True);
		assertThat(negate.getFormula(), sameInstance(True));
		assertThat(negate, sameInstance(creator.negate(True)));
	}

	@Test
	public void testConjunction() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);
		ConjunctionFormula conj = creator.conjunction(True, False);
		assertThat(conj.getFormulas(), contains(sameInstance(True), sameInstance(False)));
		assertThat(conj, sameInstance(creator.conjunction(True, False)));
	}

	@Test
	public void testConjunctionFlattening() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);
		ConjunctionFormula conj = creator.conjunction(True, False);
		conj = creator.conjunction(conj, conj);
		assertThat(conj.getFormulas(), contains(sameInstance(True), sameInstance(False), sameInstance(True),
					sameInstance(False)));
		assertThat(conj, sameInstance(creator.conjunction(True, False, True, False)));
	}

	@Test
	public void testDisjunction() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);
		DisjunctionFormula disj = creator.disjunction(True, False);
		assertThat(disj.getFormulas(), contains(sameInstance(True), sameInstance(False)));
		assertThat(disj, sameInstance(creator.disjunction(True, False)));
	}

	@Test
	public void testDisjunctionFlattening() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);
		DisjunctionFormula disj = creator.disjunction(True, False);
		disj = creator.disjunction(disj, disj);
		assertThat(disj.getFormulas(), contains(sameInstance(True), sameInstance(False), sameInstance(True),
					sameInstance(False)));
		assertThat(disj, sameInstance(creator.disjunction(True, False, True, False)));
	}

	@Test
	public void testVariable() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var = creator.variable("foo");
		assertThat(var.getVariable(), equalTo("foo"));
		assertThat(var, sameInstance(creator.variable("foo")));
		assertThat(var, not(sameInstance(creator.variable("bar"))));
	}

	@Test
	public void testFreshVariable() {
		FormulaCreator creator = new FormulaCreator();
		VariableFormula var0 = creator.freshVariable("foo");
		VariableFormula var1 = creator.freshVariable("foo");
		VariableFormula var2 = creator.freshVariable("foo");
		assertThat(var0.getVariable(), equalTo("foo0"));
		assertThat(var1.getVariable(), equalTo("foo1"));
		assertThat(var2.getVariable(), equalTo("foo2"));
	}

	@Test
	public void testFixedPoint() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		VariableFormula var = creator.variable("foo");
		FixedPointFormula fpf = creator.fixedPoint(FixedPoint.LEAST, var, True);
		assertThat(fpf.getFixedPoint(), equalTo(FixedPoint.LEAST));
		assertThat(fpf.getVariable(), sameInstance(var));
		assertThat(fpf.getFormula(), sameInstance(True));
		assertThat(fpf, sameInstance(creator.fixedPoint(FixedPoint.LEAST, var, True)));
	}

	@Test
	public void testModality() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		String event = "foo";
		ModalityFormula mf = creator.modality(Modality.UNIVERSAL, event, True);
		assertThat(mf.getModality(), equalTo(Modality.UNIVERSAL));
		assertThat(mf.getEvent(), sameInstance(event));
		assertThat(mf.getFormula(), sameInstance(True));
		assertThat(mf, sameInstance(creator.modality(Modality.UNIVERSAL, event, True)));
	}

	@Test
	public void testLet() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);
		VariableFormula var = creator.variable("foo");
		LetFormula lf = creator.let(var, True, False);
		assertThat(lf.getVariable(), sameInstance(var));
		assertThat(lf.getExpansion(), sameInstance(True));
		assertThat(lf.getFormula(), sameInstance(False));
		assertThat(lf, sameInstance(creator.let(var, True, False)));
	}

	@Test
	public void testCallNoArgs() {
		FormulaCreator creator = new FormulaCreator();
		String function = "function";
		CallFormula call = creator.call(function);
		assertThat(call.getFunction(), equalTo(function));
		assertThat(call.getArguments(), emptyIterable());
		assertThat(call, sameInstance(creator.call(function)));
	}

	@Test
	public void testCallSomeArgs() {
		FormulaCreator creator = new FormulaCreator();
		String function = "function";
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);
		VariableFormula var = creator.variable("foo");
		CallFormula call = creator.call(function, True, False, var);
		assertThat(call.getFunction(), equalTo(function));
		assertThat(call.getArguments(), contains(True, False, var));
		assertThat(call, sameInstance(creator.call(function, True, False, var)));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
