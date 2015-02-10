package pl.edu.mimuw.nesc.analysis.expressions;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.ast.gen.ComponentDeref;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.FunctionCall;
import pl.edu.mimuw.nesc.ast.gen.Identifier;
import pl.edu.mimuw.nesc.ast.gen.Offsetof;
import pl.edu.mimuw.nesc.problem.ErrorHelper;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Shortened analysis of expressions. It is intended to determine exact types
 * of expressions after instantiation of components. Types of of expressions
 * that are normally retrieved from the environment shall be set in expressions
 * AST nodes.</p>
 *
 * <p>An exception is thrown in the case a type is read from the AST node and it
 * is an unknown type or it is absent.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ShortenedExpressionsAnalysis extends ExpressionsAnalysis {
    /**
     * Performs the analysis of the given expression again. It should have been
     * analysed before by {@link FullExpressionsAnalysis}. It is assumed that
     * no identifiers, NesC calls, post task expressions and component
     * dereferences have set an unknown type.
     *
     * @param expr Expression to be analyzed again.
     * @param errorHelper Error helper that will be notified about detected
     *                    errors.
     * @return Object that depicts the given expression - the result of
     *         analysis.
     * @throws NullPointerException One of the arguments is <code>null</code>.
     * @throws IllegalArgumentException The given expression has errors.
     */
    public static ExprData analyze(Expression expr, ABI abi, ErrorHelper errorHelper) {
        checkNotNull(expr, "expression cannot be null");
        checkNotNull(abi, "ABI cannot be null");
        checkNotNull(errorHelper, "error helper cannot be null");

        final ExpressionsAnalysis analysisVisitor = new ShortenedExpressionsAnalysis(abi, errorHelper);
        final Optional<ExprData> result = expr.accept(analysisVisitor, null);
        checkArgument(result.isPresent(), "the analysis of the expression failed");

        return result.get();
    }

    private ShortenedExpressionsAnalysis(ABI abi, ErrorHelper errorHelper) {
        super(abi, errorHelper);
    }

    @Override
    public Optional<ExprData> visitIdentifier(Identifier identifier, Void arg) {
        checkType(identifier);

        final ExprData result = ExprData.builder()
                .type(identifier.getType().get())
                .isLvalue(true)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build();

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitComponentDeref(ComponentDeref deref, Void arg) {
        checkType(deref);

        final ExprData result = ExprData.builder()
                .type(deref.getType().get())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build();

        return Optional.of(result);
    }

    @Override
    public Optional<ExprData> visitOffsetof(Offsetof expr, Void arg) {
        checkType(expr);

        final ExprData result = ExprData.builder()
                .type(expr.getType().get())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build();

        return Optional.of(result);
    }

    @Override
    protected Optional<ExprData> analyzeNescCall(FunctionCall funCall, boolean isSignal) {
        return extractFromNescCall(funCall);
    }

    @Override
    protected Optional<ExprData> analyzePostTask(FunctionCall funCall) {
        return extractFromNescCall(funCall);
    }

    private Optional<ExprData> extractFromNescCall(FunctionCall funCall) {
        checkType(funCall);

        final ExprData result = ExprData.builder()
                .type(funCall.getType().get())
                .isLvalue(false)
                .isBitField(false)
                .isNullPointerConstant(false)
                .build();

        return Optional.of(result);
    }

    private void checkType(Expression expr) {
        checkArgument(expr.getType() != null, "an expression has null type");
        checkArgument(expr.getType().isPresent(), "the type of an expression is absent");
        checkArgument(!expr.getType().get().isUnknownType(),
                "an expression has set an unknown type");
    }
}
