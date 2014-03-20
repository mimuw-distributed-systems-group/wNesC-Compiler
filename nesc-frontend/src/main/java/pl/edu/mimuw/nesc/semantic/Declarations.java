package pl.edu.mimuw.nesc.semantic;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.AstUtils;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.*;

import java.util.LinkedList;

/**
 * <p>
 * Contains a set of methods useful for creating syntax tree nodes during
 * parsing.
 * </p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class Declarations {

    private static final ErrorDecl ERROR_DECLARATION;

    static {
        ERROR_DECLARATION = new ErrorDecl(Location.getDummyLocation());
        ERROR_DECLARATION.setEndLocation(Location.getDummyLocation());
    }

    public static ErrorDecl makeErrorDecl() {
        return ERROR_DECLARATION;
    }

    public static VariableDecl startDecl(Declarator declarator, Optional<AsmStmt> asmStmt,
                                         LinkedList<TypeElement> elements, LinkedList<Attribute> attributes,
                                         boolean initialised) {
        final VariableDecl variableDecl = new VariableDecl(declarator.getLocation(), declarator, attributes, null,
                asmStmt.orNull());
        if (initialised) {
            final Location endLocation = AstUtils.getEndLocation(
                    asmStmt.isPresent() ? asmStmt.get().getEndLocation() : declarator.getEndLocation(),
                    elements,
                    attributes);
            variableDecl.setEndLocation(endLocation);
        }
        return variableDecl;
    }

    public static VariableDecl finishDecl(VariableDecl declaration, Optional<Expression> initializer) {
        if (initializer.isPresent()) {
            final Location endLocation = initializer.get().getEndLocation();
            declaration.setEndLocation(endLocation);
        }
        return declaration;
    }

    public static DataDecl makeDataDecl(Location startLocation, Location endLocation,
                                        LinkedList<TypeElement> modifiers, LinkedList<Declaration> decls) {
        final DataDecl result = new DataDecl(startLocation, modifiers, decls);
        result.setEndLocation(endLocation);
        return result;
    }

    public static ExtensionDecl makeExtensionDecl(Location startLocation, Location endLocation, Declaration decl) {
        // TODO: pedantic
        final ExtensionDecl result = new ExtensionDecl(startLocation, decl);
        result.setEndLocation(endLocation);
        return result;
    }

    private Declarations() {
    }

}
