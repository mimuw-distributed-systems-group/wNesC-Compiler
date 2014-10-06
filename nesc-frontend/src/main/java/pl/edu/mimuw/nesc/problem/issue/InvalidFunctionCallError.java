package pl.edu.mimuw.nesc.problem.issue;

import java.util.LinkedList;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.type.FunctionType;
import pl.edu.mimuw.nesc.ast.type.PointerType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.util.PrettyPrint;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidFunctionCallError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_FUNCTION_CALL);
    public static final Code CODE = _CODE;

    /**
     * Expression that is called.
     */
    private final Expression funExpr;

    /**
     * Type of the function expression (the expression before parameters).
     */
    private final Type funExprType;

    /**
     * Count of parameters in function declaration without variable arguments.
     */
    private final int expectedParamsCount;

    /**
     * Count of provided parameters (with variable arguments).
     */
    private final int actualParamsCount;

    /**
     * <code>true</code> if and only if the called function has variable number
     * of arguments (ellipsis is used).
     */
    private final boolean variableArgumentsFunction;

    public static Builder builder() {
        return new Builder();
    }

    private InvalidFunctionCallError(Builder builder) {
        super(_CODE);

        this.funExpr = builder.funExpr;
        this.funExprType = builder.funExprType;
        this.expectedParamsCount = builder.expectedParamsCount;
        this.actualParamsCount = builder.actualParamsCount;
        this.variableArgumentsFunction = builder.variableArgumentsFunction;
    }

    @Override
    public String generateDescription() {
        if (!funExprType.isPointerType()) {
            return format("Called expression '%s' has type '%s' but expecting a function or a pointer to function",
                          PrettyPrint.expression(funExpr), funExprType);
        }

        final Type refType = ((PointerType) funExprType).getReferencedType();

        if (!refType.isFunctionType()) {
            return format("Called expression '%s' is a pointer to '%s' but expecting a pointer to a function",
                          PrettyPrint.expression(funExpr), refType);
        }

        final FunctionType funType = (FunctionType) refType;
        final Type retType = funType.getReturnType();

        if (!retType.isVoid()) {
            if (!retType.isComplete()) {
                return format("Cannot call function '%s' because it returns incomplete type '%s'",
                        PrettyPrint.expression(funExpr), retType);
            } else if (!retType.isObjectType()) {
                return format("Cannot call function '%s' because it does not return an object type",
                        PrettyPrint.expression(funExpr));
            }
        }

        if (!variableArgumentsFunction && actualParamsCount != expectedParamsCount) {
            return format("Function '%s' expects %d parameters but %d were provided",
                          PrettyPrint.expression(funExpr), expectedParamsCount, actualParamsCount);
        } else if (variableArgumentsFunction && actualParamsCount < expectedParamsCount) {
            return format("Function '%s' expects at least %d parameters but %d were provided",
                          PrettyPrint.expression(funExpr), expectedParamsCount, actualParamsCount);
        }

        return format("Invalid function call '%s'", PrettyPrint.expression(funExpr));
    }

    /**
     * Builder for this kind of error.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static class Builder {
        /**
         * Data needed to build a function call error object.
         */
        private int expectedParamsCount;
        private int actualParamsCount;
        private boolean variableArgumentsFunction;
        private Expression funExpr;
        private Type funExprType;

        private Builder() {
        }

        public Builder expectedParamsCount(int expectedParamsCount) {
            this.expectedParamsCount = expectedParamsCount;
            return this;
        }

        public Builder actualParamsCount(int actualParamsCount) {
            this.actualParamsCount = actualParamsCount;
            return this;
        }

        public Builder variableArgumentsFunction(boolean variableArgumentsFunction) {
            this.variableArgumentsFunction = variableArgumentsFunction;
            return this;
        }

        public Builder funExpr(Type type, Expression expr) {
            this.funExpr = expr;
            this.funExprType = type;
            return this;
        }

        private void validate() {
            checkNotNull(funExpr, "function expression cannot be null");
            checkNotNull(funExprType, "type of the function expression cannot be null");
            checkState(expectedParamsCount >= 0, "expected parameters count must be non-negative");
            checkState(actualParamsCount >= 0, "actual parameters count must be non-negative");
        }

        public InvalidFunctionCallError build() {
            validate();
            return new InvalidFunctionCallError(this);
        }
    }
}
