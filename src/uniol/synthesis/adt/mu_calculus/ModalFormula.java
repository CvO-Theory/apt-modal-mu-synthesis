package uniol.synthesis.adt.mu_calculus;

public abstract class ModalFormula extends UnaryFormula {
	final private Modality modality;
	final private Event event;

	protected ModalFormula(Modality modality, Event event, Formula formula) {
		super(formula);
		this.modality = modality;
		this.event = event;
	}

	public Modality getModality() {
		return modality;
	}

	public Event getEvent() {
		return event;
	}
}
