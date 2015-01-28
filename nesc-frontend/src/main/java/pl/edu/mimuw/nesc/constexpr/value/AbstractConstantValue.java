package pl.edu.mimuw.nesc.constexpr.value;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Skeletal implementation of the constant value interface.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class AbstractConstantValue<T> implements ConstantValue {
    /**
     * The value of this constant.
     */
    private final T value;

    protected AbstractConstantValue(T value) {
        checkNotNull(value, "value cannot be null");
        this.value = value;
    }

    /**
     * Get the value of this constant.
     *
     * @return Value of this constant. It is never <code>null</code>.
     */
    public T getValue() {
        return value;
    }

    /**
     * Function that simplifies checking arguments for arithmetic operations.
     * It throws an exception if the argument is <code>null</code> or of
     * a different type than this constant.
     *
     * @param value Constant value to check.
     * @param <V> Class to cast the constant to.
     * @return The given value casted to the type parameter.
     */
    @SuppressWarnings("unchecked")
    protected <V extends ConstantValue> V checkConstant(ConstantValue value) {
        checkNotNull(value, "value cannot be null");
        checkArgument(value.getType().equals(getType()), "type of the given value differs");
        return (V) value;
    }
}
