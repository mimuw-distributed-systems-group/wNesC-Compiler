package pl.edu.mimuw.nesc.constexpr.value;

import java.math.BigInteger;
import pl.edu.mimuw.nesc.constexpr.value.type.UnsignedIntegerConstantType;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UnsignedIntegerConstantValue extends AbstractConstantValue<BigInteger> {
    /**
     * Type of this constant.
     */
    private final UnsignedIntegerConstantType type;

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
        super(value);
        checkNotNull(type, "type cannot be null");
        checkArgument(type.getRange().contains(value), "the given value is out of range of the given type");
        this.type = type;
    }

    @Override
    public UnsignedIntegerConstantType getType() {
        return type;
    }

    @Override
    public UnsignedIntegerConstantValue add(ConstantValue toAdd) {
        final UnsignedIntegerConstantValue valueToAdd = checkConstant(toAdd);
        final BigInteger result = wrapAround(getValue().add(valueToAdd.getValue()));
        return new UnsignedIntegerConstantValue(result, this.type);
    }

    @Override
    public UnsignedIntegerConstantValue subtract(ConstantValue toSubtract) {
        final UnsignedIntegerConstantValue valueToSubtract = checkConstant(toSubtract);
        final BigInteger result = wrapAround(getValue().subtract(valueToSubtract.getValue()));
        return new UnsignedIntegerConstantValue(result, this.type);
    }

    private BigInteger wrapAround(BigInteger value) {
        return type.getRange().contains(value)
                ? value
                : value.mod(type.getUnsignedBoundary());
    }
}
