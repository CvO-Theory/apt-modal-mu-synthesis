package uniol.synthesis.adt.mu_calculus;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import uniol.synthesis.adt.mu_calculus.ConjunctionFormula;
import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.DisjunctionFormula;
import uniol.synthesis.adt.mu_calculus.Event;
import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.NegationFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

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
		} catch(OutOfMemoryError err) {
			assert ref.get() == null;
			return;
		}
	}

	@Test
	public void testNonExistingGet() {
		FormulaCreator creator = new FormulaCreator();
		assertThat(creator.getFormulasWithHashCode(42), emptyIterable());
	}

	@Test
	public void testAdd() {
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
		assertThat(conj.getLeft(), sameInstance(True));
		assertThat(conj.getRight(), sameInstance(False));
		assertThat(conj, sameInstance(creator.conjunction(True, False)));
	}

	@Test
	public void testDisjunction() {
		FormulaCreator creator = new FormulaCreator();
		Formula True = creator.constant(true);
		Formula False = creator.constant(false);
		DisjunctionFormula disj = creator.disjunction(True, False);
		assertThat(disj.getLeft(), sameInstance(True));
		assertThat(disj.getRight(), sameInstance(False));
		assertThat(disj, sameInstance(creator.disjunction(True, False)));
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
		assertThat(var0.getVariable(), equalTo("{foo0}"));
		assertThat(var1.getVariable(), equalTo("{foo1}"));
		assertThat(var2.getVariable(), equalTo("{foo2}"));
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
		Event event = new Event("foo");
		ModalityFormula mf = creator.modality(Modality.UNIVERSAL, event, True);
		assertThat(mf.getModality(), equalTo(Modality.UNIVERSAL));
		assertThat(mf.getEvent(), sameInstance(event));
		assertThat(mf.getFormula(), sameInstance(True));
		assertThat(mf, sameInstance(creator.modality(Modality.UNIVERSAL, event, True)));
	}
}
