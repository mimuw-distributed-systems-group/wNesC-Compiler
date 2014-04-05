package pl.edu.mimuw.nesc.astbuilding.nesc;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.common.util.list.Lists;

import java.util.LinkedList;

import static pl.edu.mimuw.nesc.ast.AstUtils.getEndLocation;
import static pl.edu.mimuw.nesc.ast.AstUtils.getStartLocation;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class NescDeclarations {

    public static Declaration declareTemplateParameter(Optional<Declarator> declarator,
                                                       LinkedList<TypeElement> elements,
                                                       LinkedList<Attribute> attributes) {
        /* Either declarator or elements is present. */
        final Location startLocation = declarator.isPresent()
                ? declarator.get().getLocation()
                : getStartLocation(elements).get();
        final Location endLocation = declarator.isPresent()
                ? getEndLocation(declarator.get().getEndLocation(), elements, attributes)
                : getEndLocation(startLocation, elements, attributes); // elements in not empty, $1 will not be used

        final VariableDecl variableDecl = new VariableDecl(startLocation, declarator.orNull(), attributes, null, null);
        variableDecl.setEndLocation(endLocation);
        final DataDecl dataDecl = new DataDecl(startLocation, elements, Lists.<Declaration>newList(variableDecl));
        dataDecl.setEndLocation(endLocation);
        return dataDecl;
    }

    private NescDeclarations() {
    }

}
