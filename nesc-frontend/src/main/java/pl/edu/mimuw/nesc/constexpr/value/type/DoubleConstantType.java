package pl.edu.mimuw.nesc.constexpr.value.type;

/**
 * <p>Class that represents <code>double</code> type of a constant value. It
 * follows the singleton pattern.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class DoubleConstantType extends FloatingConstantType {
    /**
     * The only instance of this constant type.
     */
    private static final DoubleConstantType INSTANCE = new DoubleConstantType();

    /**
     * Get the only instance of this class.
     *
     * @return The only instance of <code>DoubleConstantType</code>.
     */
    public static DoubleConstantType getInstance() {
        return INSTANCE;
    }

    /**
     * Private constructor for the singleton pattern.
     */
    private DoubleConstantType() {
    }

    @Override
    public Type getType() {
        return Type.DOUBLE;
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
