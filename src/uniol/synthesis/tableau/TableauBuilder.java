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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import uniol.synthesis.adt.mu_calculus.ConjunctionFormula;
import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.DisjunctionFormula;
import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.LetFormula;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.NegationFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;
import uniol.synthesis.util.FormulaWalker;
import uniol.synthesis.util.NonRecursive;
import static uniol.synthesis.util.PositiveFormFormulaTransformer.positiveForm;
import static uniol.synthesis.util.SubstitutionTransformer.substitute;
import static uniol.synthesis.util.UnLetTransformer.unLet;

public class TableauBuilder<S> {
	public interface ProgressCallback<S> {
		public void children(TableauNode<S> node, Collection<? extends Collection<TableauNode<S>>> children);
	}

	static private final ProgressCallback<Object> NOP_PROGRESS = new ProgressCallback<Object>() {
		@Override
		public void children(TableauNode<Object> node, Collection<? extends Collection<TableauNode<Object>>> children) {
		}
	};

	static private <S> ProgressCallback<S> nopProgressCallback() {
		@SuppressWarnings("unchecked")
		ProgressCallback<S> result = (ProgressCallback<S>) NOP_PROGRESS;
		return result;
	}

	public interface ResultCallback<S> {
		public void foundTableau(Tableau<S> tableau);
	}

	static public enum TableauSelection {
		ALL {
			@Override
			boolean accept(TableauNode<?> node) {
				return true;
			}
		},
		SUCCESSFUL {
			@Override
			boolean accept(TableauNode<?> node) {
				return node.isSuccessful();
			}
		};

		abstract boolean accept(TableauNode<?> node);
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

	public void createTableaus(NonRecursive engine, ResultCallback<S> resultCallback, S state, Formula formula,
			TableauSelection selection) {
		formula = positiveForm(unLet(formula));
		expandTableau(engine, resultCallback, Collections.singleton(
					new TableauNode<S>(followArcs, state, formula)), selection);
	}

	public void continueTableau(NonRecursive engine, ResultCallback<S> resultCallback, Tableau<S> tableau, TableauSelection selection) {
		expandTableau(engine, resultCallback, tableau.getLeaves(), selection);
	}

	private void expandTableau(NonRecursive engine, ResultCallback<S> resultCallback,
			Collection<TableauNode<S>> nodes, TableauSelection selection) {
		engine.enqueue(new CreateTableaus<S>(callback, resultCallback, nodes, selection));
	}

	static private class CreateTableaus<S> implements NonRecursive.Walker {
		private final ProgressCallback<S> callback;
		private final ResultCallback<S> resultCallback;
		private final Collection<TableauNode<S>> leaves = new ArrayList<>();
		private final Deque<ExpandNodeWalker<S>> todo = new ArrayDeque<>();
		private final TableauSelection selection;

		private CreateTableaus(ProgressCallback<S> callback, ResultCallback<S> resultCallback,
				Collection<TableauNode<S>> nodes, TableauSelection selection) {
			this.callback = callback;
			this.resultCallback = resultCallback;
			this.selection = selection;
			for (TableauNode<S> node : nodes)
				addToTodo(this, node);
		}

		private CreateTableaus(CreateTableaus<S> toCopy) {
			this.callback = toCopy.callback;
			this.resultCallback = toCopy.resultCallback;
			this.leaves.addAll(toCopy.leaves);
			this.todo.addAll(toCopy.todo);
			this.selection = toCopy.selection;
		}

		@Override
		public void walk(NonRecursive engine) {
			ExpandNodeWalker<S> next = todo.poll();
			if (next == null) {
				// We are done creating a tableau
				resultCallback.foundTableau(new Tableau<S>(leaves));
				return;
			}

			next.walk(engine);
			Collection<? extends Collection<TableauNode<S>>> expansion = next.getExpansion();
			callback.children(next.getNode(), expansion);
			if (expansion == null)
				// This is false / does not hold.
				return;

			if (expansion.size() == 1) {
				// A conjunction of nodes. Follow them.
				Collection<TableauNode<S>> children = expansion.iterator().next();
				if (children.isEmpty()) {
					// No children, thus this is a leave
					TableauNode<S> node = next.getNode();
					if (selection.accept(node))
						leaves.add(node);
					else
						// Abort this branch of the tableau
						return;
				} else {
					for (TableauNode<S> child : children) {
						addToTodo(this, child);
					}
				}
				// Continue handling the children
				engine.enqueue(this);
			} else {
				// A disjunction of nodes, we have to split
				for (Collection<TableauNode<S>> part : expansion) {
					CreateTableaus<S> split = new CreateTableaus<S>(this);
					for (TableauNode<S> child : part) {
						addToTodo(split, child);
					}
					engine.enqueue(split);
				}
			}
		}

		static private <S> void addToTodo(CreateTableaus<S> creator, TableauNode<S> child) {
			if (child.getFormula() instanceof DisjunctionFormula) {
				creator.todo.addLast(new ExpandNodeWalker<S>(child));
			} else {
				creator.todo.addFirst(new ExpandNodeWalker<S>(child));
			}
		}
	}

	static <S> Collection<? extends Collection<TableauNode<S>>> expandNode(TableauNode<S> node) {
		ExpandNodeWalker<S> walker = new ExpandNodeWalker<S>(node);
		walker.walk(null);
		return walker.getExpansion();
	}

	static private class ExpandNodeWalker<S> extends FormulaWalker {
		private final TableauNode<S> node;
		private Collection<? extends Collection<TableauNode<S>>> expansion;

		private ExpandNodeWalker(TableauNode<S> node) {
			super(node.getFormula());
			this.node = node;
		}

		private TableauNode<S> getNode() {
			return node;
		}

		Collection<? extends Collection<TableauNode<S>>> getExpansion() {
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
			TableauNode<S> left = node.createChild(formula.getLeft());
			TableauNode<S> right = node.createChild(formula.getRight());
			expansion = Collections.singleton(Arrays.asList(left, right));
		}

		@Override
		public void walk(NonRecursive engine, DisjunctionFormula formula) {
			Collection<TableauNode<S>> left = Collections.singleton(node.createChild(formula.getLeft()));
			Collection<TableauNode<S>> right = Collections.singleton(node.createChild(formula.getRight()));
			expansion = Arrays.asList(left, right);
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

		@Override
		public void walk(NonRecursive engine, LetFormula formula) {
			Formula result = substitute(formula.getFormula(), formula.getVariable(), formula.getExpansion());
			expansion = Collections.singleton(Collections.singleton(node.createChild(result)));
		}
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
