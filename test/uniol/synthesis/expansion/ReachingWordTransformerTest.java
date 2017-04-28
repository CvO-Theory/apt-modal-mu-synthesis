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

package uniol.synthesis.expansion;

import uniol.apt.adt.ts.TransitionSystem;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static uniol.apt.adt.matcher.Matchers.nodeWithID;

public class ReachingWordTransformerTest {
	@Test
	public void testInitialState() {
		TransitionSystem from = new TransitionSystem();
		from.createState("f0");
		from.setInitialState("f0");

		TransitionSystem to = new TransitionSystem();
		to.createState("t0");
		to.setInitialState("t0");

		assertThat(new ReachingWordTransformer(to).transform(from.getNode("f0")), nodeWithID("t0"));
	}

	@Test
	public void testLoop() {
		TransitionSystem from = new TransitionSystem();
		from.createStates("f0", "f1");
		from.setInitialState("f0");
		from.createArc("f0", "f1", "a");

		TransitionSystem to = new TransitionSystem();
		to.createState("t0");
		to.setInitialState("t0");
		to.createArc("t0", "t0", "a");

		assertThat(new ReachingWordTransformer(to).transform(from.getNode("f1")), nodeWithID("t0"));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testMissingArc() {
		TransitionSystem from = new TransitionSystem();
		from.createStates("f0", "f1");
		from.setInitialState("f0");
		from.createArc("f0", "f1", "a");

		TransitionSystem to = new TransitionSystem();
		to.createState("t0");
		to.setInitialState("t0");

		new ReachingWordTransformer(to).transform(from.getNode("f1"));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void testUnreachable() {
		TransitionSystem from = new TransitionSystem();
		from.createStates("f0", "f1");
		from.setInitialState("f0");

		TransitionSystem to = new TransitionSystem();
		to.createState("t0");
		to.setInitialState("t0");

		new ReachingWordTransformer(to).transform(from.getNode("f1"));
	}

	@Test
	public void testPath() {
		TransitionSystem from = new TransitionSystem();
		from.createStates("f0", "f1", "f2");
		from.setInitialState("f0");
		from.createArc("f0", "f1", "a");
		from.createArc("f1", "f2", "b");

		TransitionSystem to = new TransitionSystem();
		to.createStates("t0", "t1");
		to.setInitialState("t0");
		to.createArc("t0", "t0", "a");
		to.createArc("t0", "t1", "b");

		assertThat(new ReachingWordTransformer(to).transform(from.getNode("f2")), nodeWithID("t1"));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
