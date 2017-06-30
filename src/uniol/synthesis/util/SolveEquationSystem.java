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
import java.util.Set;

import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
import static uniol.synthesis.util.GetFreeVariables.getFreeVariables;
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
					if (entry.getValue().equals(var)) {
						entry.setValue(definition);
					} else if (getFreeVariables(entry.getValue()).contains(var)) {
						entry.setValue(var.getCreator().let(var, definition, entry.getValue()));
					}
				}
			}
		}
		return result;
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
