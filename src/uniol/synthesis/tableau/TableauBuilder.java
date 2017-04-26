package uniol.synthesis.tableau;

import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

import uniol.apt.adt.ts.State;

import uniol.synthesis.adt.mu_calculus.ConjunctionFormula;
import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.DisjunctionFormula;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.NegationFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
import uniol.synthesis.util.FormulaWalker;
import uniol.synthesis.util.NonRecursive;
import static uniol.synthesis.util.SubstitutionTransformer.substitute;

public class TableauBuilder {
	static Set<Set<TableauNode>> expandNode(TableauNode node) {
		ExpandNodeWalker walker = new ExpandNodeWalker(node);
		walker.walk(null);
		return walker.getExpansion();
	}

	static class ExpandNodeWalker extends FormulaWalker {
		private final TableauNode node;
		private Set<Set<TableauNode>> expansion;

		private ExpandNodeWalker(TableauNode node) {
			super(node.getFormula());
			this.node = node;
		}

		Set<Set<TableauNode>> getExpansion() {
			return expansion;
		}

		@Override
		public void walk(NonRecursive engine, ConstantFormula formula) {
			if (formula.getValue())
				expansion = Collections.singleton(Collections.<TableauNode>emptySet());
			else
				expansion = null;
		}

		@Override
		public void walk(NonRecursive engine, ConjunctionFormula formula) {
			Set<TableauNode> set = new HashSet<>();
			set.add(node.createChild(formula.getLeft()));
			set.add(node.createChild(formula.getRight()));
			expansion = Collections.singleton(set);
		}

		@Override
		public void walk(NonRecursive engine, DisjunctionFormula formula) {
			expansion = new HashSet<>();
			expansion.add(Collections.singleton(node.createChild(formula.getLeft())));
			expansion.add(Collections.singleton(node.createChild(formula.getRight())));
		}

		@Override
		public void walk(NonRecursive engine, NegationFormula formula) {
			throw new IllegalArgumentException("No negation should be present in the formula, but got: "
					+ formula.toString());
		}

		@Override
		public void walk(NonRecursive engine, VariableFormula formula) {
			FixedPointFormula definition = node.getDefinition(formula);
			if (definition == null)
				throw new IllegalArgumentException("No free variables should be present, but got: "
						+ formula.toString());
			if (node.wasAlreadyExpanded()) {
				// We already expanded this fixed point in this state. Thus, we can now use this to
				// derive the result.
				switch (definition.getFixedPoint()) {
					case LEAST:
						expansion = null;
						break;
					case GREATEST:
						expansion = Collections.singleton(Collections.<TableauNode>emptySet());
						break;
				}
			} else {
				Formula inner = substitute(definition.getFormula(), definition.getVariable(), formula);
				expansion = Collections.singleton(Collections.singleton(node.recordExpansion(formula, inner)));
			}
		}

		@Override
		public void walk(NonRecursive engine, ModalityFormula formula) {
			String event = formula.getEvent().getLabel();
			Set<State> states = node.getState().getPostsetNodesByLabel(event);
			if (states.isEmpty()) {
				expansion = Collections.singleton(Collections.<TableauNode>emptySet());
			} else {
				if (states.size() != 1)
					throw new IllegalArgumentException("Given LTS is non-deterministic in state " +
							states + " with label " + event);
				State target = states.iterator().next();
				expansion = Collections.singleton(Collections.singleton(
							node.createChild(target, formula.getFormula())));
			}
		}

		@Override
		public void walk(NonRecursive engine, FixedPointFormula formula) {
			VariableFormula freshVariable = formula.getCreator().freshVariable(formula.getVariable().getVariable());
			expansion = Collections.singleton(Collections.singleton(node.addExpansion(freshVariable, formula)));
		}
	}
}
