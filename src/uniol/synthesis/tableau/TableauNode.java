package uniol.synthesis.tableau;

import uniol.apt.adt.ts.State;

import uniol.synthesis.adt.mu_calculus.Formula;

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
}
