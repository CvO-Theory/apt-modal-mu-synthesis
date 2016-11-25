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
	final private Set<VariableFormula> usedVariables = new HashSet<>();

	public CleanFormFormulaTransformer(Set<VariableFormula> freeVariables, Formula formula) {
		super(formula);
		usedVariables.addAll(freeVariables);
	}

	@Override
	public void enter(NonRecursive engine, FixedPointFormula formula) {
		super.enter(engine, formula);

		VariableFormula variable = formula.getVariable();
		if (usedVariables.add(variable))
			// Variable was new, it does not need to be replaced
			return;

		VariableFormula replacement = formula.getCreator().freshVariable(variable.getVariable());
		boolean changed = usedVariables.add(replacement);
		assert changed;
		VariableFormula old = variableReplacements.put(variable, replacement);
		if (old != null)
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
		GetFreeVariables gfv = new GetFreeVariables(formula);
		new NonRecursive().run(gfv);
		CleanFormFormulaTransformer clean = new CleanFormFormulaTransformer(gfv.getFreeVariables(), formula);
		new NonRecursive().run(clean);
		return clean.getTransformedFormula();
	}
}
