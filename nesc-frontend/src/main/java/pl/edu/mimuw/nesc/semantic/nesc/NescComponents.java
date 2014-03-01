package pl.edu.mimuw.nesc.semantic.nesc;

import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRef;

import java.util.LinkedList;

public final class NescComponents {

    public static void declareInterfaceRef(InterfaceRef ifaceRef, LinkedList<Declaration> genericParameters,
                                           LinkedList<Attribute> attributes) {
        ifaceRef.setGenericParameters(genericParameters);
        ifaceRef.setAttributes(attributes);
    }

    private NescComponents() {
    }
}
