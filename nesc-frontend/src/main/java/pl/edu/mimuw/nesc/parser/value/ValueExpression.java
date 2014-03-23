package pl.edu.mimuw.nesc.parser.value;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Expression;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class ValueExpression extends ValueWithLocation {

    private final Expression expression;
    private final int counter;

    public ValueExpression(Location location, Location endLocation, Expression expression, int counter) {
        super(location, endLocation);
        this.expression = expression;
        this.counter = counter;
    }

    public Expression getExpression() {
        return expression;
    }

    public int getCounter() {
        return counter;
    }
}
