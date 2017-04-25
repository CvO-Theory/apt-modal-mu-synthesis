package uniol.synthesis.tableau;

import uniol.apt.adt.ts.State;

import uniol.synthesis.adt.mu_calculus.ConstantFormula;
import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.Modality;
import uniol.synthesis.adt.mu_calculus.ModalityFormula;

public class TableauNode {
	private State state;
	private Formula formula;

	public TableauNode(State state, Formula formula) {
		this.state = state;
		this.formula = formula;
	}

	public State getState() {
		return state;
	}

	public Formula getFormula() {
		return formula;
	}

	/// Return true if this node as a leave corresponds to a successful tableau
	public boolean isSuccessful() {
		if (formula instanceof ModalityFormula) {
			ModalityFormula modf = (ModalityFormula) formula;
			return modf.getModality().equals(Modality.UNIVERSAL) &&
				state.getPostsetEdgesByLabel(modf.getEvent().getLabel()).isEmpty();
		}
		if (formula instanceof ConstantFormula) {
			return ((ConstantFormula) formula).getValue();
		}
		return false;
	}

	public TableauNode createChild(State st, Formula fm) {
		return new TableauNode(st, fm);
	}

	public TableauNode createChild(Formula fm) {
		return new TableauNode(this.state, fm);
	}

	@Override
	public int hashCode() {
		int result = 0;
		result = result * 37 + state.hashCode();
		result = result * 37 + formula.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TableauNode))
			return false;
		TableauNode other = (TableauNode) o;
		return this.state.equals(other.state) &&
			this.formula.equals(other.formula);
	}
}
