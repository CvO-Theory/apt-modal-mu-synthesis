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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DisjunctionFormula extends AbstractFormula {
	final private Formula[] formulas;

	protected DisjunctionFormula(FormulaCreator creator, Formula[] formulas) {
		super(creator);
		this.formulas = formulas;
	}

	public List<Formula> getFormulas() {
		return Collections.unmodifiableList(Arrays.asList(formulas));
	}

	@Deprecated
	public Formula getRight() {
		if (formulas.length != 2)
			throw new RuntimeException();
		return formulas[1];
	}

	@Deprecated
	public Formula getLeft() {
		if (formulas.length != 2)
			throw new RuntimeException();
		return formulas[0];
	}

	static DisjunctionFormula disjunction(FormulaCreator creator, List<Formula> children) {
		// TODO XXX: 'Flatten' disjunctions of disjunctions
		int hashCode = ~children.hashCode();
		for (Formula formula : creator.getFormulasWithHashCode(hashCode)) {
			if (formula instanceof DisjunctionFormula) {
				DisjunctionFormula result = (DisjunctionFormula) formula;
				if (result.getFormulas().equals(children))
					return result;
			}
		}
		DisjunctionFormula result = new DisjunctionFormula(creator, children.toArray(new Formula[0]));
		creator.addFormulaInternal(hashCode, result);
		return result;
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
