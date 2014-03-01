package pl.edu.mimuw.nesc.parser.value;

import pl.edu.mimuw.nesc.ast.Location;

/**
 * Value representing result of grammar production returning boolean value.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class ValueBoolean extends Value {

    private final boolean value;

    public ValueBoolean(Location location, Location endLocation, boolean value) {
        super(location, endLocation);
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }
}
