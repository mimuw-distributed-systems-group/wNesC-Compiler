package pl.edu.mimuw.nesc.parser.value;

import pl.edu.mimuw.nesc.ast.Location;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class ValueWithLocation extends Value {

    protected final Location location;
    protected final Location endLocation;

    public ValueWithLocation(Location location, Location endLocation) {
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
