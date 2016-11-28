package uniol.synthesis.tableau;

import java.util.Arrays;
import java.util.HashSet;

import uniol.apt.adt.ts.State;
import uniol.synthesis.adt.mu_calculus.Formula;

public class ConjunctionTableau extends UniversalTableau {
	public ConjunctionTableau(State state, Formula formula, Tableau left, Tableau right) {
		super(state, formula, new HashSet<Tableau>(Arrays.asList(left, right)));
	}
}
