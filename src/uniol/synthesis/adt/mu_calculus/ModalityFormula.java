package uniol.synthesis.adt.mu_calculus;

import uniol.synthesis.util.FormulaCreator;

public class ModalityFormula extends AbstractFormula {
	final private Modality modality;
	final private Event event;
	final private Formula formula;

	protected ModalityFormula(FormulaCreator creator, Modality modality, Event event, Formula formula) {
		super(creator);
		this.modality = modality;
		this.event = event;
		this.formula = formula;
	}

	public Modality getModality() {
		return modality;
	}

	public Event getEvent() {
		return event;
	}

	public Formula getFormula() {
		return formula;
	}

	public static ModalityFormula modality(FormulaCreator creator, Modality modality, Event event, Formula innerFormula) {
		int hashCode = modality.hashCode() ^ event.hashCode() ^ innerFormula.hashCode();
		for (Formula formula : creator.getFormulasWithHashCode(hashCode)) {
			if (formula instanceof ModalityFormula) {
				ModalityFormula result = (ModalityFormula) formula;
				if (result.getModality().equals(modality) && result.getEvent().equals(event)
						&& result.getFormula().equals(innerFormula))
					return result;
			}
		}
		ModalityFormula result = new ModalityFormula(creator, modality, event, innerFormula);
		creator.addFormulaInternal(hashCode, result);
		return result;
	}
}
