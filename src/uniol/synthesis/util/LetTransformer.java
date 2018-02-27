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

package uniol.synthesis.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import uniol.synthesis.adt.mu_calculus.ConjunctionFormula;
import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.DisjunctionFormula;
import uniol.synthesis.adt.mu_calculus.FixedPoint;
import uniol.synthesis.adt.mu_calculus.FixedPointFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.adt.mu_calculus.LetFormula;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;
import uniol.synthesis.adt.mu_calculus.NegationFormula;
import uniol.synthesis.adt.mu_calculus.VariableFormula;

public class LetTransformer implements NonRecursive.Walker {
	private final Map<Formula, FormulaInfo> formulaInfo = new HashMap<>();
	private final Deque<Formula> results = new ArrayDeque<>();
	private final Formula rootFormula;
	private final FormulaCreator creator;
	private int topoSortNum = 0;

	public LetTransformer(Formula formula) {
		formula = UnLetTransformer.unLet(formula);
		this.rootFormula = formula;
		this.creator = formula.getCreator();
	}

	public Formula getResult() {
		assert results.size() == 1 : results;
		return results.getLast();
	}

	@Override
	public void walk(NonRecursive engine) {
		assert results.isEmpty() : results;

		engine.enqueue(new StepTwo(rootFormula));

		// First, assign FormulaInfos to all sub-formulas by recursively descending. This also builds up a list
		// of parents of each formula (FormulaInfo.parents respectively FormulaInfo.descendants).
		// Also, this does a topological sort of formulas (FormulaInfo.topoSort).
		engine.enqueue(new AssignInfo(rootFormula));
	}

	final private class StepTwo implements NonRecursive.Walker {
		final private Formula formula;

		private StepTwo(Formula formula) {
			this.formula = formula;
		}

		@Override
		public void walk(NonRecursive engine) {
			FormulaInfo info = formulaInfo.get(formula);

			// After analysis is done, build the formula. For this, we just recursively re-create the
			// formula, but replace formulas with multiple incoming edges with a variable. In turn, the
			// common ancestor of all these formulas ensures that a 'let' is introduced where needed.
			engine.enqueue(new BuildFormula(info));

			// Then compute common ancestors of formulas. This follows the parent relationship computed
			// before to find a parent from which all occurrences of a given formula are reachable. This is
			// where a 'let' should be introduced. This also computes the depth of a formula (shortest path
			// reaching it from the root) which is used to actually find common ancestors.
			engine.enqueue(new ComputeAncestors(info));
		}
	}

	final static private class ComputeAncestors implements NonRecursive.Walker {
		final private FormulaInfo info;

		private ComputeAncestors(FormulaInfo info) {
			this.info = info;
		}

		@Override
		public void walk(NonRecursive engine) {
			if (!info.computeAncestors())
				// This node is not ready yet, some of its parents will have to be handled first.
				// (That will then make sure that this node is retried later on)
				return;

			for (FormulaInfo child : info.descendants)
				engine.enqueue(new ComputeAncestors(child));
		}
	}

	final private class BuildFormula extends FormulaWalker {
		private final FormulaInfo info;

		private BuildFormula(FormulaInfo info) {
			super(info.formula);
			this.info = info;
		}

		private void enqueueLets(NonRecursive engine) {
			if (info.descendants.isEmpty())
				return;

			List<FormulaInfo> toLet = new ArrayList<>();
			for (FormulaInfo descend : info.descendants) {
				if (descend.shouldLet()) {
					toLet.add(descend);
				}
			}
			Collections.sort(toLet, new Comparator<FormulaInfo>() {
				@Override
				public int compare(FormulaInfo i1, FormulaInfo i2) {
					assert i1.topoSort != i2.topoSort;
					return i1.topoSort - i2.topoSort;
				}
			});
			for (FormulaInfo descend : toLet) {
				engine.enqueue(new BuildLet(descend.getVariable()));
				engine.enqueue(new BuildFormula(descend));
			}
		}

		private void enqueueFormula(NonRecursive engine, Formula formula) {
			FormulaInfo inf = formulaInfo.get(formula);
			assert inf != null : formula;
			if (inf.shouldLet())
				engine.enqueue(new CopyFormula(inf.getVariable()));
			else
				engine.enqueue(new BuildFormula(inf));
		}

		@Override
		public void walk(NonRecursive engine, ConstantFormula formula) {
			assert info.descendants.isEmpty();
			engine.enqueue(new CopyFormula(formula));
		}

		@Override
		public void walk(NonRecursive engine, ConjunctionFormula formula) {
			enqueueLets(engine);
			engine.enqueue(new BuildConjunction(formula.getFormulas().size()));
			for (Formula child : formula.getFormulas())
				enqueueFormula(engine, child);
		}

		@Override
		public void walk(NonRecursive engine, DisjunctionFormula formula) {
			enqueueLets(engine);
			engine.enqueue(new BuildDisjunction(formula.getFormulas().size()));
			for (Formula child : formula.getFormulas())
				enqueueFormula(engine, child);
		}

		@Override
		public void walk(NonRecursive engine, NegationFormula formula) {
			enqueueLets(engine);
			engine.enqueue(new BuildNegation());
			enqueueFormula(engine, formula.getFormula());
		}

		@Override
		public void walk(NonRecursive engine, VariableFormula formula) {
			assert info.descendants.isEmpty();
			engine.enqueue(new CopyFormula(formula));
		}

		@Override
		public void walk(NonRecursive engine, ModalityFormula formula) {
			enqueueLets(engine);
			engine.enqueue(new BuildModality(formula.getModality(), formula.getEvent()));
			enqueueFormula(engine, formula.getFormula());
		}

		@Override
		public void walk(NonRecursive engine, FixedPointFormula formula) {
			enqueueLets(engine);
			engine.enqueue(new BuildFixedPoint( formula.getFixedPoint(), formula.getVariable()));
			enqueueFormula(engine, formula.getFormula());
		}

		@Override
		public void walk(NonRecursive engine, LetFormula formula) {
			throw new AssertionError("Subformula of type Let should have been eliminated, but found "
					+ formula);
		}
	}

	final private class CopyFormula implements NonRecursive.Walker {
		private final Formula formula;

		private CopyFormula(Formula formula) {
			this.formula = formula;
		}

		@Override
		public void walk(NonRecursive engine) {
			results.addLast(formula);
		}
	}

	final private class BuildLet implements NonRecursive.Walker {
		private final VariableFormula variable;

		private BuildLet(VariableFormula variable) {
			this.variable = variable;
		}

		@Override
		public void walk(NonRecursive engine) {
			Formula expansion = results.removeLast();
			Formula inner = results.removeLast();
			Formula formula = creator.let(variable, expansion, inner);
			results.addLast(formula);
		}
	}

	private abstract class AbstractBuildFormula implements NonRecursive.Walker {
		protected void setResult(Formula formula) {
			results.addLast(formula);
		}

		protected Formula getResult() {
			return results.removeLast();
		}
	}

	final private class BuildConjunction extends AbstractBuildFormula {
		private final int children;

		private BuildConjunction(int children) {
			this.children = children;
		}

		@Override
		public void walk(NonRecursive engine) {
			List<Formula> formulas = new ArrayList<>(children);
			for (int i = 0; i < children; i++)
				formulas.add(super.getResult());
			setResult(creator.conjunction(formulas));
		}
	}

	final private class BuildDisjunction extends AbstractBuildFormula {
		private final int children;

		private BuildDisjunction(int children) {
			this.children = children;
		}

		@Override
		public void walk(NonRecursive engine) {
			List<Formula> formulas = new ArrayList<>(children);
			for (int i = 0; i < children; i++)
				formulas.add(super.getResult());
			setResult(creator.disjunction(formulas));
		}
	}

	private class BuildNegation extends AbstractBuildFormula {
		@Override
		public void walk(NonRecursive engine) {
			setResult(creator.negate(super.getResult()));
		}
	}

	final private class BuildModality extends AbstractBuildFormula {
		private final Modality modality;
		private final String event;

		private BuildModality(Modality modality, String event) {
			this.modality = modality;
			this.event = event;
		}

		@Override
		public void walk(NonRecursive engine) {
			setResult(creator.modality(modality, event, super.getResult()));
		}
	}

	final private class BuildFixedPoint extends AbstractBuildFormula {
		private final FixedPoint fixedPoint;
		private final VariableFormula variable;

		private BuildFixedPoint(FixedPoint fixedPoint, VariableFormula variable) {
			this.fixedPoint = fixedPoint;
			this.variable = variable;
		}

		@Override
		public void walk(NonRecursive engine) {
			setResult(creator.fixedPoint(fixedPoint, variable, super.getResult()));
		}
	}

	final private class AssignInfo extends FormulaWalker {
		private final FormulaInfo parent;

		private AssignInfo(Formula formula) {
			this(formula, null);
		}

		private AssignInfo(Formula formula, FormulaInfo parent) {
			super(formula);
			this.parent = parent;
		}

		private void assign(NonRecursive engine, Formula... children) {
			FormulaInfo own = formulaInfo.get(formula);
			if (own == null) {
				own = new FormulaInfo(parent, formula);
				formulaInfo.put(formula, own);

				engine.enqueue(new AssignTopoSortNumber(own));

				for (Formula child : children)
					engine.enqueue(new AssignInfo(child, own));

			} else if (parent != null) {
				assert formula.equals(own.formula);
				own.mergeAncestor(parent);
			}
		}

		@Override
		public void walk(NonRecursive engine, ConstantFormula formula) {
			assign(engine);
		}

		@Override
		public void walk(NonRecursive engine, ConjunctionFormula formula) {
			assign(engine, formula.getFormulas().toArray(new Formula[0]));
		}

		@Override
		public void walk(NonRecursive engine, DisjunctionFormula formula) {
			assign(engine, formula.getFormulas().toArray(new Formula[0]));
		}

		@Override
		public void walk(NonRecursive engine, NegationFormula formula) {
			assign(engine, formula.getFormula());
		}

		@Override
		public void walk(NonRecursive engine, VariableFormula formula) {
			assign(engine);
		}

		@Override
		public void walk(NonRecursive engine, ModalityFormula formula) {
			assign(engine, formula.getFormula());
		}

		@Override
		public void walk(NonRecursive engine, FixedPointFormula formula) {
			assign(engine, formula.getFormula());
		}

		@Override
		public void walk(NonRecursive engine, LetFormula formula) {
			throw new AssertionError("Subformula of type Let should have been eliminated, but found "
					+ formula);
		}
	}

	final private class AssignTopoSortNumber implements NonRecursive.Walker {
		private final FormulaInfo info;

		private AssignTopoSortNumber(FormulaInfo info) {
			this.info = info;
		}

		@Override
		public void walk(NonRecursive engine) {
			assert info.topoSort == 0;
			info.topoSort = topoSortNum++;
		}
	}

	final static private class FormulaInfo {
		final private Formula formula;
		final private Collection<FormulaInfo> descendants = new HashSet<>();
		final private Collection<FormulaInfo> parents = new ArrayList<>();
		private int topoSort; // topological sorting order, computed during AssignInfo (phase 1)
		private int incomingEdges = 0; // number of formulas directly containing this formula (phase 1)
		private int depth = Integer.MAX_VALUE; // Computed during computeAncestors() (phase 2)
		private FormulaInfo commonAncestor = null; // Computed during computeAncestors() (phase 2)
		private VariableFormula variable = null;

		private FormulaInfo(FormulaInfo parent, Formula formula) {
			assert !(formula instanceof LetFormula);
			this.formula = formula;
			if (parent != null)
				mergeAncestor(parent);
		}

		private void mergeAncestor(FormulaInfo otherAncestor) {
			assert otherAncestor != null;
			incomingEdges++;
			parents.add(otherAncestor);
			otherAncestor.descendants.add(this);
		}

		private boolean computeAncestors() {
			if (depth != Integer.MAX_VALUE)
				// This node was already handled before
				return false;

			if (parents.isEmpty()) {
				// This is the root
				assert commonAncestor == null;
				assert incomingEdges == 0 : incomingEdges;
				depth = 0;
				return true;
			}

			// Compute depth by looking at the parents.
			// If any parent does not have a depth yet, return false to indicate "retry later".
			int d = Integer.MAX_VALUE;
			for (FormulaInfo parent : parents) {
				if (parent.depth == Integer.MAX_VALUE)
					return false;
				d = Math.min(d, parent.depth + 1);
			}
			assert d != Integer.MAX_VALUE;
			depth = d;

			// Now compute our common ancestor
			for (FormulaInfo otherAncestor : parents) {
				otherAncestor.descendants.remove(this);

				if (commonAncestor == null) {
					commonAncestor = otherAncestor;
				} else {
					while (!commonAncestor.equals(otherAncestor)) {
						if (commonAncestor.depth < otherAncestor.depth) {
							otherAncestor = otherAncestor.commonAncestor;
						} else if (commonAncestor.depth > otherAncestor.depth) {
							commonAncestor = commonAncestor.commonAncestor;
						} else {
							assert commonAncestor.depth == otherAncestor.depth;
							otherAncestor = otherAncestor.commonAncestor;
							commonAncestor = commonAncestor.commonAncestor;
						}
						assert commonAncestor != null;
						assert otherAncestor != null;
					}
				}
			}
			commonAncestor.descendants.add(this);
			return true;
		}

		// Should this formula be let'ed out?
		private boolean shouldLet() {
			if (formula instanceof ConstantFormula)
				return false;
			if (formula instanceof VariableFormula)
				return false;
			return incomingEdges > 1;
		}

		private VariableFormula getVariable() {
			if (variable == null)
				variable = formula.getCreator().freshVariable("cse");
			return variable;
		}
	}

	static public Formula let(Formula formula) {
		LetTransformer transformer = new LetTransformer(formula);
		new NonRecursive().run(transformer);
		Formula result = transformer.getResult();
		assert UnLetTransformer.unLet(formula).equals(UnLetTransformer.unLet(result))
			: formula + " vs " + result;
		return result;
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
