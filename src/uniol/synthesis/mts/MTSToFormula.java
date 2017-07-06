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

package uniol.synthesis.mts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.ts.Arc;
import uniol.apt.adt.ts.State;
import uniol.apt.adt.ts.TransitionSystem;

import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
import uniol.synthesis.util.SolveEquationSystem;

public class MTSToFormula {
	static public enum Mode {
		GENERIC, DETERMINISTIC;
	};

	private final Mode mode;

	public MTSToFormula(Mode mode) {
		this.mode = mode;
	}

	protected VariableFormula getVariable(FormulaCreator creator, State state) {
		return creator.variable(state.getId());
	}

	protected Collection<Arc> filterPureMayArcs(Collection<Arc> arcs) {
		Collection<Arc> result = new ArrayList<>(arcs.size());
		for (Arc arc : arcs)
			if (arc.hasExtension("may"))
				result.add(arc);
		return result;
	}

	protected Collection<Arc> filterMustArcs(Collection<Arc> arcs) {
		Collection<Arc> result = new ArrayList<>(arcs.size());
		for (Arc arc : arcs)
			if (!arc.hasExtension("may"))
				result.add(arc);
		return result;
	}

	protected Formula conjunction(Formula a, Formula b) {
		Formula True = a.getCreator().constant(true);
		if (a.equals(True))
			return b;
		if (b.equals(True))
			return a;
		return a.getCreator().conjunction(a, b);
	}

	protected Formula disjunction(Formula a, Formula b) {
		Formula False = a.getCreator().constant(false);
		if (a.equals(False))
			return b;
		if (b.equals(False))
			return a;
		return a.getCreator().disjunction(a, b);
	}

	protected Formula stateToFormula(FormulaCreator creator, State state) {
		Formula result = creator.constant(true);
		for (String event : state.getGraph().getAlphabet()) {
			Collection<Arc> mayArcs = state.getPostsetEdgesByLabel(event);
			Collection<Arc> mustArcs = filterMustArcs(mayArcs);
			if (mode.equals(Mode.DETERMINISTIC))
				// If for a given label there is at most one arc with that label, the existential
				// modality already makes sure that the target state of the arc behaves correctly.
				mayArcs = filterPureMayArcs(mayArcs);

			// Each must arc must have an implementation, so we have a conjunction of existential modalities
			Formula mustFormula = creator.constant(true);
			for (Arc arc : mustArcs) {
				VariableFormula target = getVariable(creator, arc.getTarget());
				Formula part = creator.modality(Modality.EXISTENTIAL, event, target);
				mustFormula = conjunction(mustFormula, part);
			}

			if (mustArcs.isEmpty() || mode.equals(Mode.GENERIC)) {
				// Each arc must be allowed by a may arc, so we have a disjunction inside a universal modality.
				// If there are must arcs in a deterministic system, they already make sure that one of
				// the valid targets is reached and this code would not allow more.
				Formula mayFormula = creator.constant(false);
				for (Arc arc : mayArcs) {
					Formula part = getVariable(creator, arc.getTarget());
					mayFormula = disjunction(mayFormula, part);
				}
				mayFormula = creator.modality(Modality.UNIVERSAL, event, mayFormula);

				Formula formula = conjunction(mustFormula, mayFormula);
				result = conjunction(result, formula);
			} else
				result = conjunction(result, mustFormula);
		}
		return result;
	}

	protected Map<VariableFormula, Formula> mtsToEquationSystem(FormulaCreator creator, TransitionSystem mts) {
		Map<VariableFormula, Formula> result = new HashMap<>();
		for (State state : mts.getNodes()) {
			result.put(getVariable(creator, state), stateToFormula(creator, state));
		}
		return result;
	}

	public Formula mtsToFormula(FormulaCreator creator, TransitionSystem mts) {
		return new SolveEquationSystem().solve(FixedPoint.GREATEST, mtsToEquationSystem(creator, mts))
			.get(getVariable(creator, mts.getInitialState()));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
