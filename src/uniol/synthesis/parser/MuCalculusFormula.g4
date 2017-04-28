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

grammar MuCalculusFormula;

formula:	term EOF;

// The order of entries defines precedence. Associative is "left" by default, which is just what we want.
term:		NEGATION term					# termNegation
	|	IDENTIFIER					# termVariable
	|	PAREN_OPEN term PAREN_CLOSE			# termParantheses
	|	ANGLE_OPEN IDENTIFIER ANGLE_CLOSE term		# termExistentialModality
	|	BRACKET_OPEN IDENTIFIER BRACKET_CLOSE term	# termUniversalModality
	|	term CONJUNCTION term				# termConjunction
	|	term DISJUNCTION term				# termDisjunction
	|	MU IDENTIFIER DOT term				# termLeastFixedPoint
	|	NU IDENTIFIER DOT term				# termGreatestFixedPoint
	|	FALSE						# termFalse
	|	TRUE						# termTrue
	;

DOT:		'.';
PAREN_OPEN:	'(';
PAREN_CLOSE:	')';
ANGLE_OPEN:	'<';
ANGLE_CLOSE:	'>';
BRACKET_OPEN:	'[';
BRACKET_CLOSE:	']';
NEGATION:	'!';
CONJUNCTION:	'&&';
DISJUNCTION:	'||';
TRUE:		('t'|'T')('r'|'R')('u'|'U')('e'|'E');
FALSE:		('f'|'F')('a'|'A')('l'|'L')('s'|'S')('e'|'E');
NU:		('n'|'N')('u'|'U') | 'ν';
MU:		('m'|'M')('u'|'U') | 'µ';
IDENTIFIER:	('0'..'9'|'a'..'z'|'A'..'Z')+;

EOL_COMMENT:	'//' ~('\n'|'\r')* -> skip;
C_COMMENT:	'/*' .*? '*/' -> skip;
WS:		( ' ' | '\n' | '\r' | '\t')+ -> skip;

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120
