package uniol.synthesis.adt.mu_calculus;

public enum Modality {
	UNIVERSAL {
		@Override
		public Modality negate() {
			return EXISTENTIAL;
		}

		@Override
		public String toString(String event) {
			return "[" + event + "]";
		}
	},
	EXISTENTIAL {
		@Override
		public Modality negate() {
			return UNIVERSAL;
		}

		@Override
		public String toString(String event) {
			return "<" + event + ">";
		}
	};

	abstract public Modality negate();
	abstract public String toString(String event);
}
