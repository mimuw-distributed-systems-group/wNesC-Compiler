package pl.edu.mimuw.nesc.parser;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.Statement;

import java.util.LinkedList;

/**
 * <p>
 * Simulates union from C/C++. Yacc uses a union to pass result of each
 * production. The production's result may have different types, depending on
 * production (e.g. int, string, expr, stmt), so that the object returned has
 * many different fields: one for each possible returned object type.
 * </p>
 * <p/>
 * <p>
 * Java language does not provide union, but still we define a field for each
 * possible type of returned object.
 * </p>
 * <p/>
 * <p>
 * Parser demands fields to be . It does not access them using setters and
 * getters.
 * </p>
 * <p/>
 * <p>
 * FIXME: irrelevant since bison parser generator is used. Refactoring needed.
 * </p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class Value {

    public static class StructKindToken {
        Location location;
        StructKind kind;
    }

    public static class IExpr {
        Expression expr;
        int i;
    }

    public static class IStmt {
        Statement stmt;
        /**
         * <p>
         * Statements counter.
         * </p>
         * TODO
         */
        int i;
    }

    public static class IStmts {
        LinkedList<Statement> stmts;
        int i;
    }
}
