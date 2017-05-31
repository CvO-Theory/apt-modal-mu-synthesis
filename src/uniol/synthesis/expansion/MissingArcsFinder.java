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

package uniol.synthesis.expansion;

import java.util.HashSet;
import java.util.Set;

import uniol.apt.adt.ts.State;
import uniol.apt.util.Pair;

import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.tableau.Tableau;
import uniol.synthesis.tableau.TableauNode;

public class MissingArcsFinder {
	public Set<Pair<State, String>> findMissing(Tableau tableau) {
		Set<Pair<State, String>> result = new HashSet<>();

		for (TableauNode<State> node : tableau.getLeaves()) {
			Formula formula = node.getFormula();
			if (!(formula instanceof ModalityFormula))
				continue;

			ModalityFormula modality = (ModalityFormula) formula;
			if (modality.getModality().equals(Modality.UNIVERSAL))
				continue;

			Pair<State, String> pair = new Pair<>(node.getState(), modality.getEvent());
			result.add(pair);
		}

		return result;
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
