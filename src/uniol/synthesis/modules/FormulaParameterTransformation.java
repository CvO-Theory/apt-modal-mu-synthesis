package uniol.synthesis.modules;

import uniol.apt.io.parser.ParseException;
import uniol.apt.module.exception.ModuleException;
import uniol.apt.ui.AptParameterTransformation;
import uniol.apt.ui.ParameterTransformation;

import uniol.synthesis.adt.mu_calculus.Formula;
import uniol.synthesis.adt.mu_calculus.FormulaCreator;
import uniol.synthesis.parser.FormulaParser;

@AptParameterTransformation(Formula.class)
public class FormulaParameterTransformation implements ParameterTransformation<Formula> {
	@Override
	public Formula transform(String arg) throws ModuleException {
		try {
			return FormulaParser.parse(new FormulaCreator(), arg);
		} catch (ParseException e) {
			throw new ModuleException(e);
		}
	}
}
