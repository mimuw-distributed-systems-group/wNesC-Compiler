package pl.edu.mimuw.nesc.parser.value;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.StructKind;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class ValueStructKind extends ValueWithLocation {

    private final StructKind kind;

    public ValueStructKind(Location location, Location endLocation, StructKind kind) {
        super(location, endLocation);
        this.kind = kind;
    }

    public StructKind getKind() {
        return kind;
    }
}
