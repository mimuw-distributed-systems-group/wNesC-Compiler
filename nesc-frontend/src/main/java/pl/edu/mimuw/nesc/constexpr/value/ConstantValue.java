package pl.edu.mimuw.nesc.constexpr.value;

import pl.edu.mimuw.nesc.constexpr.value.type.ConstantType;

/**
 * <p>Interface with operations for a constant value - the result of evaluating
 * a constant expression.</p>
 *
 * <p>Objects of all classes that implement this interface shall be immutable.
 * </p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface ConstantValue {
    /**
     * Get the type of this constant value. Values returned by this method allow
     * casting this constant value to proper classes without error.
     *
     * @return Type of this constant value.
     */
    ConstantType getType();

    /**
     * Add the given constant value to this one. The argument and this value
     * are unchanged after the call.
     *
     * @param value Constant value to add to this one.
     * @return Result of the addition (<code>this + value</code>).
     * @throws IllegalArgumentException The given value cannot be added to this
     *                                  one (e.g. because it has a different
     *                                  type).
     */
    ConstantValue add(ConstantValue value);

    /**
     * Subtract the given value from this one. The argument and this value
     * are left unchanged.
     *
     * @param value Value to be subtracted from this one.
     * @return Result of the subtraction
     *         (<code>this - value</code>).
     * @throws IllegalArgumentException The given value cannot be subtracted
     *                                  from this one (e.g. because it has
     *                                  a different type).
     */
    ConstantValue subtract(ConstantValue value);

    /**
     * Multiply the given value by this one. The argument and this value are not
     * changed by this method.
     *
     * @param value The second argument for the multiplication operation.
     * @return Result of the multiplication
     *         (<code>this * value</code>).
     * @throws IllegalArgumentException Cannot multiply this value by the given
     *                                  one (e.g. because it has a type that
     *                                  differs from type of this value).
     */
    ConstantValue multiply(ConstantValue value);

    /**
     * Divide this value by the given one. If it is necessary, then the result
     * is truncated toward zero. The argument and this value are not changed by
     * this method.
     *
     * @param value The second argument for the division operation.
     * @return Result of the division (<code>this / value</code>).
     * @throws IllegalArgumentException Cannot divide this value by the given
     *                                  one (e.g. because it has a type that
     *                                  differs from the type of this value).
     */
    ConstantValue divide(ConstantValue value);

    /**
     * <p>Get the remainder of the division of this value by the given one. The
     * result is a value <code>r</code> such that</p>
     *
     * <pre>this.divide(value).multiply(value).add(this.remainder(value))</pre>
     *
     * <p>represents the same value as <code>this</code>.</p>
     *
     * @param value The second argument for the remainder operation.
     * @return Result of the remainder operation
     *         (<code>this % value</code>).
     * @throws UnsupportedOperationException The method is invoked on a floating
     *                                       constant.
     * @throws IllegalArgumentException The arithmetic operation cannot be
     *                                  performed, e.g. because the type of the
     *                                  parameter is different from the type of
     *                                  this constant.
     */
    ConstantValue remainder(ConstantValue value);

    /**
     * <p>Shift the bits of this value to the left.</p>
     *
     * @param value Number of bits to shift.
     * @return Result of shifting bits that constitute this value to the left
     *         (<code>this << value</code>).
     * @throws UnsupportedOperationException The method is invoked on a floating
     *                                       constant.
     * @throws IllegalArgumentException The type of the given value is not
     *                                  integer.
     */
    ConstantValue shiftLeft(ConstantValue value);

    /**
     * <p>Shift the bits of this value to the right.</p>
     *
     * @param value Number of bits to shift.
     * @return Result of shifting bits that constitute this value to the right
     *         (<code>this >> value</code>).
     * @throws UnsupportedOperationException The method is invoked on a floating
     *                                       constant.
     * @throws IllegalArgumentException The type of the given value is not
     *                                  integer.
     */
    ConstantValue shiftRight(ConstantValue value);

    /**
     * Compute the result of the comparison: <code>this &lt; value</code>.
     *
     * @param value Value to be compared with this one.
     * @return A one-bit unsigned integer value that has the value
     *         <code>1</code> if this value is less than the given one and
     *         <code>0</code> otherwise.
     * @throws IllegalArgumentException The comparison operation cannot be
     *                                  performed, e.g. because the type of the
     *                                  parameter is different from the type of
     *                                  this constant.
     */
    ConstantValue less(ConstantValue value);

    /**
     * Compute the result of the comparison: <code>this <= value</code>.
     *
     * @param value Value to be compared with this one.
     * @return A one-bit unsigned integer value that has the value
     *         <code>1</code> if this value is less than or equal to the given
     *         one and <code>0</code> otherwise.
     * @throws IllegalArgumentException The comparison operation cannot be
     *                                  performed, e.g. because the type of the
     *                                  parameter is different from the type of
     *                                  this constant.
     */
    ConstantValue lessOrEqual(ConstantValue value);

    /**
     * Compute the result of the comparison: <code>this &gt; value</code>.
     *
     * @param value Value to be compared with this one.
     * @return A one-bit unsigned integer value that has the value
     *         <code>1</code> if this value is greater than the given one and
     *         <code>0</code> otherwise.
     * @throws IllegalArgumentException The comparison operation cannot be
     *                                  performed, e.g. because the type of the
     *                                  parameter is different from the type of
     *                                  this constant.
     */
    ConstantValue greater(ConstantValue value);

    /**
     * Compute the result of the comparison: <code>this >= value</code>.
     *
     * @param value Value to be compared with this one.
     * @return A one-bit unsigned integer value that has the value
     *         <code>1</code> if this value is greater than or equal to the
     *         given one and <code>0</code> otherwise.
     * @throws IllegalArgumentException The comparison operation cannot be
     *                                  performed, e.g. because the type of the
     *                                  parameter is different from the type of
     *                                  this constant.
     */
    ConstantValue greaterOrEqual(ConstantValue value);

    /**
     * Compute the result of the comparison: <code>this == value</code>.
     *
     * @param value Value to be compared with this one.
     * @return A one-bit unsigned integer value that has the value
     *         <code>1</code> if this value is equal to the given
     *         one and <code>0</code> otherwise.
     * @throws IllegalArgumentException The comparison operation cannot be
     *                                  performed, e.g. because the type of the
     *                                  parameter is different from the type of
     *                                  this constant.
     */
    ConstantValue equalTo(ConstantValue value);

    /**
     * Compute the result of the comparison: <code>this != value</code>.
     *
     * @param value Value to be compared with this one.
     * @return A one-bit unsigned integer value that has the value
     *         <code>1</code> if this value is not equal to the given
     *         one and <code>0</code> otherwise.
     * @throws IllegalArgumentException The comparison operation cannot be
     *                                  performed, e.g. because the type of the
     *                                  parameter is different from the type of
     *                                  this constant.
     */
    ConstantValue notEqualTo(ConstantValue value);

    /**
     * Compute the bitwise AND of the bits that represent this value and the
     * given one.
     *
     * @param value The second parameter for the bitwise AND operation.
     * @return Result of the bitwise AND operation <code>this & value</code>.
     * @throws UnsupportedOperationException The method is invoked on a floating
     *                                       constant.
     * @throws IllegalArgumentException The operation cannot be performed, e.g.
     *                                  because the type of this value is
     *                                  different from the type of the given
     *                                  value.
     */
    ConstantValue bitwiseAnd(ConstantValue value);

    /**
     * Compute the bitwise OR of the bits that represent this value and the
     * given one.
     *
     * @param value The second parameter for the bitwise OR operation.
     * @return Result of the bitwise OR operation <code>this | value</code>.
     * @throws UnsupportedOperationException The method is invoked on a floating
     *                                       constant.
     * @throws IllegalArgumentException The operation cannot be performed, e.g.
     *                                  because the type of this value is
     *                                  different from the type of the given
     *                                  value.
     */
    ConstantValue bitwiseOr(ConstantValue value);

    /**
     * Compute the bitwise XOR of the bits that represent this value and the
     * given one.
     *
     * @param value The second parameter for the bitwise XOR operation.
     * @return Result of the bitwise XOR operation (<code>this ^ value</code>).
     * @throws UnsupportedOperationException The method is invoked on a floating
     *                                       constant.
     * @throws IllegalArgumentException The operation cannot be performed, e.g.
     *                                  because the type of this value is
     *                                  different from the type of the given
     *                                  value.
     */
    ConstantValue bitwiseXor(ConstantValue value);

    /**
     * Compute the bitwise NOT of the bits that represent this value.
     *
     * @return Result of the bitwise NOT operation (<code>~this</code>).
     * @throws UnsupportedOperationException The method is invoked on a floating
     *                                       constant.
     */
    ConstantValue bitwiseNot();

    /**
     * Compute the negation of this value.
     *
     * @return Negation of this value (<code>-this</code>).
     */
    ConstantValue negate();

    /**
     * Perform the cast operation on this value to the given type.
     *
     * @param targetType Type of the returned constant.
     * @return A constant value of the specified type with the value of
     *         <code>this</code> converted to the target type.
     */
    ConstantValue castTo(ConstantType targetType);

    /**
     * Compute the logical negation of this value.
     *
     * @return An unsigned one-bit constant value that represents <code>1</code>
     *         if this value is equal to 0. Otherwise, the returned value
     *         represents <code>0</code>.
     */
    ConstantValue logicalNot();
}
