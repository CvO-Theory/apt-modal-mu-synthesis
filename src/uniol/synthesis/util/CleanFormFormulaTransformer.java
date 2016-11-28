package uniol.synthesis.util;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.ConjunctionFormula;
import uniol.synthesis.adt.mu_calculus.DisjunctionFormula;
import uniol.synthesis.adt.mu_calculus.NegationFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;

public class CleanFormFormulaTransformer extends FormulaTransformer {
	final private Map<VariableFormula, VariableFormula> variableReplacements = new HashMap<>();
	final private Map<VariableFormula, VariableFormula> oldVariableReplacements = new HashMap<>();

	private CleanFormFormulaTransformer(Set<VariableFormula> freeVariables) {
		for (VariableFormula var : freeVariables)
			variableReplacements.put(var, var);
	}

	@Override
	public void enter(NonRecursive engine, FixedPointFormula formula) {
		super.enter(engine, formula);

		VariableFormula variable = formula.getVariable();
		VariableFormula replacement;
		if (variableReplacements.containsKey(variable)) {
			replacement = formula.getCreator().freshVariable(variable.getVariable());
		} else {
			// Variable is new, it does not need to be replaced
			replacement = variable;
		}
		VariableFormula old = variableReplacements.put(variable, replacement);
		oldVariableReplacements.put(replacement, old);
	}

	@Override
	public void exit(NonRecursive engine, FixedPointFormula formula) {
		super.exit(engine, formula);
		VariableFormula replacement = variableReplacements.get(formula.getVariable());
		assert replacement != null;
		VariableFormula old = oldVariableReplacements.remove(replacement);
		if (old != null)
			variableReplacements.put(formula.getVariable(), old);
		else
			variableReplacements.remove(formula.getVariable());
	}

	@Override
	protected Formula transform(VariableFormula formula) {
		VariableFormula replacement = variableReplacements.get(formula);
		if (replacement != null)
			return replacement;
		return formula;
	}

	@Override
	protected Formula transform(FixedPointFormula formula) {
		VariableFormula replacement = variableReplacements.get(formula.getVariable());
		if (replacement != null) {
			return formula.getCreator().fixedPoint(formula.getFixedPoint(), replacement, formula.getFormula());
		}
		return formula;
	}

	static public Formula cleanForm(Formula formula) {
		Set<VariableFormula> freeVariables = GetFreeVariables.getFreeVariables(formula);
		return new CleanFormFormulaTransformer(freeVariables).transform(formula);
	}
}
