package pl.edu.mimuw.nesc.constexpr.value;

import pl.edu.mimuw.nesc.constexpr.value.type.FloatingConstantType;

/**
 * <p>Ancestor of all floating constants.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class FloatingConstantValue<F extends Number> extends AbstractConstantValue<F> {
    protected FloatingConstantValue(F value) {
        super(value);
    }

    @Override
    public abstract FloatingConstantType getType();

    @Override
    public final ConstantValue remainder(ConstantValue value) {
        throw new UnsupportedOperationException("cannot compute the remainder of a floating value");
    }

    @Override
    public final ConstantValue shiftLeft(ConstantValue value) {
        throw new UnsupportedOperationException("cannot shift to the left a floating value");
    }

    @Override
    public final ConstantValue shiftRight(ConstantValue value) {
        throw new UnsupportedOperationException("cannot shift to the right a floating value");
    }

    @Override
    public final ConstantValue bitwiseAnd(ConstantValue value) {
        throw new UnsupportedOperationException("cannot use a floating value for bitwise AND");
    }

    @Override
    public final ConstantValue bitwiseOr(ConstantValue value) {
        throw new UnsupportedOperationException("cannot use a floating value for bitwise OR");
    }

    @Override
    public final ConstantValue bitwiseXor(ConstantValue value) {
        throw new UnsupportedOperationException("cannot use a floating value for bitwise XOR");
    }

    @Override
    public final ConstantValue bitwiseNot() {
        throw new UnsupportedOperationException("cannot compute a bitwise NOT of a floating value");
    }
}
