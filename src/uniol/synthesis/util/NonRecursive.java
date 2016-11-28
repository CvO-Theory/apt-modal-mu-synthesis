package uniol.synthesis.util;

import java.util.Deque;
import java.util.ArrayDeque;

public class NonRecursive {
	private final Deque<Walker> todo = new ArrayDeque<Walker>();

	public interface Walker {
		public void walk(NonRecursive engine);
	}

	public void reset() {
		todo.clear();
	}

	public void run(Walker walker) {
		reset();
		enqueue(walker);
		run();
	}

	public void run() {
		while (!todo.isEmpty()) {
			todo.removeLast().walk(this);
		}
	}

	public void enqueue(Walker walker) {
		todo.addLast(walker);
	}
}
