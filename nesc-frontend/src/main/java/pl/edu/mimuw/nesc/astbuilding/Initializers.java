package pl.edu.mimuw.nesc.astbuilding;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.common.util.list.Lists;

import java.util.LinkedList;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class Initializers {

    public static Designator setInitIndex(Location startLocation, Location endLocation, Expression first,
                                          Optional<Expression> last) {
        final Designator designator = new DesignateIndex(startLocation, first, last);
        designator.setEndLocation(endLocation);
        return designator;
    }

    public static Designator setInitLabel(Location startLocation, Location endLocation, String fieldName) {
        final DesignateField designator = new DesignateField(startLocation, fieldName);
        designator.setEndLocation(endLocation);
        return designator;
    }

    public static Expression makeInitSpecific(Location startLocation, Location endLocation,
                                              LinkedList<Designator> designators, Expression initialValue) {
        final InitSpecific result = new InitSpecific(startLocation, designators, initialValue);
        result.setEndLocation(endLocation);
        return result;
    }

    public static InitSpecific makeInitSpecific(Location startLocation, Location endLocation, Designator designator,
                                                Expression initialValue) {
        final InitSpecific result = new InitSpecific(startLocation, Lists.newList(designator), initialValue);
        result.setEndLocation(endLocation);
        return result;
    }

    public static InitList makeInitList(Location startLocation, Location endLocation,
                                        LinkedList<Expression> expressions) {
        final InitList initList = new InitList(startLocation, expressions);
        initList.setEndLocation(endLocation);
        return initList;
    }

    private Initializers() {
    }
}
