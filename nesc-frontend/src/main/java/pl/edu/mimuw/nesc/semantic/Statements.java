package pl.edu.mimuw.nesc.semantic;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.*;

import java.util.LinkedList;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class Statements {

    private static final ErrorStmt ERROR_STMT;

    static {
        ERROR_STMT = new ErrorStmt(Location.getDummyLocation());
        ERROR_STMT.setEndLocation(Location.getDummyLocation());
    }

    public static ErrorStmt makeErrorStmt() {
        return ERROR_STMT;
    }

    public static ReturnStmt makeReturn(Location startLocation, Location endLocation, Expression expression) {
        final ReturnStmt returnStmt = new ReturnStmt(startLocation, expression);
        returnStmt.setEndLocation(endLocation);
        return returnStmt;
    }

    public static ReturnStmt makeVoidReturn(Location startLocation, Location endLocation) {
        final ReturnStmt returnStmt = new ReturnStmt(startLocation, null);
        returnStmt.setEndLocation(endLocation);
        return returnStmt;
    }

    /**
     * If statement list <code>l1</code> ends with an unfinished label,
     * attach <code>l2</code> to that label. Otherwise attach <code>l2</code>
     * to the end of <code>l1</code>.
     *
     * @param l1 left-hand side list
     * @param l2 right-hand side list
     * @return merged lists with fixed unfinished labels in <code>l1</code>
     */
    public static LinkedList<Statement> chainWithLabels(LinkedList<Statement> l1, LinkedList<Statement> l2) {
        assert l1 != null;
        assert l2 != null;

        if (l1.isEmpty())
            return l2;
        if (l2.isEmpty())
            return l1;
        /* There may be an unfinished sub-label due to 'a: b:' */

        /*
         * The result will always be merged l1 and l2 lists.
         */
        l1.addAll(l2);
        // TODO
        return l1;
    }

    private Statements() {
    }
}
