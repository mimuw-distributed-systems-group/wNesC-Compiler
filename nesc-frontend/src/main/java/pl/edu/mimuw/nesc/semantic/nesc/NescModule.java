package pl.edu.mimuw.nesc.semantic.nesc;

import java.util.LinkedList;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.GenericCall;
import pl.edu.mimuw.nesc.ast.gen.IdentifierDeclarator;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRefDeclarator;
import pl.edu.mimuw.nesc.ast.gen.Module;
import pl.edu.mimuw.nesc.ast.gen.Word;

/**
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public class NescModule {

	public static GenericCall makeGenericCall(Location location,
			Expression iref, LinkedList<Expression> args) {
		GenericCall result = new GenericCall(location, iref, args);
		// TODO
		return result;
	}

	public static InterfaceRefDeclarator makeInterfaceRefDeclarator(
			Location location, String w1, String w2) {
		IdentifierDeclarator id = new IdentifierDeclarator(location, w2);
		InterfaceRefDeclarator declarator = new InterfaceRefDeclarator(
				location, id, makeWord(location, w1));
		return declarator;
	}

	public static Expression makeInterfaceDeref(Location location,
			Expression object, String field) {
		// TODO
		return null;
	}

	public static void processModule(Module module) {
		// TODO
	}

	public static Word makeWord(Location location, String s) {
		return new Word(location, s);
	}

	private NescModule() {
	}

}
