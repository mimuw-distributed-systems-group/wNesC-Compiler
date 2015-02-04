package pl.edu.mimuw.nesc.constexpr.value;

import java.math.BigDecimal;
import java.math.BigInteger;
import pl.edu.mimuw.nesc.constexpr.value.decode.*;
import pl.edu.mimuw.nesc.constexpr.value.type.*;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class DoubleConstantValue extends FloatingConstantValue<Double> {
    /**
     * Create a constant of <code>double</code> type.
     *
     * @param value Value of the created constant.
     */
    public DoubleConstantValue(double value) {
        super(value);
    }

    @Override
    public DoubleConstantType getType() {
        return DoubleConstantType.getInstance();
    }

    @Override
    public boolean logicalValue() {
        return getValue() != .0;
    }

    @Override
    public DoubleConstantValue add(ConstantValue value) {
        return new DoubleConstantValue(getValue() + prepareRhsOperand(value));
    }

    @Override
    public DoubleConstantValue subtract(ConstantValue value) {
        return new DoubleConstantValue(getValue() - prepareRhsOperand(value));
    }

    @Override
    public DoubleConstantValue multiply(ConstantValue value) {
        return new DoubleConstantValue(getValue() * prepareRhsOperand(value));
    }

    @Override
    public DoubleConstantValue divide(ConstantValue value) {
        return new DoubleConstantValue(getValue() / prepareRhsOperand(value));
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
        return UnsignedIntegerConstantValue.getLogicalValue((double) getValue() == prepareRhsOperand(value));
    }

    @Override
    public UnsignedIntegerConstantValue notEqualTo(ConstantValue value) {
        return UnsignedIntegerConstantValue.getLogicalValue((double) getValue() != prepareRhsOperand(value));
    }

    @Override
    public DoubleConstantValue negate() {
        return new DoubleConstantValue(-getValue());
    }

    @Override
    public UnsignedIntegerConstantValue logicalNot() {
        return UnsignedIntegerConstantValue.getLogicalValue(getValue() == 0.0);
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
                return new FloatConstantValue((float) (double) getValue());
            case DOUBLE:
                return this;
            default:
                throw new RuntimeException("unexpected target type '"
                        + targetType.getType() + "'");
        }
    }
}
