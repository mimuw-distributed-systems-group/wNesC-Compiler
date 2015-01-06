package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.type.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidTaskInterfaceError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_TASK_INTERFACE);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidTaskInterfaceError expectedNonGenericInterface(String name) {
        final String description = format("Task interface '%s' cannot be generic", name);
        return new InvalidTaskInterfaceError(description);
    }

    public static InvalidTaskInterfaceError postTaskCommandAbsent(String interfaceName, String commandName) {
        final String description = format("Task interface '%s' does not contain command '%s' for posting tasks",
                interfaceName, commandName);
        return new InvalidTaskInterfaceError(description);
    }

    public static InvalidTaskInterfaceError runTaskEventAbsent(String interfaceName, String eventName) {
        final String description = format("Task interface '%s' does not contain event '%s' for running tasks",
                interfaceName, eventName);
        return new InvalidTaskInterfaceError(description);
    }

    public static InvalidTaskInterfaceError runTaskIsNotEvent(String interfaceName, String eventName) {
        final String description = format("'%s' in task interface '%s' is not an event", eventName,
                interfaceName);
        return new InvalidTaskInterfaceError(description);
    }

    public static InvalidTaskInterfaceError postTaskIsNotCommand(String interfaceName, String commandName) {
        final String description = format("'%s' in task interface '%s' is not a command", commandName,
                interfaceName);
        return new InvalidTaskInterfaceError(description);
    }

    public static InvalidTaskInterfaceError invalidRunTaskEventType(String interfaceName, String eventName,
            Type expectedType, Type actualType) {

        final String description = format("Type '%s' of '%s' in task interface '%s' differs from the expected type '%s'",
                actualType, eventName, interfaceName, expectedType);
        return new InvalidTaskInterfaceError(description);
    }

    public static InvalidTaskInterfaceError postTaskCommandParametersPresent(String interfaceName,
            String commandName, int paramsCount) {

        final String description = format("Command '%s' in task interface '%s' must not take any parameters but %d parameter(s) %s declared",
                commandName, interfaceName, paramsCount, IssuesUtils.getToBeConjugation(paramsCount));
        return new InvalidTaskInterfaceError(description);
    }

    public static InvalidTaskInterfaceError invalidPostTaskCommandReturnType(String interfaceName,
            String commandName, Type actualReturnType) {
        final String description = format("Command '%s' in task interface '%s' cannot return a value of non-integer type '%s'",
                commandName, interfaceName, actualReturnType);
        return new InvalidTaskInterfaceError(description);
    }

    private InvalidTaskInterfaceError(String description) {
        super(_CODE);

        checkNotNull(description, "description cannot be null");
        checkArgument(!description.isEmpty(), "description cannot be an empty string");

        this.description = description;
    }

    @Override
    public String generateDescription() {
        return description;
    }
}
