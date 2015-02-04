package pl.edu.mimuw.nesc.constexpr.value.factory;

import java.math.BigInteger;
import pl.edu.mimuw.nesc.constexpr.value.IntegerConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.type.IntegerConstantType;

/**
 * <p>Operations for creating objects related to integer constants.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface IntegerConstantFactory {
    /**
     * Create a new integer type object. The exact class of the created object
     * and count of bits of the type depend on the implementation of this
     * interface.
     *
     * @return Newly created instance of an integer constant type.
     */
    IntegerConstantType newType();

    /**
     * Create a new integer constant with given value. The exact class of the
     * created object and its type depend on the implementation of this
     * interface.
     *
     * @param value Value of the created constant.
     * @return Newly created integer constant with given value.
     */
    IntegerConstantValue<?> newValue(BigInteger value);

    /**
     * Equivalent to <code>newValue(new BigInteger(value))</code>.
     *
     * @param value String representation of the value of the created constant.
     * @return Newly created integer constant with given value.
     * @throws NumberFormatException <code>value</code> is not a valid
     *                               representation of a big integer.
     */
    IntegerConstantValue<?> newValue(String value);

    /**
     * Equivalent to <code>newValue(BigInteger.valueOf(value))</code>.
     *
     * @param value Value of the created constant.
     * @return Newly created integer constant with given value.
     */
    IntegerConstantValue<?> newValue(long value);
}
