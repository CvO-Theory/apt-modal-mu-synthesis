package uniol.synthesis.tableau;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

import uniol.apt.adt.ts.State;
import uniol.apt.util.Pair;

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
	private Map<Sequent, Tableau> tableauCache = new HashMap<>();

	public Tableau getTableau(State state, Formula formula) {
		Sequent sequent = new Sequent(state, formula, new DefinitionList());
		tableauCache.clear();
		new NonRecursive().run(new FillCache(sequent));
		return tableauCache.get(sequent);
	}

	private class FillCache extends FormulaWalker {
		private final Sequent sequent;

		public FillCache(Sequent sequent) {
			super(sequent.getFormula());
			this.sequent = sequent;
		}

		@Override
		public void walk(NonRecursive engine, ConstantFormula formula) {
			if (formula.getValue()) {
				tableauCache.put(sequent, new TrueTableau(sequent.getState(), formula));
			} else {
				tableauCache.put(sequent, new FalseTableau(sequent.getState(), formula));
			}
		}

		@Override
		public void walk(NonRecursive engine, ConjunctionFormula formula) {
			Sequent leftSequent = sequent.newWithFormula(formula.getLeft());
			Sequent rightSequent = sequent.newWithFormula(formula.getRight());
			Tableau left = tableauCache.get(leftSequent);
			Tableau right = tableauCache.get(rightSequent);
			if (left == null || right == null) {
				engine.enqueue(this);
				engine.enqueue(new FillCache(rightSequent));
				engine.enqueue(new FillCache(leftSequent));
				return;
			}
			tableauCache.put(sequent, new ConjunctionTableau(sequent.getState(), sequent.getFormula(), left, right));
		}

		@Override
		public void walk(NonRecursive engine, DisjunctionFormula formula) {
			Sequent leftSequent = sequent.newWithFormula(formula.getLeft());
			Sequent rightSequent = sequent.newWithFormula(formula.getRight());
			Tableau left = tableauCache.get(leftSequent);
			Tableau right = tableauCache.get(rightSequent);
			if (left == null || right == null) {
				engine.enqueue(this);
				engine.enqueue(new FillCache(rightSequent));
				engine.enqueue(new FillCache(leftSequent));
				return;
			}
			tableauCache.put(sequent, new DisjunctionTableau(sequent.getState(), sequent.getFormula(), left, right));
		}

		@Override
		public void walk(NonRecursive engine, NegationFormula formula) {
			throw new AssertionError("All negations should have been eliminated!");
		}

		@Override
		public void walk(NonRecursive engine, ModalityFormula formula) {
			Set<State> followingStates = sequent.getState().getPostsetNodesByLabel(formula.getEvent().getLabel());
			Set<Tableau> followingTableaus = new HashSet<>();
			boolean hadAll = true;
			for (State following : followingStates) {
				Sequent next = sequent.newWithFormula(following, formula.getFormula());
				Tableau tableau = tableauCache.get(next);
				if (tableau == null) {
					if (hadAll)
						engine.enqueue(this);
					engine.enqueue(new FillCache(next));
					hadAll = false;
				} else {
					followingTableaus.add(tableau);
				}
			}
			if (!hadAll)
				return;

			Tableau result;
			switch (formula.getModality()) {
				case UNIVERSAL:
					result = new UniversalTableau(sequent.getState(), formula, followingTableaus);
					break;
				case EXISTENTIAL:
					result = new ExistentialTableau(sequent.getState(), formula, followingTableaus);
					break;
				default:
					throw new AssertionError("Unknown modality");
			}
			tableauCache.put(sequent, result);
		}

		@Override
		public void walk(NonRecursive engine, VariableFormula formula) {
			FixedPointFormula fp = sequent.getDefinitionList().getFixedPointFormula(formula);
			if (fp == null)
				throw new AssertionError("There should be no free variables!");
			if (sequent.getDefinitionList().wasAlreadyExpanded(sequent.getState(), formula)) {
				// Was already expanded. We cannot continue here: get the result
				Tableau result;
				switch (fp.getFixedPoint()) {
					case LEAST:
						result = new FalseTableau(sequent.getState(), formula);
						break;
					case GREATEST:
						result = new TrueTableau(sequent.getState(), formula);
						break;
					default:
						throw new AssertionError("Unknown fixed point");
				}
				tableauCache.put(sequent, result);
				return;
			}
			Formula nextFormula = substitute(fp.getFormula(), fp.getVariable(), formula);
			DefinitionList dl = sequent.getDefinitionList().noteExpansion(sequent.getState(), formula);
			Sequent next = new Sequent(sequent.getState(), nextFormula, dl);

			Tableau result = tableauCache.get(next);
			if (result == null) {
				engine.enqueue(this);
				engine.enqueue(new FillCache(next));
				return;
			}
			tableauCache.put(sequent, new VariableExpansionTableau(sequent.getState(), formula, result));
		}

		@Override
		public void walk(NonRecursive engine, FixedPointFormula formula) {
			VariableFormula constant = formula.getCreator().freshVariable(formula.getVariable().getVariable());
			DefinitionList dl = sequent.getDefinitionList().expand(constant, formula);
			Sequent next = new Sequent(sequent.getState(), constant, dl);
			engine.enqueue(new FillCacheForFixedPoint(sequent, next));
			engine.enqueue(new FillCache(next));
		}
	}

	private final class FillCacheForFixedPoint implements NonRecursive.Walker {
		private final Sequent sequent;
		private final Sequent child;

		public FillCacheForFixedPoint(Sequent sequent, Sequent child) {
			this.sequent = sequent;
			this.child = child;
		}

		@Override
		public void walk(NonRecursive engine) {
			Tableau tableau = tableauCache.get(child);
			assert tableau != null;
			tableauCache.put(sequent, new FixedPointTableau(sequent.getState(), sequent.getFormula(), tableau));
		}
	}

	private class Sequent {
		private final State state;
		private final Formula formula;
		private final DefinitionList definitionList;

		public Sequent(State state, Formula formula, DefinitionList definitionList) {
			this.state = state;
			this.formula = formula;
			this.definitionList = definitionList;
		}

		public State getState() {
			return state;
		}

		public Formula getFormula() {
			return formula;
		}

		public DefinitionList getDefinitionList() {
			return definitionList;
		}

		public Sequent newWithFormula(Formula newFormula) {
			return newWithFormula(this.state, newFormula);
		}

		public Sequent newWithFormula(State newState, Formula newFormula) {
			return new Sequent(newState, newFormula, definitionList);
		}

		@Override
		public int hashCode() {
			return state.hashCode() ^ formula.hashCode() ^ definitionList.hashCode();
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Sequent))
				return false;
			Sequent other = (Sequent) o;
			return state.equals(other.state) && formula.equals(other.formula) && definitionList.equals(other.definitionList);
		}
	}
}
