package pl.edu.mimuw.nesc.semantic.nesc;

import java.util.LinkedList;

import pl.edu.mimuw.nesc.ast.Environment;
import pl.edu.mimuw.nesc.ast.NescDeclaration;
import pl.edu.mimuw.nesc.ast.datadeclaration.DataDeclaration;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRef;
import pl.edu.mimuw.nesc.common.SourceLanguage;
import pl.edu.mimuw.nesc.semantic.Semantics;

public class NescComponents {

	public static void buildComponent(NescDeclaration cdecl) {
		// TODO
	}

	public static void declareInterfaceRef(InterfaceRef iref,
			LinkedList<Declaration> gparms, Environment env,
			LinkedList<Attribute> attributes) {
		// TODO
	}

	public static void makeImplicitInterface(DataDeclaration fndecl,
			FunctionDeclarator fdeclarator) {
		// TODO
	}

	public static void checkInterfaceParameterTypes(Declaration parms) {
		// TODO
	}

	public static Environment startImplementation() {
		Environment env = new Environment(
				(Semantics.current.isEnvEmpty() ? null
						: Semantics.current.peekEnv()), true, false);
		Semantics.current.startSemantics(SourceLanguage.IMPLEMENTATION,
				Semantics.current.container, env);
		// TODO
		return env;
	}

	private NescComponents() {
		throw new RuntimeException(
				"Object of this class should never be created");
	}
}
