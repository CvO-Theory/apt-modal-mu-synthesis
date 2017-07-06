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
import java.util.Collections;
import java.util.List;

public class CallFormula extends AbstractFormula {
	final private String function;
	final private List<Formula> arguments;

	protected CallFormula(FormulaCreator creator, String function, List<Formula> arguments) {
		super(creator);
		this.function = function;
		this.arguments = Collections.unmodifiableList(new ArrayList<Formula>(arguments));
	}

	public String getFunction() {
		return function;
	}

	public List<Formula> getArguments() {
		return arguments;
	}

	static CallFormula call(FormulaCreator creator, String function, List<Formula> arguments) {
		int hashCode = function.hashCode() ^ arguments.hashCode();
		for (Formula formula : creator.getFormulasWithHashCode(hashCode)) {
			if (formula instanceof CallFormula) {
				CallFormula result = (CallFormula) formula;
				if (result.getFunction().equals(function) && result.getArguments().equals(arguments))
					return result;
			}
		}
		CallFormula result = new CallFormula(creator, function, arguments);
		creator.addFormulaInternal(hashCode, result);
		return result;
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
