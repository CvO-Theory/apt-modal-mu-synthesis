package uniol.synthesis.adt.mu_calculus;

public class ModalityFormula extends AbstractFormula {
	final private Modality modality;
	final private String event;
	final private Formula formula;

	protected ModalityFormula(FormulaCreator creator, Modality modality, String event, Formula formula) {
		super(creator);
		this.modality = modality;
		this.event = event;
		this.formula = formula;
	}

	public Modality getModality() {
		return modality;
	}

	public String getEvent() {
		return event;
	}

	public Formula getFormula() {
		return formula;
	}

	static ModalityFormula modality(FormulaCreator creator, Modality modality, String event, Formula innerFormula) {
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
