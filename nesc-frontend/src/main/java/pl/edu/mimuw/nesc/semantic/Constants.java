package pl.edu.mimuw.nesc.semantic;

import pl.edu.mimuw.nesc.ast.CString;
import pl.edu.mimuw.nesc.ast.CstKind;
import pl.edu.mimuw.nesc.ast.KnownCst;
import pl.edu.mimuw.nesc.ast.LabelDeclaration;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.Type;
import pl.edu.mimuw.nesc.ast.cval.Cval;
import pl.edu.mimuw.nesc.ast.datadeclaration.DataDeclaration;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.LexicalCst;
import pl.edu.mimuw.nesc.ast.gen.StringAst;
import pl.edu.mimuw.nesc.ast.gen.StringCst;

public final class Constants {

	/**
	 * <p>
	 * Constants come in three kinds:
	 * <ul>
	 * <li>"unknown": value is completely unknown (constant_unknown)
	 * <li>"address": value is the address of some global symbol and an offset
	 * (constant_address)
	 * <li>"value": known value (integer, floating point)
	 * </ul>
	 * </p>
	 */

	/*
	 * KnownCst and CstKind enums is defined in nesc-ast project.
	 */

	public static KnownCst makeUnknownCst(Cval cval, Type type) {
		// TODO
		return null;
	}

	public static KnownCst makeCst(Cval cval, Type type) {
		// TODO
		return null;
	}

	// TODO offset
	public static KnownCst makeAddressCst(DataDeclaration ddecl, LabelDeclaration ldecl,
			int offset, Type t) {
		// TODO
		return null;
	}

	// TODO largest_int x
	public static KnownCst makeSignedCst(int x, Type t) {
		// TODO
		return null;
	}

	// TODO largest_uint x
	public static KnownCst makeUnsignedCst(int x, Type t) {
		// TODO
		return null;
	}

	public static KnownCst castConstants(KnownCst c, Type to) {
		// TODO
		return null;
	}

	// TODO largest_uint intvalue
	public static LexicalCst foldLexicalInt(Type itype, Location location, CString tok,
			boolean isComplex, int intvalue, boolean overflow) {
		return null;
	}

	public static LexicalCst foldLexicalReal(Type realtype, Location loc, CString tok) {
		// TODO
		return null;
	}

	/* XXX: What's the right type for charvalue ? (must hold wchar_t or int) */
	public static LexicalCst foldLexicalChar(Location location, CString tok, boolean wideFlag,
			int charvalue) {
		// TODO
		return null;
	}

	// TODO components is list?
	public static StringAst foldLexicalString(Location location, StringCst components,
			CString value, boolean wideFlag) {
		// TODO
		return null;
	}

	public static KnownCst foldLabelAddress(Expression e) {
		// TODO
		return null;
	}

	public static KnownCst foldSizeof(Expression e, Type stype) {
		// TODO
		return null;
	}

	public static KnownCst foldAlignof(Expression e, Type atype) {
		// TODO
		return null;
	}

	public static KnownCst foldCast(Expression e) {
		// TODO
		return null;
	}

	public static KnownCst foldUnary(Expression e) {
		// TODO
		return null;
	}

	public static KnownCst foldBinary(Type restype, Expression e) {
		// TODO
		return null;
	}

	public static KnownCst foldConditional(Expression e) {
		// TODO
		return null;
	}

	public static KnownCst foldFunctionCall(Expression e, int pass) {
		// TODO
		return null;
	}

	public static KnownCst foldIdentifier(Expression e, DataDeclaration decl, int pass) {
		// TODO
		return null;
	}

	public static KnownCst foldAdd(Type restype, KnownCst c1, KnownCst c2) {
		// TODO
		return null;
	}

	public static KnownCst foldaddressIdentifier(Expression e, DataDeclaration decl) {
		// TODO
		return null;
	}

	public static KnownCst foldaddressString(StringAst s) {
		// TOD
		return null;
	}

	public static KnownCst foldaddressFieldRef(Expression e) {
		// TODO
		return null;
	}

	public static boolean definiteNull(Expression e) {
		// TODO
		return false;
	}

	public static boolean definiteZero(Expression e) {
		// TODO
		return false;
	}

	public static boolean isZeroConstant(KnownCst c) {
		return false;
	}

	/*
	 * Print a warning if a constant expression had overflow in folding. Invoke
	 * this function on every expression that the language requires to be a
	 * constant expression. Note the ANSI C standard says it is erroneous for a
	 * constant expression to overflow.
	 */
	public static void constant_overflow_warning(KnownCst c) {
		// TODO
	}

	/*
	 * TODO: lots of macros
	 */

	public static boolean checkConstantOnce(Expression e, CstKind k) {
		// TODO
		return false;
	}

	/*
	 * Effects: We want to check whether e is a constant, and possibly for valid
	 * constant values, exactly once (to avoid repeated errors and warnings)
	 * over our multiple constant folding passes. Additionally, we can't check
	 * unknown constants until their value is known. We can rely on the
	 * following: - a non-constant will not become constant - we assume, for
	 * checking purposes, that a constant's kind (numerical vs address) will not
	 * change (this in some sense untrue, as in: <unknown> ? <numerical> :
	 * <address> but we treat that as <address> for checking purposes) - a known
	 * constant will maintain its value - an unknown constant will become either
	 * non-constant or a known constant
	 * 
	 * Additionally, if the constant kind does not match k, we can check it
	 * immediately (presumably to report some error).
	 * 
	 * check_constant_once supports this by returning TRUE exactly once, when
	 * its possible to check e's value
	 * 
	 * Returns: TRUE the first time !e->cst || e->cst && !constant_unkown(e) ||
	 * e->cst && e->cst does not match k
	 */

	private Constants() {
		throw new RuntimeException("Object of this class should never be created");
	}

}
