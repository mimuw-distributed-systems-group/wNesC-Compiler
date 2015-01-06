package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.astbuilding.Declarations;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidPostTaskExprError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_POST_TASK_EXPR);
    public static final Code CODE = _CODE;

    private final PostProblemKind problemKind;
    private final Expression functionExpr;
    private final int argumentsCount;

    public InvalidPostTaskExprError(PostProblemKind kind, Expression functionExpr,
            int argumentsCount) {
        super(_CODE);

        checkNotNull(kind, "kind of the problem cannot be null");
        checkNotNull(functionExpr, "the function expression cannot be null");
        checkArgument(argumentsCount >= 0, "arguments count cannot be negative");

        this.problemKind = kind;
        this.functionExpr = functionExpr;
        this.argumentsCount = argumentsCount;
    }

    @Override
    public String generateDescription() {

        switch (problemKind) {
            case IDENTIFER_NOT_PROVIDED:
                return format("Posting a task requires an identifier but '%s' encountered",
                        ASTWriter.writeToString(functionExpr));
            case PARAMETERS_GIVEN:
                return format("Tasks do not take any parameters but %d arguments given",
                        argumentsCount);
            case INVALID_OBJECT_REFERENCED:
                return format("Cannot post '%s' because it is not a task",
                        ASTWriter.writeToString(functionExpr));
            case INVALID_TASK_TYPE:
                return format("Task '%s' is not of type '%s'; correct its declaration and/or definition",
                        ASTWriter.writeToString(functionExpr), Declarations.TYPE_TASK);
            default:
                return "Invalid task posting expression";
        }

    }

    public enum PostProblemKind {
        IDENTIFER_NOT_PROVIDED,
        PARAMETERS_GIVEN,
        INVALID_OBJECT_REFERENCED,
        INVALID_TASK_TYPE,
    }
}
