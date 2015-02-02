package pl.edu.mimuw.nesc.constexpr.value.factory;

import pl.edu.mimuw.nesc.constexpr.value.IntegerConstantValue;
import pl.edu.mimuw.nesc.constexpr.value.type.IntegerConstantType;

/**
 * <p>Operations for creating objects related to integer constants.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface IntegerConstantFactory {
    /**
     * Create a new integer type object with given count of bits. The exact
     * class of the created object depends on the implementation of this
     * interface.
     *
     * @param bitsCount Count of bits in the returned type.
     * @return Newly created instance with the given count of bits.
     */
    IntegerConstantType newType(int bitsCount);

    /**
     * Create a new integer constant with given value and proper type of given
     * bits counts. The exact class of the created object depends on the
     * implementation of this interface.
     *
     * @param value Value of the created constant.
     * @param bitsCount Count of bits of the type of the created constant.
     * @return Newly created integer constant with given value and type of given
     *         count of bits.
     * @throws NumberFormatException <code>value</code> is not a valid
     *                               representation of a big integer.
     */
    IntegerConstantValue<?> newValue(String value, int bitsCount);
}
