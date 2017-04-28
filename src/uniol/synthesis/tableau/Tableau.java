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

package uniol.synthesis.tableau;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections4.Transformer;

import uniol.apt.adt.ts.State;

public class Tableau {
	private final Set<TableauNode> leaves;

	public Tableau(Collection<TableauNode> leaves) {
		this.leaves = Collections.unmodifiableSet(new HashSet<TableauNode>(leaves));
	}

	public Set<TableauNode> getLeaves() {
		return leaves;
	}

	public boolean isSuccessful() {
		for (TableauNode node : leaves)
			if (!node.isSuccessful())
				return false;
		return true;
	}

	public Tableau transform(final Transformer<State, State> transformer) {
		Set<TableauNode> result = new HashSet<>();
		for (TableauNode leave : leaves)
			result.add(leave.transform(transformer));
		return new Tableau(result);
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
