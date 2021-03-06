/*-
 * APT - Analysis of Petri Nets and labeled Transition systems
 * Copyright (C) 2017  Uli Schlachter
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package uniol.synthesis.util;

import java.util.ArrayDeque;
import java.util.Deque;

public class NonRecursive {
	private final Deque<Walker> todo = new ArrayDeque<Walker>();

	public interface Walker {
		void walk(NonRecursive engine);
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

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
