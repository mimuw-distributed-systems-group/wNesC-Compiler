package pl.edu.mimuw.nesc.ast;

/**
 * <p>
 * A set of unary operation, where operand stands on the left side of
 * expression.
 * </p>
 * 
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * 
 */
public enum LeftUnaryOperation {
	ADDRESS_OF, UNARY_MINUS, UNARY_PLUS, PREINCREMENT, PREDECREMENT, BITNOT, NOT, REALPART, IMAGPART;
}
