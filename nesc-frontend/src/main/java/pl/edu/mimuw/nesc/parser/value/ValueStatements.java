package pl.edu.mimuw.nesc.parser.value;

import pl.edu.mimuw.nesc.ast.gen.Statement;

import java.util.LinkedList;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class ValueStatements extends Value {

    private final LinkedList<Statement> statements;
    // TODO: explain counter
    private final int counter;

    public ValueStatements(LinkedList<Statement> statements, int counter) {
        this.statements = statements;
        this.counter = counter;
    }

    public LinkedList<Statement> getStatements() {
        return statements;
    }

    public int getCounter() {
        return counter;
    }
}
