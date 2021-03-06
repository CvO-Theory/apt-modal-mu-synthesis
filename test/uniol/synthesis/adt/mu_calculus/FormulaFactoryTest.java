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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class FormulaFactoryTest {
	static private class WrongFormula implements Formula {
		final private FormulaCreator creator;

		public WrongFormula(FormulaCreator creator) {
			this.creator = creator;
		}

		@Override
		public FormulaCreator getCreator() {
			return creator;
		}
	}

	private void stubCreator(FormulaCreator creator, Formula... formulas) {
		List<Formula> list = new ArrayList<>(Arrays.asList(formulas));
		list.add(new WrongFormula(creator));
		when(creator.getFormulasWithHashCode(anyInt())).thenReturn(list);
	}

	@Test
	public void testConjunctionMiss() {
		FormulaCreator creator = mock(FormulaCreator.class);
		Formula left = mock(Formula.class);
		Formula right = mock(Formula.class);
		Formula other = mock(Formula.class);
		stubCreator(creator,
				new ConjunctionFormula(creator, new Formula[] { other, other }),
				new ConjunctionFormula(creator, new Formula[] { left, other }),
				new ConjunctionFormula(creator, new Formula[] { other, right }));

		ConjunctionFormula formula = ConjunctionFormula.conjunction(creator, Arrays.asList(left, right));
		assertThat(formula.getFormulas(), contains(equalTo(left), equalTo(right)));
	}

	@Test
	public void testConjunctionHit() {
		FormulaCreator creator = mock(FormulaCreator.class);
		Formula left = mock(Formula.class);
		Formula right = mock(Formula.class);
		ConjunctionFormula expected = new ConjunctionFormula(creator, new Formula[] { left, right });
		stubCreator(creator, expected);

		assertThat(ConjunctionFormula.conjunction(creator, Arrays.asList(left, right)), sameInstance(expected));
	}

	@Test
	public void testConstantMiss() {
		FormulaCreator creator = mock(FormulaCreator.class);
		stubCreator(creator, new ConstantFormula(creator, false));

		ConstantFormula formula = ConstantFormula.constant(creator, true);
		assertThat(formula.getValue(), equalTo(true));
	}

	@Test
	public void testConstantHit() {
		FormulaCreator creator = mock(FormulaCreator.class);
		ConstantFormula expected = new ConstantFormula(creator, true);
		stubCreator(creator, expected);

		assertThat(ConstantFormula.constant(creator, true), sameInstance(expected));
	}

	@Test
	public void testDisjunctionMiss() {
		FormulaCreator creator = mock(FormulaCreator.class);
		Formula left = mock(Formula.class);
		Formula right = mock(Formula.class);
		Formula other = mock(Formula.class);
		stubCreator(creator,
				new DisjunctionFormula(creator, new Formula[] { other, other }),
				new DisjunctionFormula(creator, new Formula[] { left, other }),
				new DisjunctionFormula(creator, new Formula[] { other, right }));

		DisjunctionFormula formula = DisjunctionFormula.disjunction(creator, Arrays.asList(left, right));
		assertThat(formula.getFormulas(), contains(equalTo(left), equalTo(right)));
	}

	@Test
	public void testDisjunctionHit() {
		FormulaCreator creator = mock(FormulaCreator.class);
		Formula left = mock(Formula.class);
		Formula right = mock(Formula.class);
		DisjunctionFormula expected = new DisjunctionFormula(creator, new Formula[] { left, right });
		stubCreator(creator, expected);

		assertThat(DisjunctionFormula.disjunction(creator, Arrays.asList(left, right)), sameInstance(expected));
	}

	@Test
	public void testFixedPointMiss() {
		FormulaCreator creator = mock(FormulaCreator.class);
		VariableFormula var = mock(VariableFormula.class);
		VariableFormula otherVar = mock(VariableFormula.class);
		Formula child = mock(Formula.class);
		Formula other = mock(Formula.class);
		stubCreator(creator, new FixedPointFormula(creator, FixedPoint.LEAST, var, child),
				new FixedPointFormula(creator, FixedPoint.GREATEST, otherVar, child),
				new FixedPointFormula(creator, FixedPoint.GREATEST, var, other));

		FixedPointFormula formula = FixedPointFormula.fixedPoint(creator, FixedPoint.GREATEST, var, child);
		assertThat(formula.getFixedPoint(), equalTo(FixedPoint.GREATEST));
		assertThat(formula.getVariable(), equalTo(var));
		assertThat(formula.getFormula(), equalTo(child));
	}

	@Test
	public void testFixedPointHit() {
		FormulaCreator creator = mock(FormulaCreator.class);
		VariableFormula var = mock(VariableFormula.class);
		Formula child = mock(Formula.class);
		FixedPointFormula expected = new FixedPointFormula(creator, FixedPoint.LEAST, var, child);
		stubCreator(creator, expected);

		assertThat(FixedPointFormula.fixedPoint(creator, FixedPoint.LEAST, var, child), sameInstance(expected));
	}

	@Test
	public void testModalityMiss() {
		FormulaCreator creator = mock(FormulaCreator.class);
		String event = "event";
		String otherEvent = "otherEvent";
		Formula child = mock(Formula.class);
		Formula other = mock(Formula.class);
		stubCreator(creator, new ModalityFormula(creator, Modality.UNIVERSAL, event, child),
				new ModalityFormula(creator, Modality.EXISTENTIAL, otherEvent, child),
				new ModalityFormula(creator, Modality.EXISTENTIAL, event, other));

		ModalityFormula formula = ModalityFormula.modality(creator, Modality.EXISTENTIAL, event, child);
		assertThat(formula.getModality(), equalTo(Modality.EXISTENTIAL));
		assertThat(formula.getEvent(), equalTo(event));
		assertThat(formula.getFormula(), equalTo(child));
	}

	@Test
	public void testModalityHit() {
		FormulaCreator creator = mock(FormulaCreator.class);
		String event = "event";
		Formula child = mock(Formula.class);
		ModalityFormula expected = new ModalityFormula(creator, Modality.UNIVERSAL, event, child);
		stubCreator(creator, expected);

		assertThat(ModalityFormula.modality(creator, Modality.UNIVERSAL, event, child), sameInstance(expected));
	}

	@Test
	public void testNegationMiss() {
		FormulaCreator creator = mock(FormulaCreator.class);
		Formula child = mock(Formula.class);
		Formula other = mock(Formula.class);
		stubCreator(creator, new NegationFormula(creator, other));

		NegationFormula formula = NegationFormula.negate(creator, child);
		assertThat(formula.getFormula(), equalTo(child));
	}

	@Test
	public void testNegationHit() {
		FormulaCreator creator = mock(FormulaCreator.class);
		Formula child = mock(Formula.class);
		NegationFormula expected = new NegationFormula(creator, child);
		stubCreator(creator, expected);

		assertThat(NegationFormula.negate(creator, child), sameInstance(expected));
	}

	@Test
	public void testVariableMiss() {
		FormulaCreator creator = mock(FormulaCreator.class);
		stubCreator(creator, new VariableFormula(creator, "foo"));

		VariableFormula formula = VariableFormula.variable(creator, "bar", false);
		assertThat(formula.getVariable(), equalTo("bar"));
	}

	@Test
	public void testVariableHit() {
		String a1 = new String(new char[] { 'a' });
		String a2 = new String(new char[] { 'a' });
		assertThat(a1, not(sameInstance(a2)));

		FormulaCreator creator = mock(FormulaCreator.class);
		VariableFormula expected = new VariableFormula(creator, a1);
		stubCreator(creator, expected);

		assertThat(VariableFormula.variable(creator, a2, false), sameInstance(expected));
	}

	@Test
	public void testVariableHitButIgnored() {
		String a1 = new String(new char[] { 'a' });
		String a2 = new String(new char[] { 'a' });
		assertThat(a1, not(sameInstance(a2)));

		FormulaCreator creator = mock(FormulaCreator.class);
		VariableFormula expected = new VariableFormula(creator, a1);
		stubCreator(creator, expected);

		assertThat(VariableFormula.variable(creator, a2, true), nullValue());
	}

	@Test
	public void testLetMiss() {
		FormulaCreator creator = mock(FormulaCreator.class);
		VariableFormula variable = mock(VariableFormula.class);
		VariableFormula otherVariable = mock(VariableFormula.class);
		Formula expansion = mock(Formula.class);
		Formula child = mock(Formula.class);
		Formula other = mock(Formula.class);
		stubCreator(creator, new LetFormula(creator, otherVariable, expansion, child),
				new LetFormula(creator, variable, other, child),
				new LetFormula(creator, variable, expansion, other));

		LetFormula formula = LetFormula.let(creator, variable, expansion, child);
		assertThat(formula.getVariable(), equalTo(variable));
		assertThat(formula.getExpansion(), equalTo(expansion));
		assertThat(formula.getFormula(), equalTo(child));
	}

	@Test
	public void testLetHit() {
		FormulaCreator creator = mock(FormulaCreator.class);
		VariableFormula variable = mock(VariableFormula.class);
		Formula expansion = mock(Formula.class);
		Formula child = mock(Formula.class);
		LetFormula expected = new LetFormula(creator, variable, expansion, child);
		stubCreator(creator, expected);

		assertThat(LetFormula.let(creator, variable, expansion, child), sameInstance(expected));
	}

	@Test
	public void testCallMiss() {
		FormulaCreator creator = mock(FormulaCreator.class);
		String function = "function";
		String otherFunction = "otherFunction";
		List<Formula> arguments = Arrays.asList(mock(Formula.class));
		List<Formula> otherArguments = Collections.emptyList();
		stubCreator(creator, new CallFormula(creator, otherFunction, arguments),
				new CallFormula(creator, function, otherArguments));

		CallFormula formula = CallFormula.call(creator, function, arguments);
		assertThat(formula.getFunction(), equalTo(function));
		assertThat(formula.getArguments(), equalTo(arguments));
	}

	@Test
	public void testCallHit() {
		FormulaCreator creator = mock(FormulaCreator.class);
		String function = "function";
		List<Formula> arguments = Collections.emptyList();
		CallFormula expected = new CallFormula(creator, function, arguments);
		stubCreator(creator, expected);

		assertThat(CallFormula.call(creator, function, arguments), sameInstance(expected));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
