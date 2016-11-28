package uniol.synthesis.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.HashBag;

import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.ConjunctionFormula;
import uniol.synthesis.adt.mu_calculus.DisjunctionFormula;
import uniol.synthesis.adt.mu_calculus.NegationFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;

public class GetFreeVariables extends RecursiveFormulaWalker {
	final private Set<VariableFormula> freeVariables = new HashSet<>();
	final private Bag<VariableFormula> currentlyBoundVariables = new HashBag<>();

	private GetFreeVariables() {
	}

	public Set<VariableFormula> getFreeVariables() {
		return Collections.unmodifiableSet(freeVariables);
	}

	@Override
	protected void visit(NonRecursive engine, VariableFormula formula) {
		if (!currentlyBoundVariables.contains(formula))
			freeVariables.add(formula);
	}

	@Override
	protected void enter(NonRecursive engine, FixedPointFormula formula) {
		currentlyBoundVariables.add(formula.getVariable());
	}

	@Override
	protected void exit(NonRecursive engine, FixedPointFormula formula) {
		boolean changed = currentlyBoundVariables.remove(formula.getVariable(), 1);
		assert changed;
	}

	static public Set<VariableFormula> getFreeVariables(Formula formula) {
		GetFreeVariables gfv = new GetFreeVariables();
		gfv.walk(formula);
		return gfv.getFreeVariables();
	}
}
