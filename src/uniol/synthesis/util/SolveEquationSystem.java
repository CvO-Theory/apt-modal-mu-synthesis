package uniol.synthesis.util;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

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
					entry.setValue(substitute(entry.getValue(), var, definition));
				}
			}
		}
		return result;
	}
}
