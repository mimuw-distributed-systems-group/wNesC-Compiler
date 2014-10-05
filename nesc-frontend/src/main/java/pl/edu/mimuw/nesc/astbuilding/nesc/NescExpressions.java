package pl.edu.mimuw.nesc.astbuilding.nesc;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.*;

import java.util.LinkedList;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class NescExpressions {

    public static TypeArgument makeTypeArgument(AstType astType) {
        final TypeArgument typeArgument = new TypeArgument(astType.getLocation(), astType);
        typeArgument.setEndLocation(astType.getEndLocation());
        typeArgument.setType(astType.getType());
        return typeArgument;
    }

    public static ComponentDeref makeComponentDeref(Identifier component, Word field) {
        final ComponentDeref result = new ComponentDeref(component.getLocation(), component, field);
        result.setEndLocation(field.getEndLocation());
        return result;
    }

    public static InterfaceDeref makeInterfaceDeref(Identifier iface, Word methodName) {
        final InterfaceDeref result = new InterfaceDeref(iface.getLocation(), iface, methodName);
        result.setEndLocation(methodName.getEndLocation());
        return result;
    }

    public static GenericCall makeGenericCall(Location location, Location endLocation, Expression object,
                                              LinkedList<Expression> genericParams) {
        final GenericCall result = new GenericCall(location, object, genericParams);
        result.setEndLocation(endLocation);
        return result;
    }

    private NescExpressions() {
    }

}
