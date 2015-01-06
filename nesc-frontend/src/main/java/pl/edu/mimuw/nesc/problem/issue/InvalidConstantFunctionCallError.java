package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static pl.edu.mimuw.nesc.astwriting.Tokens.ConstantFun;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidConstantFunctionCallError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_CONSTANT_FUNCTION_CALL);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidConstantFunctionCallError invalidCallKind(ConstantFun fun) {
        final String description = format("Constant function '%s' cannot be invoked with 'post', 'signal' or 'call'", fun);
        return new InvalidConstantFunctionCallError(description);
    }

    public static InvalidConstantFunctionCallError invalidParametersCount(ConstantFun fun, int actualParamsCount,
            int expectedParamsCount) {

        final String description = format("Constant function '%s' expects %d parameter(s) but %d provided",
                fun, expectedParamsCount, actualParamsCount);
        return new InvalidConstantFunctionCallError(description);
    }

    public static InvalidConstantFunctionCallError invalidIdentifierType(ConstantFun fun, Type providedType) {
        final String description = format("Constant function '%s' expects a constant string as the 1st parameter but expression of type '%s' provided",
                fun, providedType);
        return new InvalidConstantFunctionCallError(description);
    }

    public static InvalidConstantFunctionCallError nonConstantIdentifier(ConstantFun fun, Expression providedExpr) {
        final String description = format("Constant function '%s' expects a constant string as the 1st parameter but non-constant string '%s' provided",
                fun, ASTWriter.writeToString(providedExpr));
        return new InvalidConstantFunctionCallError(description);
    }

    public static InvalidConstantFunctionCallError invalidNumbersCountType(Type providedType) {
        final String description = format("Constant function 'uniqueN' expects an integer constant as the 2nd parameter but expression of type '%s' provided",
                providedType);
        return new InvalidConstantFunctionCallError(description);
    }

    public static InvalidConstantFunctionCallError unsupportedIntegerExpression(Expression providedExpr) {
        final String msg = format("Unsupported expression '%s' for the 2nd parameter of 'uniqueN'",
                ASTWriter.writeToString(providedExpr));
        final String notice = format("implement checking and evaluating constant expressions and change class '%s' and method '%s'",
                "pl.edu.mimuw.nesc.fold.UniqueValuesProcessor", "pl.edu.mimuw.nesc.analysis.ExpressionsAnalysis.checkNumbersCountForUniqueN");
        final String description = format("%s; %s", msg, notice);
        return new InvalidConstantFunctionCallError(description);
    }

    private InvalidConstantFunctionCallError(String description) {
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
