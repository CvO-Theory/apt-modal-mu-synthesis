package uniol.synthesis.util;

import java.util.Map;
import java.util.HashMap;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;

import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class SubstitutionTransformer extends FormulaTransformer {
	private final Bag<VariableFormula> currentlyBoundVariables = new HashBag<>();
	private final Map<VariableFormula, Formula> substitution;

	public SubstitutionTransformer(Map<VariableFormula, Formula> substitution) {
		this.substitution = substitution;
	}

	@Override
	public void reset() {
		currentlyBoundVariables.clear();
	}

	protected Formula transform(VariableFormula formula) {
		if (currentlyBoundVariables.contains(formula))
			return formula;
		Formula transformation = substitution.get(formula);
		if (transformation != null)
			return transformation;
		return formula;
	}

	@Override
	public void enter(NonRecursive engine, FixedPointFormula formula) {
		super.enter(engine, formula);
		currentlyBoundVariables.add(formula.getVariable());
	}

	@Override
	public void exit(NonRecursive engine, FixedPointFormula formula) {
		super.exit(engine, formula);
		boolean changed = currentlyBoundVariables.remove(formula.getVariable(), 1);
		assert changed;
	}

	static public Formula substitute(Formula formula, Map<VariableFormula, Formula> substitution) {
		return new SubstitutionTransformer(substitution).transform(formula);
	}

	static public Formula substitute(Formula formula, VariableFormula variable, Formula substitution) {
		Map<VariableFormula, Formula> sub = new HashMap<>();
		sub.put(variable, substitution);
		return substitute(formula, sub);
	}
}
