package pl.edu.mimuw.nesc.problem.issue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidSchedulerError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_SCHEDULER);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidSchedulerError componentExpected(String schedulerName) {
        final String description = format("Scheduler '%s' is not a component; the scheduler must be a component",
                schedulerName);
        return new InvalidSchedulerError(description);
    }

    public static InvalidSchedulerError providedInterfaceExpected(String schedulerName, String interfaceName) {
        final String description = format("Task interface '%s' of scheduler component '%s' must be provided but it is used",
                interfaceName, schedulerName);
        return new InvalidSchedulerError(description);
    }

    public static InvalidSchedulerError invalidInterfaceProvided(String schedulerName,
            String interfaceAlias, String providedInterfaceName, String expectedInterfaceName) {

        final String description = format("Interface '%s' of scheduler component '%s' does not refer to interface '%s' but it references interface '%s'",
                interfaceAlias, schedulerName, expectedInterfaceName, providedInterfaceName);
        return new InvalidSchedulerError(description);
    }

    public static InvalidSchedulerError parameterisedInterfaceExpected(String schedulerName,
            String interfaceName) {

        final String description = format("Interface '%s' of scheduler component '%s' must be parameterised",
                interfaceName, schedulerName);
        return new InvalidSchedulerError(description);
    }

    public static InvalidSchedulerError invalidInterfaceParamsCount(String schedulerName,
            String interfaceName, int actualArgsCount, int expectedArgsCount) {

        final String description = format("Parameterised interface '%s' of scheduler component '%s' must have exactly %d parameter(s) but %d %s present",
                interfaceName, schedulerName, expectedArgsCount, actualArgsCount, IssuesUtils.getToBeConjugation(actualArgsCount));
        return new InvalidSchedulerError(description);
    }

    public static InvalidSchedulerError absentTaskInterfaceInScheduler(String schedulerName,
            String interfaceName) {
        final String description = format("Scheduler component '%s' does not contain a parameterised interface '%s' in its specification",
                schedulerName, interfaceName);
        return new InvalidSchedulerError(description);
    }

    public static InvalidSchedulerError configurationWithGenericModuleWithTask(String schedulerName,
            String moduleName) {
        final String description = format("Scheduler component '%s' cannot instantiate component '%s' because it contains a task",
                schedulerName, moduleName);
        return new InvalidSchedulerError(description);
    }

    private InvalidSchedulerError(String description) {
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
