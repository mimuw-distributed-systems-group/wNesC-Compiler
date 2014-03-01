package pl.edu.mimuw.nesc.semantic;

import java.util.LinkedList;

import pl.edu.mimuw.nesc.ast.LeftUnaryOperation;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.Type;
import pl.edu.mimuw.nesc.ast.datadeclaration.DataDeclaration;
import pl.edu.mimuw.nesc.ast.gen.AddressOf;
import pl.edu.mimuw.nesc.ast.gen.AlignofExpr;
import pl.edu.mimuw.nesc.ast.gen.AlignofType;
import pl.edu.mimuw.nesc.ast.gen.Andand;
import pl.edu.mimuw.nesc.ast.gen.ArrayRef;
import pl.edu.mimuw.nesc.ast.gen.Assign;
import pl.edu.mimuw.nesc.ast.gen.AstType;
import pl.edu.mimuw.nesc.ast.gen.Bitand;
import pl.edu.mimuw.nesc.ast.gen.BitandAssign;
import pl.edu.mimuw.nesc.ast.gen.Bitor;
import pl.edu.mimuw.nesc.ast.gen.BitorAssign;
import pl.edu.mimuw.nesc.ast.gen.Bitxor;
import pl.edu.mimuw.nesc.ast.gen.BitxorAssign;
import pl.edu.mimuw.nesc.ast.gen.Cast;
import pl.edu.mimuw.nesc.ast.gen.CastList;
import pl.edu.mimuw.nesc.ast.gen.Comma;
import pl.edu.mimuw.nesc.ast.gen.Conditional;
import pl.edu.mimuw.nesc.ast.gen.Dereference;
import pl.edu.mimuw.nesc.ast.gen.Divide;
import pl.edu.mimuw.nesc.ast.gen.DivideAssign;
import pl.edu.mimuw.nesc.ast.gen.Eq;
import pl.edu.mimuw.nesc.ast.gen.ErrorExpr;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.ExtensionExpr;
import pl.edu.mimuw.nesc.ast.gen.FieldRef;
import pl.edu.mimuw.nesc.ast.gen.FunctionCall;
import pl.edu.mimuw.nesc.ast.gen.Geq;
import pl.edu.mimuw.nesc.ast.gen.Gt;
import pl.edu.mimuw.nesc.ast.gen.IdLabel;
import pl.edu.mimuw.nesc.ast.gen.Identifier;
import pl.edu.mimuw.nesc.ast.gen.LabelAddress;
import pl.edu.mimuw.nesc.ast.gen.Leq;
import pl.edu.mimuw.nesc.ast.gen.Lshift;
import pl.edu.mimuw.nesc.ast.gen.LshiftAssign;
import pl.edu.mimuw.nesc.ast.gen.Lt;
import pl.edu.mimuw.nesc.ast.gen.Minus;
import pl.edu.mimuw.nesc.ast.gen.MinusAssign;
import pl.edu.mimuw.nesc.ast.gen.Modulo;
import pl.edu.mimuw.nesc.ast.gen.ModuloAssign;
import pl.edu.mimuw.nesc.ast.gen.Ne;
import pl.edu.mimuw.nesc.ast.gen.Oror;
import pl.edu.mimuw.nesc.ast.gen.Plus;
import pl.edu.mimuw.nesc.ast.gen.PlusAssign;
import pl.edu.mimuw.nesc.ast.gen.Postdecrement;
import pl.edu.mimuw.nesc.ast.gen.Postincrement;
import pl.edu.mimuw.nesc.ast.gen.Predecrement;
import pl.edu.mimuw.nesc.ast.gen.Preincrement;
import pl.edu.mimuw.nesc.ast.gen.Rshift;
import pl.edu.mimuw.nesc.ast.gen.RshiftAssign;
import pl.edu.mimuw.nesc.ast.gen.SizeofExpr;
import pl.edu.mimuw.nesc.ast.gen.SizeofType;
import pl.edu.mimuw.nesc.ast.gen.Times;
import pl.edu.mimuw.nesc.ast.gen.TimesAssign;
import pl.edu.mimuw.nesc.ast.gen.Unary;

/**
 * <p>
 * Contains a set of methods useful for creating syntax tree nodes during
 * parsing.
 * </p>
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public class Expressions {

    private static final ErrorExpr ERROR_EXPRESSION;

    static {
        final Location errorLocation = new Location("", 0, 0);
        ERROR_EXPRESSION = new ErrorExpr(errorLocation);
        ERROR_EXPRESSION.setEndLocation(errorLocation);
    }

	/* Return TRUE if no error and lhstype and rhstype are not error_type */
	public static boolean check_assignment(Type lhstype, Type rhstype,
			Expression rhs, String context, DataDeclaration fundecl, int parmnum) {
		// TODO
		return false;
	}

	public static boolean checkConversion(Type to, Type from) {
		// TODO
		return false;
	}

	public static boolean checkArguments(Type fntype,
			LinkedList<Expression> arglist, DataDeclaration fundecl,
			boolean generic_call) {
		// TODO
		return false;
	}

	public static Type defaultConversion(Expression e) {
		// TODO
		return null;
	}

	public static Type defaultConversionForAssignment(Expression e) {
		// TODO
		return null;
	}

	public static LabelAddress makeLabelAddress(Location location,
			IdLabel idLabel) {
		LabelAddress result = new LabelAddress(location, idLabel);
		// TODO
		return result;
	}

	public static AddressOf makeAddressOf(Location location, Expression exp) {
		AddressOf result = new AddressOf(location, exp);
		// TODO
		return result;
	}

	public static AlignofExpr makeAlignofExpr(Location location, Expression exp) {
		AlignofExpr result = new AlignofExpr(location, exp);
		// TODO
		return result;
	}

	public static AlignofType makeAlignofType(Location location, AstType astType) {
		AlignofType result = new AlignofType(location, astType);
		// TODO
		return result;
	}

	public static Cast makeCast(Location location, AstType astType,
			Expression exp) {
		Cast result = new Cast(location, exp, astType);
		// TODO
		return result;
	}

	public static CastList makeCastList(Location location, AstType astType,
			Expression exp) {
		CastList result = new CastList(location, astType, exp);
		// TODO
		return result;
	}

	public static Plus makePlus(Location location, Expression exp1,
			Expression exp2) {
		Plus result = new Plus(location, exp1, exp2);
		// TODO
		return result;
	}

	public static Minus makeMinus(Location location, Expression exp1,
			Expression exp2) {
		Minus result = new Minus(location, exp1, exp2);
		// TODO
		return result;
	}

	public static Times makeTimes(Location location, Expression exp1,
			Expression exp2) {
		Times result = new Times(location, exp1, exp2);
		// TODO
		return result;
	}

	public static Divide makeDivide(Location location, Expression exp1,
			Expression exp2) {
		Divide result = new Divide(location, exp1, exp2);
		// TODO
		return result;
	}

	public static Modulo makeModulo(Location location, Expression exp1,
			Expression exp2) {
		Modulo result = new Modulo(location, exp1, exp2);
		// TODO
		return result;
	}

	public static Lshift makeLshift(Location location, Expression exp1,
			Expression exp2) {
		Lshift result = new Lshift(location, exp1, exp2);
		// TODO
		return result;
	}

	public static Rshift makeRshift(Location location, Expression exp1,
			Expression exp2) {
		Rshift result = new Rshift(location, exp1, exp2);
		// TODO
		return result;
	}

	public static Leq makeLeq(Location location, Expression exp1,
			Expression exp2) {
		Leq result = new Leq(location, exp1, exp2);
		// TODO
		return result;
	}

	public static Geq makeGeq(Location location, Expression exp1,
			Expression exp2) {
		Geq result = new Geq(location, exp1, exp2);
		// TODO
		return result;
	}

	public static Lt makeLt(Location location, Expression exp1, Expression exp2) {
		Lt result = new Lt(location, exp1, exp2);
		// TODO
		return result;
	}

	public static Gt makeGt(Location location, Expression exp1, Expression exp2) {
		Gt result = new Gt(location, exp1, exp2);
		// TODO
		return result;
	}

	public static Eq makeEq(Location location, Expression exp1, Expression exp2) {
		Eq result = new Eq(location, exp1, exp2);
		// TODO
		return result;
	}

	public static Ne makeNe(Location location, Expression exp1, Expression exp2) {
		Ne result = new Ne(location, exp1, exp2);
		// TODO
		return result;
	}

	public static Bitand makeBitand(Location location, Expression exp1,
			Expression exp2) {
		Bitand result = new Bitand(location, exp1, exp2);
		// TODO
		return result;
	}

	public static Bitor makeBitor(Location location, Expression exp1,
			Expression exp2) {
		Bitor result = new Bitor(location, exp1, exp2);
		// TODO
		return result;
	}

	public static Bitxor makeBitxor(Location location, Expression exp1,
			Expression exp2) {
		Bitxor result = new Bitxor(location, exp1, exp2);
		// TODO
		return result;
	}

	public static Andand makeAndand(Location location, Expression exp1,
			Expression exp2) {
		Andand result = new Andand(location, exp1, exp2);
		// TODO
		return result;
	}

	public static Oror makeOror(Location location, Expression exp1,
			Expression exp2) {
		Oror result = new Oror(location, exp1, exp2);
		// TODO
		return result;
	}

	public static Assign makeAssign(Location location, Expression exp1,
			Expression exp2) {
		Assign result = new Assign(location, exp1, exp2);
		// TODO
		return result;
	}

	public static PlusAssign makePlusAssign(Location location, Expression exp1,
			Expression exp2) {
		PlusAssign result = new PlusAssign(location, exp1, exp2);
		// TODO
		return result;
	}

	public static MinusAssign makeMinusAssign(Location location,
			Expression exp1, Expression exp2) {
		MinusAssign result = new MinusAssign(location, exp1, exp2);
		// TODO
		return result;
	}

	public static TimesAssign makeTimesAssign(Location location,
			Expression exp1, Expression exp2) {
		TimesAssign result = new TimesAssign(location, exp1, exp2);
		// TODO
		return result;
	}

	public static DivideAssign makeDivideAssign(Location location,
			Expression exp1, Expression exp2) {
		DivideAssign result = new DivideAssign(location, exp1, exp2);
		// TODO
		return result;
	}

	public static ModuloAssign makeModuloAssign(Location location,
			Expression exp1, Expression exp2) {
		ModuloAssign result = new ModuloAssign(location, exp1, exp2);
		// TODO
		return result;
	}

	public static LshiftAssign makeLshiftAssign(Location location,
			Expression exp1, Expression exp2) {
		LshiftAssign result = new LshiftAssign(location, exp1, exp2);
		// TODO
		return result;
	}

	public static RshiftAssign makeRshiftAssign(Location location,
			Expression exp1, Expression exp2) {
		RshiftAssign result = new RshiftAssign(location, exp1, exp2);
		// TODO
		return result;
	}

	public static BitandAssign makeBitandAssign(Location location,
			Expression exp1, Expression exp2) {
		BitandAssign result = new BitandAssign(location, exp1, exp2);
		// TODO
		return result;
	}

	public static BitorAssign makeBitorAssign(Location location,
			Expression exp1, Expression exp2) {
		BitorAssign result = new BitorAssign(location, exp1, exp2);
		// TODO
		return result;
	}

	public static BitxorAssign makeBitxorAssign(Location location,
			Expression exp1, Expression exp2) {
		BitxorAssign result = new BitxorAssign(location, exp1, exp2);
		// TODO
		return result;
	}

	public static Conditional makeConditional(Location location,
			Expression condition, Expression trueExp, Expression falseExp) {
		Conditional result = new Conditional(location, condition, trueExp,
				falseExp);
		// TODO
		return result;
	}

	public static Identifier makeIdentifier(Location location, String id,
			boolean maybeImplicit) {
		Identifier result = new Identifier(location, id, null); // FIXME
		// TODO
		return result;
	}

	public static ArrayRef makeArrayRef(Location location, Expression array,
			LinkedList<Expression> index) {
		ArrayRef result = new ArrayRef(location, array, index);
		// TODO
		return result;
	}

	public static FieldRef makeFieldRef(Location location, Expression object,
			String field) {
		FieldRef result = new FieldRef(location, object, field);
		// TODO
		return result;
	}

	public static Comma makeComma(Location location,
			LinkedList<Expression> expressionList) {
		Comma result = new Comma(location, expressionList);
		// TODO
		return result;
	}

	public static FunctionCall makeFunctionCall(Location location,
			Expression function, LinkedList<Expression> args) {
		FunctionCall result = new FunctionCall(location, function, args, null,
				null);
		// TODO
		return result;
	}

	public static Dereference makeDereference(Location location, Expression exp) {
		Dereference result = new Dereference(location, exp);
		// TODO
		return result;
	}

	public static ExtensionExpr makeExtensionExpr(Location location,
			Expression exp) {
		ExtensionExpr result = new ExtensionExpr(location, exp);
		// TODO
		return result;
	}

	public static Predecrement makePredecrement(Location location,
			Expression exp) {
		Predecrement result = new Predecrement(location, exp);
		// TODO
		return result;
	}

	public static Preincrement makePreincrement(Location location,
			Expression exp) {
		Preincrement result = new Preincrement(location, exp);
		// TODO
		return result;
	}

	public static Postdecrement makePostdecrement(Location location,
			Expression exp) {
		Postdecrement result = new Postdecrement(location, exp);
		// TODO
		return result;
	}

	public static Postincrement makePostincrement(Location location,
			Expression exp) {
		Postincrement result = new Postincrement(location, exp);
		// TODO
		return result;
	}

	public static SizeofExpr makeSizeofExpr(Location location, Expression exp) {
		SizeofExpr result = new SizeofExpr(location, exp);
		// TODO
		return result;
	}

	public static SizeofType makeSizeofType(Location location, AstType astType) {
		SizeofType result = new SizeofType(location, astType);
		// TODO
		return result;
	}

	public static Unary makeUnary(Location location,
			LeftUnaryOperation operation, Expression exp) {
		assert operation != null;

		switch (operation) {
		case ADDRESS_OF:
			return makeAddressOf(location, exp);
		case PREINCREMENT:
			return makePreincrement(location, exp);
		case PREDECREMENT:
			return makePredecrement(location, exp);
		default:
			// FIXME
			return makePredecrement(location, exp);
			//throw new InvalidParameterException("Unhandled unary operation value "
			//		+ operation);
		}
	}

    public static ErrorExpr makeErrorExpr() {
        return ERROR_EXPRESSION;
    }

    private Expressions() {
    }
}

