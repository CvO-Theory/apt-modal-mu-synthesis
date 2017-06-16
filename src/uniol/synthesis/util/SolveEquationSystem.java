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

package uniol.synthesis.util;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
import static uniol.synthesis.util.GetFreeVariables.getFreeVariables;
import static uniol.synthesis.util.GetFreeVariables.getFreeVariablesCounts;
import static uniol.synthesis.util.SubstitutionTransformer.substitute;

public class SolveEquationSystem {
	public Map<VariableFormula, Formula> solve(FixedPoint fp, Map<VariableFormula, Formula> input) {
		Map<VariableFormula, Formula> result = new HashMap<>(input);
		Queue<VariableFormula> unhandled = new ArrayDeque<>(result.keySet());

		while (!unhandled.isEmpty()) {
			VariableFormula var = unhandled.remove();
			Formula definition = result.get(var);

			// If the variable appears as a free variable in its definition, introduce a fixed point to bind
			// the variable
			if (getFreeVariables(definition).contains(var))
				definition = definition.getCreator().fixedPoint(fp, var, definition);

			// Replace this variable's definition with what we just computed and substitute it into all
			// other variables.
			for (Map.Entry<VariableFormula, Formula> entry : result.entrySet()) {
				if (entry.getKey().equals(var)) {
					entry.setValue(definition);
				} else {
					Map<VariableFormula, Integer> counts = getFreeVariablesCounts(entry.getValue());
					Integer count = counts.get(var);
					if (count == null) {
						// Not a free variable, so no need to handle
					} else if (count == 1) {
						// Only appears once, so use a substitution
						entry.setValue(substitute(entry.getValue(), var, definition));
					} else {
						// Appears multiple times, so use a "let" expression
						assert count > 1;
						entry.setValue(var.getCreator().let(var, definition, entry.getValue()));
					}
				}
			}
		}
		return result;
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
