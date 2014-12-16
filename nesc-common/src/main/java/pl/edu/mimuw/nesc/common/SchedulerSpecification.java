package pl.edu.mimuw.nesc.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The specification of the scheduler to use for running tasks.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class SchedulerSpecification {
    private final String componentName;
    private final String uniqueIdentifier;
    private final String interfaceNameInScheduler;
    private final String taskInterfaceName;
    private final String taskRunEventName;
    private final String taskPostCommandName;

    /**
     * <p>Parses the given scheduler specification string and initializes this
     * object to contain the information from it.</p>
     *
     * @param specification String with the specification of the scheduler as in
     *                      the options for the compiler.
     * @throws NullPointerException The specification string is
     *                              <code>null</code>.
     * @throws IllegalArgumentException The specification string is empty or
     *                                  does not contain six non-empty
     *                                  comma-separated values.
     */
    public SchedulerSpecification(String specification) {
        checkNotNull(specification, "the specification string cannot be null");
        checkArgument(!specification.isEmpty(), "the specification string cannot be empty");

        final String[] values = specification.split(",");

        checkArgument(values.length == 6, "the specification string does not contain exactly six values");
        checkArgument(!values[0].isEmpty(), "component name in the specification is empty");
        checkArgument(!values[1].isEmpty(), "identifier for 'unqiue' in the specification is empty");
        checkArgument(!values[2].isEmpty(), "scheduler interface name in the specification is empty");
        checkArgument(!values[3].isEmpty(), "task interface name in the specification is empty");
        checkArgument(!values[4].isEmpty(), "name of the event for running tasks in the specification is empty");
        checkArgument(!values[5].isEmpty(), "name of the command for posting tasks in the specification is empty");

        this.componentName = values[0];
        this.uniqueIdentifier = values[1];
        this.interfaceNameInScheduler = values[2];
        this.taskInterfaceName = values[3];
        this.taskRunEventName = values[4];
        this.taskPostCommandName = values[5];
    }

    public String getComponentName() {
        return componentName;
    }

    public String getUniqueIdentifier() {
        return uniqueIdentifier;
    }

    public String getInterfaceNameInScheduler() {
        return interfaceNameInScheduler;
    }

    public String getTaskInterfaceName() {
        return taskInterfaceName;
    }

    public String getTaskRunEventName() {
        return taskRunEventName;
    }

    public String getTaskPostCommandName() {
        return taskPostCommandName;
    }
}
