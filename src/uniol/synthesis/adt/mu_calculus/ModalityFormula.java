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

public class ModalityFormula extends AbstractFormula {
	final private Modality modality;
	final private String event;
	final private Formula formula;

	protected ModalityFormula(FormulaCreator creator, Modality modality, String event, Formula formula) {
		super(creator);
		this.modality = modality;
		this.event = event;
		this.formula = formula;
	}

	public Modality getModality() {
		return modality;
	}

	public String getEvent() {
		return event;
	}

	public Formula getFormula() {
		return formula;
	}

	static ModalityFormula modality(FormulaCreator creator, Modality modality, String event, Formula innerFormula) {
		int hashCode = modality.hashCode() ^ event.hashCode() ^ innerFormula.hashCode();
		for (Formula formula : creator.getFormulasWithHashCode(hashCode)) {
			if (formula instanceof ModalityFormula) {
				ModalityFormula result = (ModalityFormula) formula;
				if (result.getModality().equals(modality) && result.getEvent().equals(event)
						&& result.getFormula().equals(innerFormula))
					return result;
			}
		}
		ModalityFormula result = new ModalityFormula(creator, modality, event, innerFormula);
		creator.addFormulaInternal(hashCode, result);
		return result;
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
