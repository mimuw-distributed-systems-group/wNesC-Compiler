package pl.edu.mimuw.nesc.ast.type;

/**
 * A class that represents an unknown type T. It is known that T is an integer
 * type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UnknownIntegerType extends UnknownArithmeticType {
    private static final String NAME_SUBSTITUTE = "?integer?";
    private static final UnknownIntegerType UNNAMED_INSTANCE = new UnknownIntegerType(NAME_SUBSTITUTE);

    /**
     * Get the instance of this class that represents an unnamed unknown and
     * unqualified integer type.
     *
     * @return The instance of <code>UnknownIntegerType</code> class.
     */
    public static UnknownIntegerType unnamed() {
        return UNNAMED_INSTANCE;
    }

    protected UnknownIntegerType(String name) {
        this(false, false, name);
    }

    protected UnknownIntegerType(boolean constQualified, boolean volatileQualified,
            String name) {
        super(constQualified, volatileQualified, name);
    }

    @Override
    public final UnknownIntegerType removeName() {
        return new UnknownIntegerType(isConstQualified(), isVolatileQualified(), NAME_SUBSTITUTE);
    }

    @Override
    public final boolean isUnknownIntegerType() {
        return true;
    }

    @Override
    public UnknownIntegerType addQualifiers(boolean addConst, boolean addVolatile,
            boolean addRestrict) {

        return new UnknownIntegerType(addConstQualifier(addConst),
                addVolatileQualifier(addVolatile), getName());
    }

    @Override
    public UnknownIntegerType removeQualifiers(boolean removeConst, boolean removeVolatile,
            boolean removeRestrict) {

        return new UnknownIntegerType(removeConstQualifier(removeConst),
                removeVolatileQualifier(removeVolatile), getName());
    }
}
