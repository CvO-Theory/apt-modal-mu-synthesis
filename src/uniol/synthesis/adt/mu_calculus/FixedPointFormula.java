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

public class FixedPointFormula extends AbstractFormula {
	final private FixedPoint fixedPoint;
	final private VariableFormula variable;
	final private Formula formula;

	protected FixedPointFormula(FormulaCreator creator, FixedPoint fixedPoint, VariableFormula variable, Formula formula) {
		super(creator);
		this.fixedPoint = fixedPoint;
		this.variable = variable;
		this.formula = formula;
	}

	public FixedPoint getFixedPoint() {
		return fixedPoint;
	}

	public VariableFormula getVariable() {
		return variable;
	}

	public Formula getFormula() {
		return formula;
	}

	static FixedPointFormula fixedPoint(FormulaCreator creator, FixedPoint fixedPoint, VariableFormula variable, Formula innerFormula) {
		int hashCode = fixedPoint.hashCode() ^ variable.hashCode() ^ innerFormula.hashCode();
		for (Formula formula : creator.getFormulasWithHashCode(hashCode)) {
			if (formula instanceof FixedPointFormula) {
				FixedPointFormula result = (FixedPointFormula) formula;
				if (result.getFixedPoint().equals(fixedPoint) && result.getVariable().equals(variable)
						&& result.getFormula().equals(innerFormula))
					return result;
			}
		}
		FixedPointFormula result = new FixedPointFormula(creator, fixedPoint, variable, innerFormula);
		creator.addFormulaInternal(hashCode, result);
		return result;
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
