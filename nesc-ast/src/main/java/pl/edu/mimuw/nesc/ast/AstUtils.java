package pl.edu.mimuw.nesc.ast;

import java.util.LinkedList;

import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;

public final class AstUtils {
	
	public static boolean isOldStyleFunction(FunctionDecl fn) {
		final LinkedList<Declaration> parms = fn.getFdeclarator().getParms();
		return (!parms.isEmpty() || isOldIdentifierDecl(parms));
	}
	
	public static boolean isOldIdentifierDecl(LinkedList<Declaration> parms) {
		// TODO visitor for all parms
		return false;
	}

}
