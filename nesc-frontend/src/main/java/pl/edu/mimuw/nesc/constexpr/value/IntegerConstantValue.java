package pl.edu.mimuw.nesc.constexpr.value;

import java.math.BigInteger;
import pl.edu.mimuw.nesc.constexpr.value.decode.*;
import pl.edu.mimuw.nesc.constexpr.value.type.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Ancestor of all integer values.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class IntegerConstantValue<T extends IntegerConstantValue<T>> extends AbstractConstantValue<BigInteger> {
    /**
     * Decoder of values in the representation of the type of this integer
     * value.
     */
    private final Decoder decoder;

    protected IntegerConstantValue(BigInteger value, Decoder decoder) {
        super(value);
        checkNotNull(decoder, "decoder cannot be null");
        this.decoder = decoder;
    }

    @Override
    public abstract IntegerConstantType getType();

    @Override
    public T add(ConstantValue value) {
        return newDecoded(getValue().add(prepareRhsOperand(value)));
    }

    @Override
    public T subtract(ConstantValue value) {
        return newDecoded(getValue().subtract(prepareRhsOperand(value)));
    }

    @Override
    public T multiply(ConstantValue value) {
        return newDecoded(getValue().multiply(prepareRhsOperand(value)));
    }

    @Override
    public T divide(ConstantValue value) {
        return newDecoded(getValue().divide(prepareRhsOperand(value)));
    }

    @Override
    public T remainder(ConstantValue value) {
        return newDecoded(getValue().remainder(prepareRhsOperand(value)));
    }

    @Override
    public T shiftLeft(ConstantValue value) {
        return newDecoded(getValue().shiftLeft(prepareShiftCount(value)));
    }

    @Override
    public T shiftRight(ConstantValue value) {
        return newValue(getValue().shiftRight(prepareShiftCount(value)));
    }

    @Override
    public T bitwiseAnd(ConstantValue value) {
        return newValue(getValue().and(prepareRhsOperand(value)));
    }

    @Override
    public T bitwiseXor(ConstantValue value) {
        return newValue(getValue().xor(prepareRhsOperand(value)));
    }

    @Override
    public T bitwiseOr(ConstantValue value) {
        return newValue(getValue().or(prepareRhsOperand(value)));
    }

    @Override
    public T bitwiseNot() {
        return newValue(getValue().not());
    }

    @Override
    public UnsignedIntegerConstantValue less(ConstantValue value) {
        return UnsignedIntegerConstantValue.getLogicalValue(compareWith(value) < 0);
    }

    @Override
    public UnsignedIntegerConstantValue lessOrEqual(ConstantValue value) {
        return UnsignedIntegerConstantValue.getLogicalValue(compareWith(value) <= 0);
    }

    @Override
    public UnsignedIntegerConstantValue greater(ConstantValue value) {
        return UnsignedIntegerConstantValue.getLogicalValue(compareWith(value) > 0);
    }

    @Override
    public UnsignedIntegerConstantValue greaterOrEqual(ConstantValue value) {
        return UnsignedIntegerConstantValue.getLogicalValue(compareWith(value) >= 0);
    }

    @Override
    public UnsignedIntegerConstantValue equalTo(ConstantValue value) {
        return UnsignedIntegerConstantValue.getLogicalValue(compareWith(value) == 0);
    }

    @Override
    public UnsignedIntegerConstantValue notEqualTo(ConstantValue value) {
        return UnsignedIntegerConstantValue.getLogicalValue(compareWith(value) != 0);
    }

    @Override
    public T negate() {
        return newDecoded(getValue().negate());
    }

    @Override
    public UnsignedIntegerConstantValue logicalNot() {
        return UnsignedIntegerConstantValue.getLogicalValue(getValue().equals(BigInteger.ZERO));
    }

    @Override
    public ConstantValue castTo(ConstantType targetType) {
        checkNotNull(targetType, "the destination type cannot be null");

        switch (targetType.getType()) {
            case SIGNED_INTEGER: {
                final SignedIntegerConstantType type = (SignedIntegerConstantType) targetType;
                final Decoder decoder = new TwosComplementDecoder(type.getBitsCount());
                return new SignedIntegerConstantValue(decoder.decode(getValue()), type);
            }
            case UNSIGNED_INTEGER: {
                final UnsignedIntegerConstantType type = (UnsignedIntegerConstantType) targetType;
                final Decoder decoder = new NaturalBinaryCodeDecoder(type.getBitsCount());
                return new UnsignedIntegerConstantValue(decoder.decode(getValue()), type);
            }
            case FLOAT:
                return new FloatConstantValue(getValue().floatValue());
            case DOUBLE:
                return new DoubleConstantValue(getValue().doubleValue());
            default:
                throw new RuntimeException("unexpected constant type '"
                        + targetType.getType() + "'");
        }
    }

    private int prepareShiftCount(ConstantValue shiftCountValue) {
        final IntegerConstantValue<?> integerValue = requireInteger(shiftCountValue);
        final BigInteger bitsCount = BigInteger.valueOf(getType().getBitsCount());
        return integerValue.getValue().abs().mod(bitsCount).intValue();
    }

    private int compareWith(ConstantValue value) {
        return getValue().compareTo(prepareRhsOperand(value));
    }

    /**
     * Create a new integer constant value of the same type as the type of this
     * constant value.
     *
     * @param value Value of the constant to create.
     * @return Newly created integer value of the same type as this with the
     *         given value.
     */
    protected abstract T newValue(BigInteger value);

    /**
     * Interprets the two's complement representation of given value in the
     * representation of the type of this constant.
     *
     * @param value Value to decode.
     * @return Decoded value of the representation of the given value.
     */
    private BigInteger decode(BigInteger value) {
        return decoder.decode(value);
    }

    /**
     * Create a new integer constant of the same type as this constant. The
     * value of the returned constant is the given value after truncation.
     *
     * @param value Value to truncate and assign to the returned constant.
     * @return Newly created constant of the same type as this. The value of the
     *         returned constant is the given value after truncation.
     */
    private T newDecoded(BigInteger value) {
        return newValue(decode(value));
    }
}
