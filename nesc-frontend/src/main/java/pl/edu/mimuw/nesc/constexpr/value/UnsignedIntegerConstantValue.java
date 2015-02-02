package pl.edu.mimuw.nesc.constexpr.value;

import java.math.BigInteger;
import pl.edu.mimuw.nesc.constexpr.value.decode.NaturalBinaryCodeDecoder;
import pl.edu.mimuw.nesc.constexpr.value.type.UnsignedIntegerConstantType;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UnsignedIntegerConstantValue extends IntegerConstantValue<UnsignedIntegerConstantValue> {
    /**
     * Type of an unsigned integer value that is assumed to contain a boolean
     * value.
     */
    private static final UnsignedIntegerConstantType LOGICAL_INTEGER_TYPE = new UnsignedIntegerConstantType(1);

    /**
     * The one-bit unsigned integer constant that represents boolean value
     * <code>true</code>.
     */
    private static final UnsignedIntegerConstantValue TRUE_VALUE =
            new UnsignedIntegerConstantValue(BigInteger.ONE, LOGICAL_INTEGER_TYPE);

    /**
     * The one-bit unsigned integer constant that represents boolean value
     * <code>false</code>.
     */
    private static final UnsignedIntegerConstantValue FALSE_VALUE =
            new UnsignedIntegerConstantValue(BigInteger.ZERO, LOGICAL_INTEGER_TYPE);

    /**
     * Type of this constant.
     */
    private final UnsignedIntegerConstantType type;

    /**
     * Get a one-bit unsigned integer value that represents boolean value
     * <code>true</code>, i.e. it has value <code>1</code>.
     *
     * @return A one-bit unsigned integer with value <code>1</code>.
     */
    public static UnsignedIntegerConstantValue getTrueValue() {
        return TRUE_VALUE;
    }

    /**
     * Get a one-bit unsigned integer value that represents boolean value
     * <code>false</code>, i.e. it has value <code>0</code>.
     *
     * @return A one-bit unsigned integer with value <code>0</code>.
     */
    public static UnsignedIntegerConstantValue getFalseValue() {
        return FALSE_VALUE;
    }

    /**
     * Get a one-bit unsigned integer value that represents the given boolean
     * value, i.e. it has value <code>1</code> if the given parameter is
     * <code>true</code> or <code>0</code> otherwise.
     *
     * @param value Boolean value that reflects the value of the returned
     *              constant.
     * @return One-bit unsigned integer value that represents the given boolean
     *         value.
     */
    public static UnsignedIntegerConstantValue getLogicalValue(boolean value) {
        return value
                ? getTrueValue()
                : getFalseValue();
    }

    /**
     * Create an unsigned integer constant of the given value and type.
     *
     * @param value Value of the constant.
     * @param type Type of the constant.
     * @throws NullPointerException One of the arguments is <code>null</code>.
     * @throws IllegalArgumentException The given value is out of range for the
     *                                  given type.
     */
    public UnsignedIntegerConstantValue(BigInteger value, UnsignedIntegerConstantType type) {
        super(value, new NaturalBinaryCodeDecoder(type.getBitsCount()));
        checkNotNull(type, "type cannot be null");
        checkArgument(type.getRange().contains(value), "the given value is out of range of the given type");
        this.type = type;
    }

    @Override
    public UnsignedIntegerConstantType getType() {
        return type;
    }

    @Override
    protected UnsignedIntegerConstantValue newValue(BigInteger value) {
        return new UnsignedIntegerConstantValue(value, getType());
    }
}
