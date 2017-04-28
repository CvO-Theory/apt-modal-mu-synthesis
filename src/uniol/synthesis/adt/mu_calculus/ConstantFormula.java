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

public class ConstantFormula extends AbstractFormula {
	private final boolean value;

	protected ConstantFormula(FormulaCreator creator, boolean value) {
		super(creator);
		this.value = value;
	}

	public boolean getValue() {
		return value;
	}

	static ConstantFormula constant(FormulaCreator creator, boolean value) {
		int hashCode = Boolean.valueOf(value).hashCode();
		for (Formula formula : creator.getFormulasWithHashCode(hashCode)) {
			if (formula instanceof ConstantFormula) {
				ConstantFormula result = (ConstantFormula) formula;
				if (result.getValue() == value)
					return result;
			}
		}
		ConstantFormula result = new ConstantFormula(creator, value);
		creator.addFormulaInternal(hashCode, result);
		return result;
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
