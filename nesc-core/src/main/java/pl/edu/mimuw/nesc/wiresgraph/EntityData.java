package pl.edu.mimuw.nesc.wiresgraph;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Information about the command or event that corresponds to a node in the
 * wires graph.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class EntityData {
    /**
     * Unique name of the function that is the implementation of the command
     * or event.
     */
    private final String uniqueName;

    protected EntityData(String uniqueName) {
        checkNotNull(uniqueName, "unique name cannot be null");
        checkArgument(!uniqueName.isEmpty(), "unique name cannot be an empty string");

        this.uniqueName = uniqueName;
    }

    /**
     * Check if the command or event is a provided command or event by a module.
     *
     * @return <code>true</code> if and only if the command or event is provided
     *         by a module. If so it can be safely casted to
     *         {@link SinkFunctionData}. Otherwise, it will be
     *         {@link IntermediateFunctionData}.
     */
    public abstract boolean isImplemented();

    /**
     * Get the unique name of the function that is the implementation of the
     * command or event.
     *
     * @return Unique name of the function that corresponds to this command or
     *         event (either a name of an intermediate function or a direct
     *         implementation of a provided command or event in a module).
     */
    public String getUniqueName() {
        return uniqueName;
    }
}
