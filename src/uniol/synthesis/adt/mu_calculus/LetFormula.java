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

public class LetFormula extends AbstractFormula {
	final private VariableFormula variable;
	final private Formula expansion;
	final private Formula formula;

	protected LetFormula(FormulaCreator creator, VariableFormula variable, Formula expansion, Formula formula) {
		super(creator);
		this.variable = variable;
		this.expansion = expansion;
		this.formula = formula;
	}

	public VariableFormula getVariable() {
		return variable;
	}

	public Formula getExpansion() {
		return expansion;
	}

	public Formula getFormula() {
		return formula;
	}

	static LetFormula let(FormulaCreator creator, VariableFormula variable, Formula expansion,
			Formula innerFormula) {
		int hashCode = variable.hashCode() ^ expansion.hashCode() ^ innerFormula.hashCode();
		for (Formula formula : creator.getFormulasWithHashCode(hashCode)) {
			if (formula instanceof LetFormula) {
				LetFormula result = (LetFormula) formula;
				if (result.getVariable().equals(variable) && result.getExpansion().equals(expansion)
						&& result.getFormula().equals(innerFormula))
					return result;
			}
		}
		LetFormula result = new LetFormula(creator, variable, expansion, innerFormula);
		creator.addFormulaInternal(hashCode, result);
		return result;
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
