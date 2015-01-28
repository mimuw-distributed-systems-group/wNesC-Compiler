package pl.edu.mimuw.nesc.constexpr.value;

import pl.edu.mimuw.nesc.constexpr.value.type.DoubleConstantType;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class DoubleConstantValue extends AbstractConstantValue<Double> {
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
    public DoubleConstantValue add(ConstantValue toAdd) {
        final DoubleConstantValue valueToAdd = checkConstant(toAdd);
        return new DoubleConstantValue(getValue() + valueToAdd.getValue());
    }

    @Override
    public DoubleConstantValue subtract(ConstantValue toSubtract) {
        final DoubleConstantValue valueToSubtract = checkConstant(toSubtract);
        return new DoubleConstantValue(getValue() - valueToSubtract.getValue());
    }
}
