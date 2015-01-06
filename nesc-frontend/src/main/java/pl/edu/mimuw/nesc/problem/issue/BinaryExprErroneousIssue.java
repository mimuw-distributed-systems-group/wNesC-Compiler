package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.type.Type;

import static com.google.common.base.Preconditions.checkNotNull;
import static pl.edu.mimuw.nesc.astwriting.Tokens.BinaryOp;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class BinaryExprErroneousIssue extends ErroneousIssue {

    protected final Expression leftExpr, rightExpr;
    protected final Type leftType, rightType;
    protected final BinaryOp op;

    protected BinaryExprErroneousIssue(ErrorCode code, Type leftType, Expression leftExpr,
            BinaryOp op, Type rightType, Expression rightExpr) {
        super(code);

        checkNotNull(leftType, "type of the left expression cannot be null");
        checkNotNull(leftExpr, "left expression cannot be null");
        checkNotNull(op, "binary operator cannot be null");
        checkNotNull(rightType, "type of the right expression cannot be null");
        checkNotNull(rightExpr, "right expression cannot be null");

        this.leftType = leftType;
        this.leftExpr = leftExpr;
        this.op = op;
        this.rightType = rightType;
        this.rightExpr = rightExpr;
    }
}
