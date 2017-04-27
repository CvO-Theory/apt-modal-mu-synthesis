package uniol.synthesis.adt.mu_calculus;

public enum FixedPoint {
	LEAST {
		@Override
		public FixedPoint negate() {
			return GREATEST;
		}

		@Override
		public String toString() {
			return "mu";
		}
	},
	GREATEST {
		@Override
		public FixedPoint negate() {
			return LEAST;
		}

		@Override
		public String toString() {
			return "nu";
		}
	};

	abstract public FixedPoint negate();
}
