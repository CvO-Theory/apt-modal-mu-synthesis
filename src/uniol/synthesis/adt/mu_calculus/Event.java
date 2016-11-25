package uniol.synthesis.adt.mu_calculus;

public class Event implements Comparable<Event> {

	private final String label;

	public Event(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	@Override
	public int compareTo(Event other) {
		return label.compareTo(other.label);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Event))
			return false;

		return label.equals(((Event) o).label);
	}

	@Override
	public int hashCode() {
		return label.hashCode();
	}

	@Override
	public String toString() {
		return label;
	}
}
