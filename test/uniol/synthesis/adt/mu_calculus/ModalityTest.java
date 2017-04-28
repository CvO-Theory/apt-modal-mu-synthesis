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

package uniol.synthesis.adt.mu_calculus;

import org.testng.annotations.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ModalityTest {
	@Test
	public void testNegate() {
		assertThat(Modality.UNIVERSAL.negate(), equalTo(Modality.EXISTENTIAL));
		assertThat(Modality.EXISTENTIAL.negate(), equalTo(Modality.UNIVERSAL));
	}

	@Test
	public void testToString() {
		assertThat(Modality.UNIVERSAL.toString("event"), equalTo("[event]"));
		assertThat(Modality.EXISTENTIAL.toString("event"), equalTo("<event>"));
	}

	@Test
	public void testValues() {
		assertThat(Modality.values(), arrayContainingInAnyOrder(Modality.EXISTENTIAL, Modality.UNIVERSAL));
	}

	@Test
	public void testValueOf() {
		assertThat(Modality.valueOf("UNIVERSAL"), is(Modality.UNIVERSAL));
		assertThat(Modality.valueOf("EXISTENTIAL"), is(Modality.EXISTENTIAL));
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
