package pl.edu.mimuw.nesc.wiresgraph;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Data about a provided command or event from a module. Node in the wires
 * graph that corresponds to such command or event is a sink.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class SinkFunctionData extends EntityData {
    /**
     * Initializes this object by storing the argument in member fields.
     *
     * @param uniqueName Unique name of the command or event.
     * @throws NullPointerException The unique name is <code>null</code>.
     * @throws IllegalArgumentException Unique name is an empty string.
     */
    SinkFunctionData(String uniqueName) {
        super(uniqueName);
    }

    @Override
    public boolean isImplemented() {
        return true;
    }
}
