package pl.edu.mimuw.nesc.analysis;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.problem.ErrorHelper;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that is responsible for analysis of expressions.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class ExpressionsAnalysis extends ExceptionVisitor<Optional<ExprData>, Void> {
    /**
     * Environment of the expression that is analyzed.
     */
    private final Environment environment;

    /**
     * Object that will be notified about detected problems.
     */
    private final ErrorHelper errorHelper;

    /**
     * Analyze the given expression and report all detected errors to the given
     * error helper.
     *
     * @param expr Expression to be analyzed.
     * @param environment Environment of the expression.
     * @param errorHelper Object that will be notified about detected problems.
     * @return Data about the given expression. The object is present if and
     *         only if the expression is valid, otherwise it is absent.
     */
    public static Optional<ExprData> analyze(Expression expr, Environment environment,
            ErrorHelper errorHelper) {
        checkNotNull(expr, "expression cannot be null");
        checkNotNull(environment, "environment cannot be null");
        checkNotNull(errorHelper, "error helper cannot be null");

        final ExpressionsAnalysis analysisVisitor = new ExpressionsAnalysis(environment, errorHelper);
        return expr.accept(analysisVisitor, null);
    }

    /**
     * Private constructor to prevent this class from being instantiated.
     *
     * @param environment Environment of the analyzed expression.
     * @param errorHelper Object that will be notified about detected problems.
     */
    private ExpressionsAnalysis(Environment environment, ErrorHelper errorHelper) {
        this.environment = environment;
        this.errorHelper = errorHelper;
    }

    @Override
    public Optional<ExprData> visitPlus(Plus expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitMinus(Minus expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitTimes(Times expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitDivide(Divide expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitModulo(Modulo expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitLshift(Lshift expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitRshift(Rshift expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitLeq(Leq expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitGeq(Geq expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitLt(Lt expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitGt(Gt expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitEq(Eq expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitNe(Ne expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitBitand(Bitand expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitBitor(Bitor expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitBitxor(Bitxor expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitAndand(Andand expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitOror(Oror expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitAssign(Assign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitPlusAssign(PlusAssign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitMinusAssign(MinusAssign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitTimesAssign(TimesAssign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitDivideAssign(DivideAssign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitModuloAssign(ModuloAssign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitLshiftAssign(LshiftAssign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitRshiftAssign(RshiftAssign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitBitandAssign(BitandAssign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitBitorAssign(BitorAssign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitBitxorAssign(BitxorAssign expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitUnaryMinus(UnaryMinus expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitDereference(Dereference expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitAddressOf(AddressOf expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitUnaryPlus(UnaryPlus expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitBitnot(Bitnot expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitNot(Not expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitAlignofType(AlignofType expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitSizeofType(SizeofType expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitSizeofExpr(SizeofExpr expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitAlignofExpr(AlignofExpr expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitRealpart(Realpart expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitImagpart(Imagpart expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitArrayRef(ArrayRef expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitErrorExpr(ErrorExpr expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitComma(Comma expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitLabelAddress(LabelAddress expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitConditional(Conditional expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitIdentifier(Identifier expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitCompoundExpr(CompoundExpr expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitIntegerCst(IntegerCst expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitFloatingCst(FloatingCst expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitCharacterCst(CharacterCst expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitStringCst(StringCst expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitStringAst(StringAst expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitFunctionCall(FunctionCall expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitFieldRef(FieldRef expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitInterfaceDeref(InterfaceDeref expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitComponentDeref(ComponentDeref expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitPreincrement(Preincrement expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitPredecrement(Predecrement expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitPostincrement(Postincrement expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitPostdecrement(Postdecrement expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitCast(Cast expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitCastList(CastList expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitInitList(InitList expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitInitSpecific(InitSpecific expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitTypeArgument(TypeArgument expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitGenericCall(GenericCall expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitExtensionExpr(ExtensionExpr expr, Void arg) {
        // FIXME
        return Optional.absent();
    }

    @Override
    public Optional<ExprData> visitConjugate(Conjugate expr, Void arg) {
        // FIXME
        return Optional.absent();
    }
}
