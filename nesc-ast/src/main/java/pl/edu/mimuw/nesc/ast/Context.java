package pl.edu.mimuw.nesc.ast;

public enum Context {
	ATOMIC, EXECUTABLE, READ, WRITE, FNCALL, ADDRESSED, DEREF, CONSTANT;
}
