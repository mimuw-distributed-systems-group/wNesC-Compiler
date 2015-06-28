package pl.edu.mimuw.nesc.astutil;

import java.util.HashSet;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.gen.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A decorator visitor that counts loops and conditional statements that enclose
 * visited nodes.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class CountingDecoratorVisitor<T> extends DecoratorVisitor<CountingDecoratorVisitor.CountingOracle<T>,
        CountingDecoratorVisitor.CountingOracle<T>> {
    /**
     * Set with expressions that need decreasing the enclosing loops count.
     * Such expressions are initialization expressions of for loops.
     */
    private final Set<Expression> enclosingLoopsCountDecrementExprs;

    /**
     * Set with expressions that need decreasing the enclosing conditional
     * statements count. Such expressions are conditions of if statements,
     * conditions of switch statements and conditions of the conditional
     * expressions.
     */
    private final Set<Expression> enclosingCondStmtsCountDecrementExprs;

    public CountingDecoratorVisitor(IdentityVisitor<CountingOracle<T>> decoratedVisitor) {
        super(decoratedVisitor);
        this.enclosingLoopsCountDecrementExprs = new HashSet<>();
        this.enclosingCondStmtsCountDecrementExprs = new HashSet<>();
    }

    @Override
    public CountingOracle<T> visitSizeofExpr(SizeofExpr expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitSizeofExpr(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitAlignofExpr(AlignofExpr expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitAlignofExpr(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitSizeofType(SizeofType expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitSizeofType(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitAlignofType(AlignofType expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitAlignofType(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitRealpart(Realpart expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitRealpart(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitIdentifier(Identifier expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitIdentifier(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitFunctionCall(FunctionCall expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitFunctionCall(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitImagpart(Imagpart expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitImagpart(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitAddressOf(AddressOf expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitAddressOf(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitUnaryMinus(UnaryMinus expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitUnaryMinus(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitUnaryPlus(UnaryPlus expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitUnaryPlus(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitConjugate(Conjugate expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitConjugate(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitBitnot(Bitnot expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitBitnot(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitNot(Not expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitNot(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitPreincrement(Preincrement expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitPreincrement(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitPredecrement(Predecrement expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitPredecrement(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitPostincrement(Postincrement expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitPostincrement(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitPostdecrement(Postdecrement expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitPostdecrement(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitPlus(Plus expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitPlus(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitMinus(Minus expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitMinus(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitTimes(Times expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitTimes(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitDivide(Divide expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitDivide(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitModulo(Modulo expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitModulo(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitLshift(Lshift expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitLshift(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitRshift(Rshift expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitRshift(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitLeq(Leq expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitLeq(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitGeq(Geq expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitGeq(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitLt(Lt expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitLt(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitGt(Gt expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitGt(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitEq(Eq expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitEq(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitNe(Ne expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitNe(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitBitand(Bitand expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitBitand(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitBitor(Bitor expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitBitor(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitBitxor(Bitxor expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitBitxor(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitAndand(Andand expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitAndand(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitOror(Oror expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitOror(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitAssign(Assign expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitAssign(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitPlusAssign(PlusAssign expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitPlusAssign(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitMinusAssign(MinusAssign expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitMinusAssign(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitTimesAssign(TimesAssign expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitTimesAssign(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitDivideAssign(DivideAssign expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitDivideAssign(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitModuloAssign(ModuloAssign expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitModuloAssign(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitLshiftAssign(LshiftAssign expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitLshiftAssign(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitRshiftAssign(RshiftAssign expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitRshiftAssign(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitBitandAssign(BitandAssign expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitBitandAssign(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitBitorAssign(BitorAssign expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitBitorAssign(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitBitxorAssign(BitxorAssign expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitBitxorAssign(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitInitList(InitList expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitInitList(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitInitSpecific(InitSpecific expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitInitSpecific(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitIntegerCst(IntegerCst expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitIntegerCst(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitFloatingCst(FloatingCst expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitFloatingCst(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitCharacterCst(CharacterCst expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitCharacterCst(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitStringCst(StringCst expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitStringCst(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitStringAst(StringAst expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitStringAst(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitGenericCall(GenericCall expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitGenericCall(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitInterfaceDeref(InterfaceDeref expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitInterfaceDeref(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitComponentDeref(ComponentDeref expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitComponentDeref(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitOffsetof(Offsetof expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitOffsetof(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitLabelAddress(LabelAddress expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitLabelAddress(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitCast(Cast expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitCast(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitCastList(CastList expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitCastList(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitCompoundExpr(CompoundExpr expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitCompoundExpr(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitUniqueCall(UniqueCall expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitUniqueCall(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitUniqueNCall(UniqueNCall expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitUniqueNCall(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitUniqueCountCall(UniqueCountCall expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitUniqueCountCall(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitArrayRef(ArrayRef expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitArrayRef(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitFieldRef(FieldRef expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitFieldRef(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitDereference(Dereference expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitDereference(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitExtensionExpr(ExtensionExpr expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitExtensionExpr(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitTypeArgument(TypeArgument expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitTypeArgument(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitComma(Comma expr, CountingOracle<T> oracle) {
        return getDecoratedVisitor().visitComma(expr, correctEnclosingCounts(expr, oracle));
    }

    @Override
    public CountingOracle<T> visitForStmt(ForStmt stmt, CountingOracle<T> oracle) {
        oracle = getDecoratedVisitor().visitForStmt(stmt, oracle);
        if (stmt.getInitExpression().isPresent()) {
            enclosingLoopsCountDecrementExprs.add(stmt.getInitExpression().get());
        }
        return oracle.incrementEnclosingLoopsCount();
    }

    @Override
    public CountingOracle<T> visitWhileStmt(WhileStmt stmt, CountingOracle<T> oracle) {
        oracle = getDecoratedVisitor().visitWhileStmt(stmt, oracle);
        return oracle.incrementEnclosingLoopsCount();
    }

    @Override
    public CountingOracle<T> visitDoWhileStmt(DoWhileStmt stmt, CountingOracle<T> oracle) {
        oracle = getDecoratedVisitor().visitDoWhileStmt(stmt, oracle);
        return oracle.incrementEnclosingLoopsCount();
    }

    @Override
    public CountingOracle<T> visitIfStmt(IfStmt stmt, CountingOracle<T> oracle) {
        oracle = getDecoratedVisitor().visitIfStmt(stmt, oracle);
        enclosingCondStmtsCountDecrementExprs.add(stmt.getCondition());
        return oracle.incrementEnclosingConditionalStmtsCount();
    }

    @Override
    public CountingOracle<T> visitSwitchStmt(SwitchStmt stmt, CountingOracle<T> oracle) {
        oracle = getDecoratedVisitor().visitSwitchStmt(stmt, oracle);
        enclosingCondStmtsCountDecrementExprs.add(stmt.getCondition());
        return oracle.incrementEnclosingConditionalStmtsCount();
    }

    @Override
    public CountingOracle<T> visitConditional(Conditional expr, CountingOracle<T> oracle) {
        enclosingCondStmtsCountDecrementExprs.add(expr.getCondition());
        return getDecoratedVisitor().visitConditional(expr, correctEnclosingCounts(expr, oracle))
                .incrementEnclosingConditionalStmtsCount();
    }

    private CountingOracle<T> correctEnclosingCounts(Expression expr, CountingOracle<T> unadjustedOracle) {
        CountingOracle<T> adjustedOracle = unadjustedOracle;
        if (enclosingLoopsCountDecrementExprs.remove(expr)) {
            adjustedOracle = adjustedOracle.decrementEnclosingLoopsCount();
        }
        if (enclosingCondStmtsCountDecrementExprs.remove(expr)) {
            adjustedOracle = adjustedOracle.decrementEnclosingConditionalStmtsCount();
        }
        return adjustedOracle;
    }

    /**
     * Oracle with information about enclosing of the current node.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static class CountingOracle<T> {
        private final T decoratedOracle;
        private final int enclosingLoopsCount;
        private final int enclosingConditionalStmtsCount;

        public CountingOracle(T decoratedOracle) {
            checkNotNull(decoratedOracle, "decorated oracle cannot be null");
            this.decoratedOracle = decoratedOracle;
            this.enclosingLoopsCount = 0;
            this.enclosingConditionalStmtsCount = 0;
        }

        private CountingOracle(T decoratedOracle, int enclosingLoopsCount,
                int enclosingConditionalStmtsCount) {
            this.decoratedOracle = decoratedOracle;
            this.enclosingLoopsCount = enclosingLoopsCount;
            this.enclosingConditionalStmtsCount = enclosingConditionalStmtsCount;
        }

        public int getEnclosingLoopsCount() {
            return enclosingLoopsCount;
        }

        public int getEnclosingConditionalStmtsCount() {
            return enclosingConditionalStmtsCount;
        }

        public T getDecoratedOracle() {
            return decoratedOracle;
        }

        public CountingOracle<T> modifyDecoratedOracle(T decoratedOracle) {
            checkNotNull(decoratedOracle, "decorated oracle cannot be null");
            return new CountingOracle<>(decoratedOracle, this.enclosingLoopsCount,
                    this.enclosingConditionalStmtsCount);
        }

        private CountingOracle<T> incrementEnclosingLoopsCount() {
            return new CountingOracle<>(this.decoratedOracle, this.enclosingLoopsCount + 1,
                    this.enclosingConditionalStmtsCount);
        }

        private CountingOracle<T> decrementEnclosingLoopsCount() {
            return new CountingOracle<>(this.decoratedOracle, this.enclosingLoopsCount - 1,
                    this.enclosingConditionalStmtsCount);
        }

        private CountingOracle<T> incrementEnclosingConditionalStmtsCount() {
            return new CountingOracle<>(this.decoratedOracle, this.enclosingLoopsCount,
                    this.enclosingConditionalStmtsCount + 1);
        }

        private CountingOracle<T> decrementEnclosingConditionalStmtsCount() {
            return new CountingOracle<>(this.decoratedOracle, this.enclosingLoopsCount,
                    this.enclosingConditionalStmtsCount - 1);
        }
    }
}
