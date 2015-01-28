package pl.edu.mimuw.nesc.constexpr.value;

import java.math.BigInteger;
import pl.edu.mimuw.nesc.constexpr.value.type.SignedIntegerConstantType;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class SignedIntegerConstantValue extends AbstractConstantValue<BigInteger> {
    /**
     * Type of this signed integer constant.
     */
    private final SignedIntegerConstantType type;

    /**
     * Create a constant of given value and type.
     *
     * @param value Value of the constant.
     * @param type Type of this constant.
     * @throws NullPointerException One of the arguments is <code>null</code>.
     * @throws IllegalArgumentException The given value is out of range of the
     *                                  given type.
     */
    public SignedIntegerConstantValue(BigInteger value, SignedIntegerConstantType type) {
        super(value);
        checkNotNull(type, "type of the constant cannot be null");
        checkArgument(type.getRange().contains(value), "the given value is out of range of the given type");
        this.type = type;
    }

    @Override
    public SignedIntegerConstantType getType() {
        return type;
    }

    @Override
    public SignedIntegerConstantValue add(ConstantValue toAdd) {
        final SignedIntegerConstantValue valueToAdd = checkConstant(toAdd);
        final BigInteger result = wrapAround(getValue().add(valueToAdd.getValue()));
        return new SignedIntegerConstantValue(result, this.type);
    }

    @Override
    public SignedIntegerConstantValue subtract(ConstantValue toSubtract) {
        final SignedIntegerConstantValue valueToSubtract = checkConstant(toSubtract);
        final BigInteger result = wrapAround(getValue().subtract(valueToSubtract.getValue()));
        return new SignedIntegerConstantValue(result, this.type);
    }

    private BigInteger wrapAround(BigInteger value) {
        return type.getRange().contains(value)
                ? value
                : value.add(type.getUnsignedDisplacement())
                    .mod(type.getUnsignedBoundary())
                    .subtract(type.getUnsignedDisplacement());
    }
}
