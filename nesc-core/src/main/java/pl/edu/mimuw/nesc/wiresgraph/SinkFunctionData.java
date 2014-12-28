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
     * Unique name of the command or event in the module.
     */
    private final String uniqueName;

    /**
     * Initializes this object by storing the argument in member fields.
     *
     * @param uniqueName Unique name of the command or event.
     * @throws NullPointerException The unique name is <code>null</code>.
     * @throws IllegalArgumentException Unique name is an empty string.
     */
    SinkFunctionData(String uniqueName) {
        checkNotNull(uniqueName, "unique name cannot be null");
        checkArgument(!uniqueName.isEmpty(), "unique name cannot be an empty string");

        this.uniqueName = uniqueName;
    }

    @Override
    public boolean isImplemented() {
        return true;
    }

    /**
     * Get the unique name of the command or event in the module.
     *
     * @return Name of the function with the implementation of the command or
     *         event in the module.
     */
    public String getUniqueName() {
        return uniqueName;
    }
}
