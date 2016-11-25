package uniol.synthesis.adt.mu_calculus;

public abstract class ModalFormula extends AbstractFormula {
	final private Modality modality;
	final private Event event;
	final private Formula formula;

	protected ModalFormula(Modality modality, Event event, Formula formula) {
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
}
