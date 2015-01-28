package pl.edu.mimuw.nesc.constexpr.value;

import pl.edu.mimuw.nesc.constexpr.value.type.FloatConstantType;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class FloatConstantValue extends AbstractConstantValue<Float> {
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
    public FloatConstantValue add(ConstantValue toAdd) {
        final FloatConstantValue valueToAdd = checkConstant(toAdd);
        return new FloatConstantValue(getValue() + valueToAdd.getValue());
    }

    @Override
    public FloatConstantValue subtract(ConstantValue toSubtract) {
        final FloatConstantValue valueToSubtract = checkConstant(toSubtract);
        return new FloatConstantValue(getValue() - valueToSubtract.getValue());
    }
}
