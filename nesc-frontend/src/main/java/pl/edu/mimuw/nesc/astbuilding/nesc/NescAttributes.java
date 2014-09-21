package pl.edu.mimuw.nesc.astbuilding.nesc;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.InitList;
import pl.edu.mimuw.nesc.ast.gen.NescAttribute;
import pl.edu.mimuw.nesc.ast.gen.Word;

import java.util.LinkedList;

import static pl.edu.mimuw.nesc.ast.util.AstUtils.getEndLocation;
import static pl.edu.mimuw.nesc.ast.util.AstUtils.getStartLocation;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class NescAttributes {

    public static NescAttribute startAttributeUse(Word name) {
        final NescAttribute attr = new NescAttribute(null, name, null);

        /*
         * TODO: there is a nice trick to handle error recovery
         * see: nesc-attributes.c#start_attribute_use
         *
         * Create new environment so that we can track whether this is a
         * deputy scope or not. Using an environment makes it easy to
         * recover parsing errors: we just call poplevel in the appropriate
         * error production (see nattrib rules in c-parse.y).
         */

        return attr;
    }

    public static NescAttribute finishAttributeUse(Location startLocation, Location endLocation,
                                                   NescAttribute attribute, LinkedList<Expression> initializer) {

        final Expression initList;
        if (initializer.isEmpty()) {
            initList = new InitList(Location.getDummyLocation(), initializer);
        } else {
            final Location initStartLocation = getStartLocation(initializer).get();
            final Location initEndLocation = getEndLocation(initializer).get();

            initList = new InitList(initStartLocation, initializer);
            initList.setEndLocation(initEndLocation);
        }

        attribute.setValue(initList);
        attribute.setLocation(startLocation);
        attribute.setEndLocation(endLocation);
        return attribute;
    }

}
