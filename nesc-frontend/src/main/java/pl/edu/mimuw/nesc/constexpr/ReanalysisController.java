package pl.edu.mimuw.nesc.constexpr;

import pl.edu.mimuw.nesc.ast.gen.*;

/**
 * <p>Class responsible for checking if re-analysis of expressions is necessary
 * for their correct evaluation. It follows the singleton pattern.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ReanalysisController {
    /**
     * The only instance of the visitor.
     */
    private static final ReanalysisController INSTANCE = new ReanalysisController();

    /**
     * Get the only instance of this class.
     *
     * @return The only instance of this class.
     */
    public static ReanalysisController getInstance() {
        return INSTANCE;
    }

    /**
     * Private constructor for the singleton pattern.
     */
    private ReanalysisController() {
    }

    /**
     * Check if the re-analysis of the given expression is necessary for its
     * correct evaluation.
     *
     * @param expr Constant expression to check.
     * @return <code>true</code> if and only if the re-analysis is necessary.
     * @throws IllegalArgumentException Given expression is not a constant
     *                                  expression.
     */
    public boolean isReanalysisNecessary(Expression expr) {
        if (expr instanceof Assignment
                || expr instanceof Increment
                || expr instanceof FunctionCall && !(expr instanceof ConstantFunctionCall)
                || expr instanceof Comma
                || expr instanceof InitSpecific
                || expr instanceof InitList
                || expr instanceof ArrayRef
                || expr instanceof CastList
                || expr instanceof CompoundExpr
                || expr instanceof Dereference
                || expr instanceof AddressOf
                || expr instanceof FieldRef
                || expr instanceof LabelAddress
                || expr instanceof GenericCall
                || expr instanceof InterfaceDeref
                || expr instanceof ComponentDeref
                || expr instanceof TypeArgument
                || expr instanceof ErrorExpr) {
            throw new IllegalArgumentException("expression of class '"
                    + expr.getClass().getCanonicalName()
                    + "' is not a constant expression");
        } else if (!expr.getType().get().isFullyKnown()) {
            return true;
        } else if (expr instanceof SizeofExpr) {
            return isReanalysisNecessary((SizeofExpr) expr);
        } else if (expr instanceof AlignofExpr) {
            return isReanalysisNecessary((AlignofExpr) expr);
        } else if (expr instanceof Binary) {
            return isReanalysisNecessary((Binary) expr);
        } else if (expr instanceof Unary) {
            return isReanalysisNecessary((Unary) expr);
        } else if (expr instanceof Conditional) {
            return isReanalysisNecessary((Conditional) expr);
        } else if (expr instanceof ConstantFunctionCall) {
            return isReanalysisNecessary((ConstantFunctionCall) expr);
        } else {
            return false;
        }
    }

    private boolean isReanalysisNecessary(Binary expr) {
        return isReanalysisNecessary(expr.getLeftArgument())
                || isReanalysisNecessary(expr.getRightArgument());
    }

    private boolean isReanalysisNecessary(Unary expr) {
        return isReanalysisNecessary(expr.getArgument());
    }

    private boolean isReanalysisNecessary(Conditional expr) {
        if (isReanalysisNecessary(expr.getCondition())) {
            return true;
        } else if (isReanalysisNecessary(expr.getOnFalseExp())) {
            return true;
        } else if (expr.getOnTrueExp().isPresent() && isReanalysisNecessary(expr.getOnTrueExp().get())) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isReanalysisNecessary(ConstantFunctionCall expr) {
        if (isReanalysisNecessary(expr.getFunction())) {
            return true;
        }

        for (Expression param : expr.getArguments()) {
            if (isReanalysisNecessary(param)) {
                return true;
            }
        }

        return false;
    }

    private boolean isReanalysisNecessary(SizeofExpr expr) {
        return !expr.getArgument().getType().get().isFullyKnown();
    }

    private boolean isReanalysisNecessary(AlignofExpr expr) {
        return !expr.getArgument().getType().get().isFullyKnown();
    }
}
