package pl.edu.mimuw.nesc.ast;

public class IValue {

	/*
	 * Types representing a parsed initialiser. Unspecified fields and array
	 * elements were unspecified in the initialiser.
	 */

	public static enum Kind {
		BASE, ARRAY, STRUCTURED;
	}

	private Kind kind;
	private Type type;
	private IValue instantiation;

}
