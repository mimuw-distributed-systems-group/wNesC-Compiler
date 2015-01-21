package pl.edu.mimuw.nesc.astutil;

import com.google.common.base.Optional;
import java.util.List;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.type.Type;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ExpressionUtils {
    /**
     * Visitor that sets the unique name in visited identifiers.
     */
    private static final UniqueNameSetVisitor UNIQUE_NAME_SET_VISITOR = new UniqueNameSetVisitor();

    /**
     * Visitor that sets the type in visited expressions.
     */
    private static final TypeSetVisitor TYPE_SET_VISITOR = new TypeSetVisitor();

    /**
     * Sets the unique name of all identifiers contained in the given expression
     * to the given unique name.
     *
     * @param expr Expression with potential identifiers.
     * @param uniqueName Unique name to set.
     */
    public static void setUniqueNameDeep(Expression expr, Optional<String> uniqueName) {
        checkNotNull(expr, "expression cannot be null");
        checkNotNull(uniqueName, "unique name cannot be null");
        expr.traverse(UNIQUE_NAME_SET_VISITOR, uniqueName);
    }

    /**
     * Set the unique name of all identifiers contained in all expressions from
     * the given list to the given unique name.
     *
     * @param exprs List with expressions to modify identifiers in.
     * @param uniqueName Unique name to set.
     */
    public static void setUniqueNameDeep(List<? extends Expression> exprs, Optional<String> uniqueName) {
        checkNotNull(exprs, "list of expressions cannot be null");

        for (Expression expr : exprs) {
            setUniqueNameDeep(expr, uniqueName);
        }
    }

    /**
     * Set the type of all expressions contained in the given expression and in
     * the expression itself.
     *
     * @param expr Expression to change types.
     * @param type Type to set.
     */
    public static void setTypeDeep(Expression expr, Optional<Type> type) {
        checkNotNull(expr, "expression cannot be null");
        checkNotNull(type, "type cannot be null");
        expr.traverse(TYPE_SET_VISITOR, type);
    }

    /**
     * Set the types of all expressions and their subexpressions from the given
     * list.
     *
     * @param exprs List with expressions to modify types in.
     * @param type Type to set.
     */
    public static void setTypeDeep(List<? extends Expression> exprs, Optional<Type> type) {
        checkNotNull(exprs, "list of expressions cannot be null");

        for (Expression expr : exprs) {
            setTypeDeep(expr, type);
        }
    }

    private ExpressionUtils() {
    }

    private static final class UniqueNameSetVisitor extends IdentityVisitor<Optional<String>> {
        @Override
        public Optional<String> visitIdentifier(Identifier identifier, Optional<String> uniqueName) {
            identifier.setUniqueName(uniqueName);
            return uniqueName;
        }
    }

    private static final class TypeSetVisitor extends IdentityVisitor<Optional<Type>> {
        @Override
        public Optional<Type> visitPlus(Plus expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitMinus(Minus expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitTimes(Times expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitDivide(Divide expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitModulo(Modulo expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitLshift(Lshift expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitRshift(Rshift expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitLeq(Leq expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitGeq(Geq expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitLt(Lt expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitGt(Gt expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitEq(Eq expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitNe(Ne expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitBitand(Bitand expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitBitor(Bitor expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitBitxor(Bitxor expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitAndand(Andand expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitOror(Oror expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitAssign(Assign expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitPlusAssign(PlusAssign expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitMinusAssign(MinusAssign expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitTimesAssign(TimesAssign expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitDivideAssign(DivideAssign expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitModuloAssign(ModuloAssign expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitLshiftAssign(LshiftAssign expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitRshiftAssign(RshiftAssign expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitBitandAssign(BitandAssign expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitBitorAssign(BitorAssign expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitBitxorAssign(BitxorAssign expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitUnaryMinus(UnaryMinus expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitDereference(Dereference expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitAddressOf(AddressOf expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitUnaryPlus(UnaryPlus expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitBitnot(Bitnot expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitNot(Not expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitAlignofType(AlignofType expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitSizeofType(SizeofType expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitOffsetof(Offsetof expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitSizeofExpr(SizeofExpr expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitAlignofExpr(AlignofExpr expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitRealpart(Realpart expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitImagpart(Imagpart expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitArrayRef(ArrayRef expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitErrorExpr(ErrorExpr expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitComma(Comma expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitLabelAddress(LabelAddress expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitConditional(Conditional expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitIdentifier(Identifier expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitCompoundExpr(CompoundExpr expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitIntegerCst(IntegerCst expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitFloatingCst(FloatingCst expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitCharacterCst(CharacterCst expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitStringCst(StringCst expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitStringAst(StringAst expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitFunctionCall(FunctionCall expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitUniqueCall(UniqueCall expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitUniqueNCall(UniqueNCall expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitUniqueCountCall(UniqueCountCall expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitFieldRef(FieldRef expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitInterfaceDeref(InterfaceDeref expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitComponentDeref(ComponentDeref expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitPreincrement(Preincrement expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitPredecrement(Predecrement expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitPostincrement(Postincrement expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitPostdecrement(Postdecrement expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitCast(Cast expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitCastList(CastList expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitInitList(InitList expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitInitSpecific(InitSpecific expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitTypeArgument(TypeArgument expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitGenericCall(GenericCall expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }

        @Override
        public Optional<Type> visitExtensionExpr(ExtensionExpr expr, Optional<Type> type) {
            expr.setType(type);
            return type;
        }
    }
}
