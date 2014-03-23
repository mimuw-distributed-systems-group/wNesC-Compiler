package pl.edu.mimuw.nesc.parser.value;

import pl.edu.mimuw.nesc.ast.gen.Statement;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class ValueStatement extends Value {

    private final Statement statement;
    // TODO: explain counter
    private int counter;

    public ValueStatement(Statement statement, int counter) {
        this.statement = statement;
        this.counter = counter;
    }

    public Statement getStatement() {
        return statement;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }
}
