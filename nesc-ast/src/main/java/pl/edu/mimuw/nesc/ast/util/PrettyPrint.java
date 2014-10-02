package pl.edu.mimuw.nesc.ast.util;

import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import pl.edu.mimuw.nesc.ast.NescCallKind;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.ast.type.Type;

import static com.google.common.base.Preconditions.checkNotNull;
import static pl.edu.mimuw.nesc.ast.util.AstConstants.*;
import static pl.edu.mimuw.nesc.ast.util.AstConstants.BinaryOp.*;
import static pl.edu.mimuw.nesc.ast.util.AstConstants.UnaryOp.*;

/**
 * A class that is responsible for printing certain NesC constructions as
 * a string.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class PrettyPrint extends ExceptionVisitor<Void, Void> {
    /**
     * Constants used for printing.
     */
    private static final String TEXT_ERROR = "<error>";
    private static final String TEXT_COMPOUND_EXPR = "<compound expr>";
    private static final String TEXT_INVALID_TYPE = "<invalid type>";
    private static final String TEXT_INITIALIZER_LIST = "<initializer list>";
    private static final String TEXT_INITIALIZER = "<initializer>";
    private static final String TEXT_GENERIC_CALL = "<generic call>";
    private static final String TEXT_CONJUGATE = "<conjugate>";
    private static final String TEXT_BUILTIN_VA_ARG = "__builtin_va_arg";
    private static final String TEXT_EXTENSION = "__extension__";
    private static final String COMMA = ",";

    /**
     * Map with call keywords for particular call kinds.
     */
    private static final ImmutableMap<NescCallKind, String> CALL_KEYWORDS = ImmutableMap.of(
        NescCallKind.COMMAND_CALL, CALL_COMMAND,
        NescCallKind.EVENT_SIGNAL, CALL_EVENT,
        NescCallKind.POST_TASK, CALL_TASK
    );

    /**
     * String builder that is used for building the string representation of an
     * expression.
     */
    private final StringBuilder strBuilder = new StringBuilder();

    /**
     * Print an expression to a string.
     *
     * @param expr Expression to be printed.
     * @return Textual representation of the given expression as it would appear
     *         in a program.
     * @throws NullPointerException Given argument is null.
     */
    public static String expression(Expression expr) {
        checkNotNull(expr, "expression cannot be null");

        final PrettyPrint printer = new PrettyPrint();
        expr.accept(printer, null);
        return printer.strBuilder.toString();
    }

    /**
     * Private constructor to prevent this class from being instantiated.
     */
    private PrettyPrint() {
    }

    @Override
    public Void visitPlus(Plus expr, Void arg) {
        printBinary(expr, PLUS);
        return null;
    }

    @Override
    public Void visitMinus(Minus expr, Void arg) {
        printBinary(expr, MINUS);
        return null;
    }

    @Override
    public Void visitTimes(Times expr, Void arg) {
        printBinary(expr, TIMES);
        return null;
    }

    @Override
    public Void visitDivide(Divide expr, Void arg) {
        printBinary(expr, DIVIDE);
        return null;
    }

    @Override
    public Void visitModulo(Modulo expr, Void arg) {
        printBinary(expr, MODULO);
        return null;
    }

    @Override
    public Void visitLshift(Lshift expr, Void arg) {
        printBinary(expr, LSHIFT);
        return null;
    }

    @Override
    public Void visitRshift(Rshift expr, Void arg) {
        printBinary(expr, RSHIFT);
        return null;
    }

    @Override
    public Void visitLeq(Leq expr, Void arg) {
        printBinary(expr, LEQ);
        return null;
    }

    @Override
    public Void visitGeq(Geq expr, Void arg) {
        printBinary(expr, GEQ);
        return null;
    }

    @Override
    public Void visitLt(Lt expr, Void arg) {
        printBinary(expr, LT);
        return null;
    }

    @Override
    public Void visitGt(Gt expr, Void arg) {
        printBinary(expr, GT);
        return null;
    }

    @Override
    public Void visitEq(Eq expr, Void arg) {
        printBinary(expr, EQ);
        return null;
    }

    @Override
    public Void visitNe(Ne expr, Void arg) {
        printBinary(expr, NE);
        return null;
    }

    @Override
    public Void visitBitand(Bitand expr, Void arg) {
        printBinary(expr, BITAND);
        return null;
    }

    @Override
    public Void visitBitor(Bitor expr, Void arg) {
        printBinary(expr, BITOR);
        return null;
    }

    @Override
    public Void visitBitxor(Bitxor expr, Void arg) {
        printBinary(expr, BITXOR);
        return null;
    }

    @Override
    public Void visitAndand(Andand expr, Void arg) {
        printBinary(expr, ANDAND);
        return null;
    }

    @Override
    public Void visitOror(Oror expr, Void arg) {
        printBinary(expr, OROR);
        return null;
    }

    @Override
    public Void visitAssign(Assign expr, Void arg) {
        printBinary(expr, ASSIGN);
        return null;
    }

    @Override
    public Void visitPlusAssign(PlusAssign expr, Void arg) {
        printBinary(expr, ASSIGN_PLUS);
        return null;
    }

    @Override
    public Void visitMinusAssign(MinusAssign expr, Void arg) {
        printBinary(expr, ASSIGN_MINUS);
        return null;
    }

    @Override
    public Void visitTimesAssign(TimesAssign expr, Void arg) {
        printBinary(expr, ASSIGN_TIMES);
        return null;
    }

    @Override
    public Void visitDivideAssign(DivideAssign expr, Void arg) {
        printBinary(expr, ASSIGN_DIVIDE);
        return null;
    }

    @Override
    public Void visitModuloAssign(ModuloAssign expr, Void arg) {
        printBinary(expr, ASSIGN_MODULO);
        return null;
    }

    @Override
    public Void visitLshiftAssign(LshiftAssign expr, Void arg) {
        printBinary(expr, ASSIGN_LSHIFT);
        return null;
    }

    @Override
    public Void visitRshiftAssign(RshiftAssign expr, Void arg) {
        printBinary(expr, ASSIGN_RSHIFT);
        return null;
    }

    @Override
    public Void visitBitandAssign(BitandAssign expr, Void arg) {
        printBinary(expr, ASSIGN_BITAND);
        return null;
    }

    @Override
    public Void visitBitorAssign(BitorAssign expr, Void arg) {
        printBinary(expr, ASSIGN_BITOR);
        return null;
    }

    @Override
    public Void visitBitxorAssign(BitxorAssign expr, Void arg) {
        printBinary(expr, ASSIGN_BITXOR);
        return null;
    }

    @Override
    public Void visitUnaryMinus(UnaryMinus expr, Void arg) {
        printUnary(expr, UNARY_MINUS);
        return null;
    }

    @Override
    public Void visitDereference(Dereference expr, Void arg) {
        printUnary(expr, DEREFERENCE);
        return null;
    }

    @Override
    public Void visitAddressOf(AddressOf expr, Void arg) {
        printUnary(expr, ADDRESSOF);
        return null;
    }

    @Override
    public Void visitUnaryPlus(UnaryPlus expr, Void arg) {
        printUnary(expr, UNARY_PLUS);
        return null;
    }

    @Override
    public Void visitBitnot(Bitnot expr, Void arg) {
        printUnary(expr, BITNOT);
        return null;
    }

    @Override
    public Void visitNot(Not expr, Void arg) {
        printUnary(expr, NOT);
        return null;
    }

    @Override
    public Void visitAlignofType(AlignofType expr, Void arg) {
        printExprWithType(expr, OP_ALIGNOF, expr.getAsttype());
        return null;
    }

    @Override
    public Void visitSizeofType(SizeofType expr, Void arg) {
        printExprWithType(expr, OP_SIZEOF, expr.getAsttype());
        return null;
    }

    @Override
    public Void visitSizeofExpr(SizeofExpr expr, Void arg) {
        printLetterUnary(expr, OP_SIZEOF);
        return null;
    }

    @Override
    public Void visitAlignofExpr(AlignofExpr expr, Void arg) {
        printLetterUnary(expr, OP_ALIGNOF);
        return null;
    }

    @Override
    public Void visitRealpart(Realpart expr, Void arg) {
        printLetterUnary(expr, OP_REALPART);
        return null;
    }

    @Override
    public Void visitImagpart(Imagpart expr, Void arg) {
        printLetterUnary(expr, OP_IMAGPART);
        return null;
    }

    @Override
    public Void visitArrayRef(ArrayRef expr, Void arg) {
        printLeftParentheses(expr);

        expr.getArray().accept(this, null);
        strBuilder.append(LBRACK);
        printCommaSepList(expr.getIndex());
        strBuilder.append(RBRACK);

        printRightParentheses(expr);
        return null;
    }

    @Override
    public Void visitErrorExpr(ErrorExpr expr, Void arg) {
        printLeftParentheses(expr);
        strBuilder.append(TEXT_ERROR);
        printRightParentheses(expr);
        return null;
    }

    @Override
    public Void visitComma(Comma expr, Void arg) {
        printLeftParentheses(expr);
        printCommaSepList(expr.getExpressions());
        printRightParentheses(expr);
        return null;
    }

    @Override
    public Void visitLabelAddress(LabelAddress expr, Void arg) {
        printLeftParentheses(expr);

        strBuilder.append(LABELADDRESS);
        strBuilder.append(expr.getIdLabel().getId());

        printRightParentheses(expr);
        return null;
    }

    @Override
    public Void visitConditional(Conditional expr, Void arg) {
        printLeftParentheses(expr);

        expr.getCondition().accept(this, null);
        strBuilder.append(" ? ");
        expr.getOnTrueExp().accept(this, null);
        strBuilder.append(" : ");
        expr.getOnFalseExp().accept(this, null);

        printRightParentheses(expr);
        return null;
    }

    @Override
    public Void visitIdentifier(Identifier expr, Void arg) {
        printLeftParentheses(expr);
        strBuilder.append(expr.getName());
        printRightParentheses(expr);
        return null;
    }

    @Override
    public Void visitCompoundExpr(CompoundExpr expr, Void arg) {
        printLeftParentheses(expr);

        strBuilder.append(LPAREN);
        strBuilder.append(TEXT_COMPOUND_EXPR);
        strBuilder.append(RPAREN);

        printRightParentheses(expr);
        return null;
    }

    @Override
    public Void visitIntegerCst(IntegerCst expr, Void arg) {
        printConstant(expr);
        return null;
    }

    @Override
    public Void visitFloatingCst(FloatingCst expr, Void arg) {
        printConstant(expr);
        return null;
    }

    @Override
    public Void visitCharacterCst(CharacterCst expr, Void arg) {
        printConstant(expr);
        return null;
    }

    @Override
    public Void visitStringCst(StringCst expr, Void arg) {
        printLeftParentheses(expr);

        strBuilder.append('"');
        strBuilder.append(expr.getString());
        strBuilder.append('"');

        printRightParentheses(expr);
        return null;
    }

    @Override
    public Void visitStringAst(StringAst expr, Void arg) {
        printLeftParentheses(expr);
        printSpaceSepList(expr.getStrings());
        printRightParentheses(expr);
        return null;
    }

    @Override
    public Void visitFunctionCall(FunctionCall expr, Void arg) {
        printLeftParentheses(expr);
        final AstType vaArgCall = expr.getVaArgCall();

        if (vaArgCall == null) {
            // Call keyword
            final Optional<String> callKeyword = Optional.fromNullable(CALL_KEYWORDS.get(expr.getCallKind()));
            if (callKeyword.isPresent()) {
                strBuilder.append(callKeyword.get());
                strBuilder.append(" ");
            }

            // Function identifier and parameters
            expr.getFunction().accept(this, null);
            strBuilder.append(LPAREN);
            printCommaSepList(expr.getArguments());
            strBuilder.append(RPAREN);
        } else {
            strBuilder.append(TEXT_BUILTIN_VA_ARG);
            strBuilder.append(LPAREN);
            printCommaSepList(expr.getArguments());
            strBuilder.append(COMMA);
            strBuilder.append(" ");
            printType(vaArgCall.getType());
            strBuilder.append(RPAREN);
        }

        printRightParentheses(expr);
        return null;
    }

    @Override
    public Void visitFieldRef(FieldRef expr, Void arg) {
        printFieldLikeExpr(expr, expr.getFieldName());
        return null;
    }

    @Override
    public Void visitInterfaceDeref(InterfaceDeref expr, Void arg) {
        printFieldLikeExpr(expr, expr.getMethodName());
        return null;
    }

    @Override
    public Void visitComponentDeref(ComponentDeref expr, Void arg) {
        printFieldLikeExpr(expr, expr.getFieldName());
        return null;
    }

    @Override
    public Void visitPreincrement(Preincrement expr, Void arg) {
        printPreincrement(expr, INCREMENT);
        return null;
    }

    @Override
    public Void visitPredecrement(Predecrement expr, Void arg) {
        printPreincrement(expr, DECREMENT);
        return null;
    }

    @Override
    public Void visitPostincrement(Postincrement expr, Void arg) {
        printPostincrement(expr, INCREMENT);
        return null;
    }

    @Override
    public Void visitPostdecrement(Postdecrement expr, Void arg) {
        printPostincrement(expr, DECREMENT);
        return null;
    }

    @Override
    public Void visitCast(Cast expr, Void arg) {
        printCastLikeExpr(expr, expr.getAsttype().getType(), expr.getArgument());
        return null;
    }

    @Override
    public Void visitCastList(CastList expr, Void arg) {
        printCastLikeExpr(expr, expr.getAsttype().getType(), expr.getInitExpr());
        return null;
    }

    @Override
    public Void visitInitList(InitList expr, Void arg) {
        printLeftParentheses(expr);
        strBuilder.append(TEXT_INITIALIZER_LIST);
        printRightParentheses(expr);
        return null;
    }

    @Override
    public Void visitInitSpecific(InitSpecific expr, Void arg) {
        printLeftParentheses(expr);
        strBuilder.append(TEXT_INITIALIZER);
        printRightParentheses(expr);
        return null;
    }

    @Override
    public Void visitTypeArgument(TypeArgument expr, Void arg) {
        printLeftParentheses(expr);
        printType(expr.getAsttype().getType());
        printRightParentheses(expr);
        return null;
    }

    @Override
    public Void visitGenericCall(GenericCall expr, Void arg) {
        // FIXME
        printLeftParentheses(expr);
        strBuilder.append(TEXT_GENERIC_CALL);
        printRightParentheses(expr);
        return null;
    }

    @Override
    public Void visitExtensionExpr(ExtensionExpr expr, Void arg) {
        printLeftParentheses(expr);

        strBuilder.append(TEXT_EXTENSION);
        strBuilder.append(" ");
        expr.getArgument().accept(this, null);

        printRightParentheses(expr);
        return null;
    }

    @Override
    public Void visitConjugate(Conjugate expr, Void arg) {
        // FIXME
        printLeftParentheses(expr);
        strBuilder.append(TEXT_CONJUGATE);
        printRightParentheses(expr);
        return null;
    }


    private void printBinary(Binary binary, BinaryOp op) {
        printLeftParentheses(binary);
        binary.getLeftArgument().accept(this, null);

        strBuilder.append(' ');
        strBuilder.append(op);
        strBuilder.append(' ');

        binary.getRightArgument().accept(this, null);
        printRightParentheses(binary);
    }

    private void printUnary(Unary unary, UnaryOp op) {
        printLeftParentheses(unary);

        strBuilder.append(op);
        unary.getArgument().accept(this, null);

        printRightParentheses(unary);
    }

    private void printLetterUnary(Unary unary, String op) {
        printLeftParentheses(unary);

        strBuilder.append(op);
        strBuilder.append(' ');
        unary.getArgument().accept(this, null);

        printRightParentheses(unary);
    }

    private void printExprWithType(Expression expr, String op, AstType astType) {
        printLeftParentheses(expr);

        strBuilder.append(op);
        strBuilder.append(LPAREN);
        printType(astType.getType());
        strBuilder.append(RPAREN);

        printRightParentheses(expr);
    }

    private void printConstant(LexicalCst cst) {
        printLeftParentheses(cst);
        strBuilder.append(cst.getString());
        printRightParentheses(cst);
    }

    private void printType(Optional<Type> type) {
        final String typeStr = type
                .transform(Functions.toStringFunction())
                .or(TEXT_INVALID_TYPE);
        strBuilder.append(typeStr);
    }

    private void printPreincrement(Unary unary, UnaryOp op) {
        printLeftParentheses(unary);
        strBuilder.append(op);
        unary.getArgument().accept(this, null);
        printRightParentheses(unary);
    }

    private void printPostincrement(Unary unary, UnaryOp op) {
        printLeftParentheses(unary);
        unary.getArgument().accept(this, null);
        strBuilder.append(op);
        printRightParentheses(unary);
    }

    private void printCastLikeExpr(Expression expr, Optional<Type> type, Expression subExpr) {
        printLeftParentheses(expr);

        strBuilder.append(LPAREN);
        printType(type);
        strBuilder.append(RPAREN);
        strBuilder.append(" ");
        subExpr.accept(this, null);

        printRightParentheses(expr);
    }

    private void printFieldLikeExpr(Unary unary, String fieldName) {
        printLeftParentheses(unary);

        unary.getArgument().accept(this, null);
        strBuilder.append(DOT);
        strBuilder.append(fieldName);

        printRightParentheses(unary);
    }

    private void printCommaSepList(List<? extends Expression> expressions) {
        printSeparatedList(expressions, COMMA);
    }

    private void printSpaceSepList(List<? extends Expression> expressions) {
        printSeparatedList(expressions, " ");
    }

    private void printSeparatedList(List<? extends Expression> expressions, String separator) {
        boolean first = true;

        for (Expression expr : expressions) {
            if (!first) {
                strBuilder.append(separator);
                strBuilder.append(" ");
            } else {
                first = false;
            }

            expr.accept(this, null);
        }
    }

    private void printLeftParentheses(Expression expression) {
        printRepeated(LPAREN, Optional.fromNullable(expression.getParenthesesCount()).or(0));
    }

    private void printRightParentheses(Expression expression) {
        printRepeated(RPAREN, Optional.fromNullable(expression.getParenthesesCount()).or(0));
    }

    private void printRepeated(String toRepeat, int count) {
        for (int i = 0; i < count; ++i) {
            strBuilder.append(toRepeat);
        }
    }
}
