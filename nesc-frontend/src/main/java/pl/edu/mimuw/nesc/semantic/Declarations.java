package pl.edu.mimuw.nesc.semantic;

import java.util.LinkedList;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.AsmStmt;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Declarator;
import pl.edu.mimuw.nesc.ast.gen.ErrorDecl;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.ExtensionDecl;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;

/**
 * <p>
 * Contains a set of methods useful for creating syntax tree nodes during
 * parsing.
 * </p>
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public class Declarations {

    private static final ErrorDecl ERROR_DECLARATION;

    static {
        ERROR_DECLARATION = new ErrorDecl(Location.getDummyLocation());
        ERROR_DECLARATION.setEndLocation(Location.getDummyLocation());
    }

	// TODO extern data_declaration bad_decl;

	public static VariableDecl startDecl(Declarator declarator, AsmStmt asmStmt,
			LinkedList<TypeElement> elements, boolean initialised, LinkedList<Attribute> attributes) {
		VariableDecl variableDecl = new VariableDecl(declarator.getLocation(), declarator,
				attributes, null, asmStmt);
		// TODO
		return variableDecl;
	}

	public static VariableDecl finishDecl(VariableDecl decl, Expression init) {
		// TODO
		return decl;
	}

	public static DataDecl makeDataDecl(LinkedList<TypeElement> modifiers,
			LinkedList<Declaration> decls) {
		DataDecl result = new DataDecl(null, modifiers, decls);
		// TODO
		return result;
	}

	public static ExtensionDecl makeExtensionDecl(int oldPedantic, Location location,
			Declaration decl) {
		ExtensionDecl result = new ExtensionDecl(location, decl);
		// TODO
		return result;
	}

	public static ErrorDecl makeErrorDecl() {
		return ERROR_DECLARATION;
	}

	private Declarations() {
	}

}
