package pl.edu.mimuw.nesc.type;

/**
 * A class that represents an unknown type T. It is known that T is an
 * arithmetic type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class UnknownArithmeticType extends UnknownType {
    private static final String NAME_SUBSTITUTE = "?arithmetic?";
    private static final UnknownArithmeticType UNNAMED_INSTANCE = new UnknownArithmeticType(NAME_SUBSTITUTE);

    /**
     * Get the instance of this class that represents an unnamed unknown
     * and unqualified arithmetic type.
     *
     * @return The instance of <code>UnknownArithmeticType</code> class.
     */
    public static UnknownArithmeticType unnamed() {
        return UNNAMED_INSTANCE;
    }

    protected UnknownArithmeticType(String name) {
        this(false, false, name);
    }

    protected UnknownArithmeticType(boolean constQualified, boolean volatileQualified,
            String name) {
        super(constQualified, volatileQualified, name);
    }

    @Override
    public UnknownArithmeticType removeName() {
        return new UnknownArithmeticType(isConstQualified(), isVolatileQualified(), NAME_SUBSTITUTE);
    }

    @Override
    public final boolean isUnknownArithmeticType() {
        return true;
    }

    @Override
    public final UnknownArithmeticType promote() {
        return removeName();
    }

    @Override
    public UnknownArithmeticType addQualifiers(boolean addConst, boolean addVolatile,
            boolean addRestrict) {

        return new UnknownArithmeticType(addConstQualifier(addConst),
                addVolatileQualifier(addVolatile), getName());
    }

    @Override
    public UnknownArithmeticType removeQualifiers(boolean removeConst, boolean removeVolatile,
            boolean removeRestrict) {

        return new UnknownArithmeticType(removeConstQualifier(removeConst),
                removeVolatileQualifier(removeVolatile), getName());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
