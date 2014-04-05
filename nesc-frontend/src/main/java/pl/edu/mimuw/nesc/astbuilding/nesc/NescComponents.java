package pl.edu.mimuw.nesc.astbuilding.nesc;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.*;

import java.util.LinkedList;

import static pl.edu.mimuw.nesc.ast.AstUtils.makeWord;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class NescComponents {

    public static void declareInterfaceRef(InterfaceRef ifaceRef, LinkedList<Declaration> genericParameters,
                                           LinkedList<Attribute> attributes) {
        ifaceRef.setGenericParameters(genericParameters);
        ifaceRef.setAttributes(attributes);
    }

    public static InterfaceRefDeclarator makeInterfaceRefDeclarator(Location ifaceStartLocation, String ifaceName,
                                                                    Location funcNameStartLocation,
                                                                    Location funcNameEndLocation, String functionName) {
        final IdentifierDeclarator id = new IdentifierDeclarator(funcNameStartLocation, functionName);
        id.setEndLocation(funcNameEndLocation);
        final InterfaceRefDeclarator declarator = new InterfaceRefDeclarator(ifaceStartLocation, id,
                makeWord(ifaceStartLocation, funcNameEndLocation, ifaceName));
        return declarator;
    }

    private NescComponents() {
    }
}
