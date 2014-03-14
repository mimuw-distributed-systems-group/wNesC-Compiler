package pl.edu.mimuw.nesc.parser.value;

import pl.edu.mimuw.nesc.ast.LeftUnaryOperation;
import pl.edu.mimuw.nesc.ast.Location;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Value representing result of grammar production returning type of left
 * unary operation (such as preincrementation, negation, etc.).
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class ValueLeftUnaryOp extends Value {

    private final LeftUnaryOperation operation;

    public ValueLeftUnaryOp(Location location, Location endLocation, LeftUnaryOperation operation) {
        super(location, endLocation);
        checkNotNull(operation, "operation cannot be null");
        this.operation = operation;
    }

    public LeftUnaryOperation getOperation() {
        return operation;
    }
}
