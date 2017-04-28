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

public class ConjunctionFormula extends AbstractFormula {
	final private Formula left;
	final private Formula right;

	protected ConjunctionFormula(FormulaCreator creator, Formula left, Formula right) {
		super(creator);
		this.left = left;
		this.right = right;
	}

	public Formula getRight() {
		return right;
	}

	public Formula getLeft() {
		return left;
	}

	static ConjunctionFormula conjunction(FormulaCreator creator, Formula left, Formula right) {
		int hashCode = left.hashCode() ^ Integer.rotateLeft(right.hashCode(), 16);
		for (Formula formula : creator.getFormulasWithHashCode(hashCode)) {
			if (formula instanceof ConjunctionFormula) {
				ConjunctionFormula result = (ConjunctionFormula) formula;
				if (result.getLeft().equals(left) && result.getRight().equals(right))
					return result;
			}
		}
		ConjunctionFormula result = new ConjunctionFormula(creator, left, right);
		creator.addFormulaInternal(hashCode, result);
		return result;
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
