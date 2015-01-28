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
     * Adds the given constant value to this one. The argument and this value
     * are unchanged after the call.
     *
     * @param toAdd Constant value to add to this one.
     * @return Result of the addition.
     * @throws IllegalArgumentException The given value cannot be added to this
     *                                  one (e.g. because it has a different
     *                                  type).
     */
    ConstantValue add(ConstantValue toAdd);

    /**
     * Subtracts the given value from this one. The argument and this value
     * are left unchanged.
     *
     * @param toSubtract Value to be subtracted from this one.
     * @return Result of the subtraction.
     * @throws IllegalArgumentException The given value cannot be subtracted
     *                                  from this one (e.g. becuase it has
     *                                  a different type).
     */
    ConstantValue subtract(ConstantValue toSubtract);
}
