package pl.edu.mimuw.nesc.constexpr.value.type;

/**
 * <p>Class that represents <code>float</code> type of a constant. It follows
 * the singleton pattern.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class FloatConstantType extends FloatingConstantType {
    /**
     * The only instance of this class.
     */
    private static final FloatConstantType INSTANCE = new FloatConstantType();

    /**
     * Get the only instance of this class.
     *
     * @return The only instance of <code>FloatConstantType</code> class.
     */
    public static FloatConstantType getInstance() {
        return INSTANCE;
    }

    /**
     * Private constructor for the singleton pattern.
     */
    private FloatConstantType() {
    }

    @Override
    public Type getType() {
        return Type.FLOAT;
    }

    @Override
    public boolean equals(Object otherObject) {
        return otherObject == INSTANCE;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
