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
