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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import uniol.apt.adt.ts.State;

public class GraphvizProgressCallback implements TableauBuilder.ProgressCallback {
	private final StringBuilder builder = new StringBuilder("digraph img {\n");
	private final Map<TableauNode<State>, String> mapping = new HashMap<>();
	private int nodeCounter = 0;
	private int tableauCounter = 0;

	private String mapNode(TableauNode<State> node) {
		String result = mapping.get(node);
		if (result == null) {
			result = "s" + (nodeCounter++);
			mapping.put(node, result);
			builder.append(result).append("[label=\"").append(node.getState()).append(", ");
			builder.append(node.getFormula()).append("\"];\n");
		}
		return result;
	}

	@Override
	public void children(TableauNode<State> node, Set<Set<TableauNode<State>>> children) {
		String from = mapNode(node);
		if (children == null) {
			builder.append(from).append(" -> fail;\n");
		} else {
			for (Set<TableauNode<State>> set : children) {
				for (TableauNode<State> child : set) {
					String target = mapNode(child);
					builder.append(from).append(" -> ").append(target).append(";\n");
				}
			}
		}
	}

	public void tableau(Tableau tableau) {
		String id = "t" + (tableauCounter++) + (tableau.isSuccessful() ? "success" : "fail");
		for (TableauNode<State> leave : tableau.getLeaves()) {
			String target = mapNode(leave);
			builder.append(id).append(" -> ").append(target).append(";\n");
		}
	}

	@Override
	public String toString() {
		return builder.toString() + "}\n";
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
