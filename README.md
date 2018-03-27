Modal Mu-Calculus and Petri nets
================================

This repository contains some additional modules for
[APT](https://github.com/CvO-Theory/apt). Please read the [README.md of
APT](https://github.com/CvO-Theory/apt/blob/master/README.md) for more
information about APT.

Building this code
------------------

You can build an executable JAR file by running `ant jar` in the source
directory. This command creates the file `modal-mu-synthesis.jar` which you can
execute via `java -jar modal-mu-synthesis.jar`. Since this repository references
APTs source code directly, you do not need an extra copy of APT for this.

New Modules
-----------

Currently, this repository contains four additional modules for APT:

- `model_check`: Check if a given lts satisfies a given formula.
- `mts_to_formula`: Translate a modal transition system into a formula. Edges
  with the extension `[may]` are interpreted as may edges while all other edges
  are both may and must edges.
- `realise_pn`: Given a class of Petri nets and a formula, find Petri net
  realisations of the formula, i.e. find Petri nets which have a reachability
  graph that satisfies the given formula.
- `call_expansion`: Expand some abbreviations in formulas. See this module's
  long description for details.

Specifying Formulas
-------------------

Formulas of the modal mu-calculus are specified as follows:

- `true`, `false`, and parentheses are just entered.
- An existential modality is entered as `<a>X` and means that event `a` has to
  be possible and afterwards `X` holds.
- A universal modality `[a]X` means that after every `a`-labelled transition,
  `X` holds.
- Negation is written as an exclamation mark `!`.
- Conjunction and disjunction are written as `&&` and `||`.
- `Let` expressions are detailed below.
- Fixed points are entered as `nu X.term` and `mu X.term`, where `term` may
  contain `X` as a variable.

let`-expressions are interpreted as syntactic substitution. For example, `let X
= [a]false in [b]X && [c]X` expresses that the sequences `ba`and `ca` are not
allowed.

A complete example of a formula is
```
nu X.<a><b><c>true && <b><a>[c]X
```
The precedence of operators is the order in which they are given above.
