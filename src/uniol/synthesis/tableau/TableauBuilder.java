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

package uniol.synthesis.tableau;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import uniol.synthesis.adt.mu_calculus.ConjunctionFormula;
import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.DisjunctionFormula;
import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.NegationFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
import uniol.synthesis.util.FormulaWalker;
import uniol.synthesis.util.NonRecursive;
import static uniol.synthesis.util.CleanFormFormulaTransformer.cleanForm;
import static uniol.synthesis.util.PositiveFormFormulaTransformer.positiveForm;
import static uniol.synthesis.util.SubstitutionTransformer.substitute;

public class TableauBuilder<S> {
	public interface ProgressCallback<S> {
		public void children(TableauNode<S> node, Set<Set<TableauNode<S>>> children);
	}

	static private final ProgressCallback<Object> NOP_PROGRESS = new ProgressCallback<Object>() {
		@Override
		public void children(TableauNode<Object> node, Set<Set<TableauNode<Object>>> children) {
		}
	};

	static private <S> ProgressCallback<S> nopProgressCallback() {
		@SuppressWarnings("unchecked")
		ProgressCallback<S> result = (ProgressCallback<S>) NOP_PROGRESS;
		return result;
	}

	private final FollowArcs<S> followArcs;
	private final ProgressCallback<S> callback;

	public TableauBuilder(FollowArcs<S> followArcs) {
		this(followArcs, TableauBuilder.<S>nopProgressCallback());
	}

	public TableauBuilder(FollowArcs<S> followArcs, ProgressCallback<S> callback) {
		this.followArcs = followArcs;
		this.callback = callback;
	}

	public Set<Tableau<S>> createTableaus(S state, Formula formula) {
		formula = cleanForm(positiveForm(formula));
		return expandTableau(Collections.singleton(new TableauNode<S>(followArcs, state, formula)));
	}

	public Set<Tableau<S>> continueTableau(Tableau<S> tableau) {
		return expandTableau(tableau.getLeaves());
	}

	private Set<Tableau<S>> expandTableau(Set<TableauNode<S>> nodes) {
		Set<Tableau<S>> result = new HashSet<>();
		new NonRecursive().run(new CreateTableaus<S>(callback, result, nodes));
		return result;
	}

	static private class CreateTableaus<S> implements NonRecursive.Walker {
		private final ProgressCallback<S> callback;
		private final Set<Tableau<S>> result;
		private final Set<TableauNode<S>> leaves = new HashSet<>();
		private final Queue<ExpandNodeWalker<S>> todo = new ArrayDeque<>();

		private CreateTableaus(ProgressCallback<S> callback, Set<Tableau<S>> result, Set<TableauNode<S>> nodes) {
			this.callback = callback;
			this.result = result;
			for (TableauNode<S> node : nodes)
				this.todo.add(new ExpandNodeWalker<S>(node));
		}

		private CreateTableaus(CreateTableaus<S> toCopy) {
			this.callback = toCopy.callback;
			this.result = toCopy.result;
			this.leaves.addAll(toCopy.leaves);
			this.todo.addAll(toCopy.todo);
		}

		@Override
		public void walk(NonRecursive engine) {
			ExpandNodeWalker<S> next = todo.poll();
			if (next == null) {
				// We are done creating a tableau
				result.add(new Tableau<S>(leaves));
				return;
			}

			next.walk(engine);
			Set<Set<TableauNode<S>>> expansion = next.getExpansion();
			callback.children(next.getNode(), expansion);
			if (expansion == null)
				// This is false / does not hold.
				return;

			if (expansion.size() == 1) {
				// A conjunction of nodes. Follow them.
				Set<TableauNode<S>> children = expansion.iterator().next();
				if (children.isEmpty()) {
					// No children, thus this is a leave
					leaves.add(next.getNode());
				} else {
					for (TableauNode<S> child : children) {
						todo.add(new ExpandNodeWalker<S>(child));
					}
				}
				// Continue handling the children
				engine.enqueue(this);
			} else {
				// A disjunction of nodes, we have to split
				for (Set<TableauNode<S>> part : expansion) {
					CreateTableaus<S> split = new CreateTableaus<S>(this);
					for (TableauNode<S> child : part) {
						split.todo.add(new ExpandNodeWalker<S>(child));
					}
					engine.enqueue(split);
				}
			}
		}
	}

	static <S> Set<Set<TableauNode<S>>> expandNode(TableauNode<S> node) {
		ExpandNodeWalker<S> walker = new ExpandNodeWalker<S>(node);
		walker.walk(null);
		return walker.getExpansion();
	}

	static private class ExpandNodeWalker<S> extends FormulaWalker {
		private final TableauNode<S> node;
		private Set<Set<TableauNode<S>>> expansion;

		private ExpandNodeWalker(TableauNode<S> node) {
			super(node.getFormula());
			this.node = node;
		}

		private TableauNode<S> getNode() {
			return node;
		}

		Set<Set<TableauNode<S>>> getExpansion() {
			return expansion;
		}

		@Override
		public void walk(NonRecursive engine, ConstantFormula formula) {
			if (formula.getValue())
				expansion = Collections.singleton(Collections.<TableauNode<S>>emptySet());
			else
				expansion = null;
		}

		@Override
		public void walk(NonRecursive engine, ConjunctionFormula formula) {
			Set<TableauNode<S>> set = new HashSet<>();
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
				// derive the result. For LEAST, we have a failure and we have nothing to do since
				// this.expansion is already null. Thus, only GREATEST needs to be handled.
				if (FixedPoint.GREATEST.equals(definition.getFixedPoint())) {
					expansion = Collections.singleton(Collections.singleton(
								node.createChild(definition.getCreator()
									.constant(true))));
				}
			} else {
				Formula inner = substitute(definition.getFormula(), definition.getVariable(), formula);
				expansion = Collections.singleton(Collections.singleton(node.recordExpansion(formula, inner)));
			}
		}

		@Override
		public void walk(NonRecursive engine, ModalityFormula formula) {
			String event = formula.getEvent();
			Set<S> states = node.getFollowArcs().followArcs(node.getState(), event);
			if (states.isEmpty()) {
				expansion = Collections.singleton(Collections.<TableauNode<S>>emptySet());
			} else {
				if (states.size() != 1)
					throw new IllegalArgumentException("Given LTS is non-deterministic in state " +
							node.getState() + " with label " + event);
				S target = states.iterator().next();
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

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
