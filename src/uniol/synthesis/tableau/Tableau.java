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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.collections4.Transformer;

public class Tableau<S> {
	private final Collection<TableauNode<S>> leaves;

	public Tableau(Collection<TableauNode<S>> leaves) {
		this.leaves = Collections.unmodifiableCollection(new ArrayList<TableauNode<S>>(leaves));
	}

	public Collection<TableauNode<S>> getLeaves() {
		return leaves;
	}

	public boolean isSuccessful() {
		for (TableauNode<S> node : leaves)
			if (!node.isSuccessful())
				return false;
		return true;
	}

	public Tableau<S> transform(final Transformer<S, S> transformer) {
		Collection<TableauNode<S>> result = new ArrayList<>(leaves.size());
		for (TableauNode<S> leave : leaves)
			result.add(leave.transform(transformer));
		return new Tableau<S>(result);
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
