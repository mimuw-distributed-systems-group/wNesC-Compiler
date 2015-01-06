package pl.edu.mimuw.nesc.problem.issue;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidNescCallError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_NESC_CALL);
    public static final Code CODE = _CODE;

    private final String msg;

    public static InvalidNescCallError invalidCallee(Expression funExpr) {
        checkNotNull(funExpr, "function expression cannot be null");

        final String msg = format("'%s' does not denote a command or an event",
                ASTWriter.writeToString(funExpr));

        return new InvalidNescCallError(msg);
    }

    public static InvalidNescCallError nonexistentInterfaceEntity(String methodName,
            String instanceName, String interfaceName) {

        final String msg = instanceName.equals(interfaceName)
            ? format("Interface '%s' does not contain command or event '%s'",
                     interfaceName, methodName)
            : format("Interface '%s' instantiated as '%s' does not contain command or event '%s'",
                     interfaceName, instanceName, methodName);

        return new InvalidNescCallError(msg);
    }

    public static InvalidNescCallError missingInstanceParameters(boolean isEvent, Expression funExpr) {
        final String msg = format("'%s' is a parameterised %s but instance parameters are not provided",
                ASTWriter.writeToString(funExpr), getEntity(isEvent));
        return new InvalidNescCallError(msg);
    }

    public static InvalidNescCallError unexpectedInstanceParameters(String identifier,
            Optional<String> methodName) {

        final String msg = methodName.isPresent()
                ? format("Interface '%s' is not parameterised but instance parameters are provided",
                         identifier)
                : format("'%s' is not a parameterised command or event but instance parameters are provided",
                         identifier);

        return new InvalidNescCallError(msg);
    }

    public static InvalidNescCallError invalidInstanceParametersCount(boolean isEvent,
            String parameterisedEntity, int expectedCount, int actualCount) {

        final String fmt = actualCount == 1
                ? "%s '%s' requires %d instance parameter(s) but %d is provided"
                : "%s '%s' requires %s instance parameter(s) but %d are provided";

        final String msg = format(fmt, getEntity(isEvent, true), parameterisedEntity,
                expectedCount, actualCount);

        return new InvalidNescCallError(msg);
    }

    public static InvalidNescCallError invalidNormalParametersCount(boolean isEvent,
            Expression funExpr, int expectedCount, int actualCount) {

        final String fmt = actualCount == 1
                ? "%s '%s' requires %d parameter(s) but %d is provided"
                : "%s '%s' requires %s parameter(s) but %d are provided";

        final String msg = format(fmt, getEntity(isEvent, true), ASTWriter.writeToString(funExpr),
                expectedCount, actualCount);

        return new InvalidNescCallError(msg);
    }

    public static InvalidNescCallError invalidCallKind(boolean isEvent, Expression funExpr) {
        final String fmt = isEvent
                ? "'%s' cannot be called because it is an event"
                : "'%s' cannot be signaled because it is a command";

        final String msg = format(fmt, ASTWriter.writeToString(funExpr));
        return new InvalidNescCallError(msg);
    }

    private static String getEntity(boolean isEvent) {
        return getEntity(isEvent, false);
    }

    private static String getEntityA(boolean isEvent) {
        return getEntityA(isEvent, false);
    }

    private static String getEntity(boolean isSignal, boolean firstLetterCapital) {
        if (isSignal) {
            return firstLetterCapital
                    ? "Event"
                    : "event";
        } else {
            return firstLetterCapital
                    ? "Command"
                    : "command";
        }
    }

    private static String getEntityA(boolean isSignal, boolean firstLetterCapital) {
        if (isSignal) {
            return firstLetterCapital
                    ? "An event"
                    : "an event";
        } else {
            return firstLetterCapital
                    ? "A command"
                    : "a command";
        }
    }

    private InvalidNescCallError(String msg) {
        super(_CODE);
        this.msg = msg;
    }

    @Override
    public String generateDescription() {
        return msg;
    }
}
