package pl.edu.mimuw.nesc.constexpr.value;

import java.math.BigDecimal;
import java.math.BigInteger;
import pl.edu.mimuw.nesc.constexpr.value.decode.Decoder;
import pl.edu.mimuw.nesc.constexpr.value.decode.NaturalBinaryCodeDecoder;
import pl.edu.mimuw.nesc.constexpr.value.decode.TwosComplementDecoder;
import pl.edu.mimuw.nesc.constexpr.value.type.*;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class FloatConstantValue extends FloatingConstantValue<Float> {
    /**
     * Create a constant of <code>float</code> type.
     *
     * @param value Value of the constant.
     */
    public FloatConstantValue(float value) {
        super(value);
    }

    @Override
    public FloatConstantType getType() {
        return FloatConstantType.getInstance();
    }

    @Override
    public FloatConstantValue add(ConstantValue value) {
        return new FloatConstantValue(getValue() + prepareRhsOperand(value));
    }

    @Override
    public FloatConstantValue subtract(ConstantValue value) {
        return new FloatConstantValue(getValue() - prepareRhsOperand(value));
    }

    @Override
    public FloatConstantValue multiply(ConstantValue value) {
        return new FloatConstantValue(getValue() * prepareRhsOperand(value));
    }

    @Override
    public FloatConstantValue divide(ConstantValue value) {
        return new FloatConstantValue(getValue() / prepareRhsOperand(value));
    }

    @Override
    public UnsignedIntegerConstantValue less(ConstantValue value) {
        return UnsignedIntegerConstantValue.getLogicalValue(getValue() < prepareRhsOperand(value));
    }

    @Override
    public UnsignedIntegerConstantValue lessOrEqual(ConstantValue value) {
        return UnsignedIntegerConstantValue.getLogicalValue(getValue() <= prepareRhsOperand(value));
    }

    @Override
    public UnsignedIntegerConstantValue greater(ConstantValue value) {
        return UnsignedIntegerConstantValue.getLogicalValue(getValue() > prepareRhsOperand(value));
    }

    @Override
    public UnsignedIntegerConstantValue greaterOrEqual(ConstantValue value) {
        return UnsignedIntegerConstantValue.getLogicalValue(getValue() >= prepareRhsOperand(value));
    }

    @Override
    public UnsignedIntegerConstantValue equalTo(ConstantValue value) {
        return UnsignedIntegerConstantValue.getLogicalValue((float) getValue() == prepareRhsOperand(value));
    }

    @Override
    public UnsignedIntegerConstantValue notEqualTo(ConstantValue value) {
        return UnsignedIntegerConstantValue.getLogicalValue((float) getValue() != prepareRhsOperand(value));
    }

    @Override
    public FloatConstantValue negate() {
        return new FloatConstantValue(-getValue());
    }

    @Override
    public UnsignedIntegerConstantValue logicalNot() {
        return UnsignedIntegerConstantValue.getLogicalValue(getValue() == 0.0f);
    }

    @Override
    public ConstantValue castTo(ConstantType targetType) {
        switch (targetType.getType()) {
            case SIGNED_INTEGER :{
                final SignedIntegerConstantType type = (SignedIntegerConstantType) targetType;
                final Decoder decoder = new TwosComplementDecoder(type.getBitsCount());
                final BigInteger integralPart = new BigDecimal(getValue()).toBigInteger();
                return new SignedIntegerConstantValue(decoder.decode(integralPart), type);
            }
            case UNSIGNED_INTEGER: {
                final UnsignedIntegerConstantType type = (UnsignedIntegerConstantType) targetType;
                final Decoder decoder = new NaturalBinaryCodeDecoder(type.getBitsCount());
                final BigInteger integralPart = new BigDecimal(getValue()).toBigInteger();
                return new UnsignedIntegerConstantValue(decoder.decode(integralPart), type);
            }
            case FLOAT:
                return this;
            case DOUBLE:
                return new DoubleConstantValue(getValue());
            default:
                throw new RuntimeException("unexpected target type '"
                        + targetType.getType() + "'");
        }
    }
}
