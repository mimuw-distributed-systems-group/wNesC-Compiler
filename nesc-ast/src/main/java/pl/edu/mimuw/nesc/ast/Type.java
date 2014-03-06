package pl.edu.mimuw.nesc.ast;

import java.util.LinkedList;

import pl.edu.mimuw.nesc.ast.datadeclaration.DataDeclaration;

public class Type {

	public static enum Kind {
		PRIMITIVE, COMPLEX, TAGGED, ERROR, VOID, POINTER, FUNCTION, ARRAY, IREF, VARIABLE, CREF;
	}
	
	public static enum Network {
		NX_NO, NX_BASE, NX_DERIVED;
	}

	private Kind kind;
	private LinkedList<Object> typeQuals;
	private Network network;
	private DataDeclaration combiner, basedecl, typedefdecl;
	// TODO size, alignment
	private boolean userAlign;

	public Type() {
	}

	public Kind getKind() {
		return kind;
	}

	public void setKind(Kind kind) {
		this.kind = kind;
	}

}
