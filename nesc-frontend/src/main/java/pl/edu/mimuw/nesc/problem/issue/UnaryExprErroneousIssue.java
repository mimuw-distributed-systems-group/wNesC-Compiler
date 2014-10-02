package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.util.AstConstants.UnaryOp;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class UnaryExprErroneousIssue extends ErroneousIssue {

    protected final UnaryOp op;
    protected final Type argType;
    protected final Expression argExpr;

    protected UnaryExprErroneousIssue(ErrorCode code, UnaryOp op, Type argType, Expression argExpr) {
        super(code);

        checkNotNull(op, "unary operator cannot be null");
        checkNotNull(argType, "type of the argument cannot be null");
        checkNotNull(argExpr, "expression used as the argument for an unary operator cannot be null");

        this.argExpr = argExpr;
        this.argType = argType;
        this.op = op;
    }
}
