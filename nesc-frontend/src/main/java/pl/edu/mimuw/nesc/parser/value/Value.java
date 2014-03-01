package pl.edu.mimuw.nesc.parser.value;

import pl.edu.mimuw.nesc.ast.Location;

/**
 * Base class that represents value used in parser. It does not represents
 * any AST node, but contains result of "intermediate" grammar rules.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class Value {

    protected final Location location;
    protected final Location endLocation;

    public Value(Location location, Location endLocation) {
        this.location = location;
        this.endLocation = endLocation;
    }

    public Location getLocation() {
        return location;
    }

    public Location getEndLocation() {
        return endLocation;
    }
}
