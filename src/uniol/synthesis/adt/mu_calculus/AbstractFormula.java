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

import java.util.Deque;

import uniol.synthesis.util.NonRecursive;
import uniol.synthesis.util.PrintFormula;

public abstract class AbstractFormula implements Formula {
	private final FormulaCreator creator;

	protected AbstractFormula(FormulaCreator creator) {
		this.creator = creator;
	}

	@Override
	public FormulaCreator getCreator() {
		return creator;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		PrintFormula.printFormula(sb, this);
		return sb.toString();
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
