package pl.edu.mimuw.nesc.parser.value;

import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.NescCallKind;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class ValueCallKind extends ValueWithLocation {

    private final NescCallKind callKind;


    public ValueCallKind(Location location, Location endLocation, NescCallKind callKind) {
        super(location, endLocation);
        checkNotNull(callKind, "call kind cannot be null");
        this.callKind = callKind;
    }

    public NescCallKind getCallKind() {
        return callKind;
    }
}
