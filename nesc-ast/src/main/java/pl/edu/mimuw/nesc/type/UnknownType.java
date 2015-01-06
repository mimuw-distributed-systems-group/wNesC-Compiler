package pl.edu.mimuw.nesc.type;

import static com.google.common.base.Preconditions.*;

/**
 * A class that represents an unknown type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class UnknownType extends AbstractType {
    private static final String NAME_SUBSTITUTE = "??";
    private static final UnknownType UNNAMED_INSTANCE = new UnknownType(NAME_SUBSTITUTE);

    /**
     * Name of the unknown type that is used to refer to it:
     *
     *     generic module M(typedef T)
     *                              ^
     *                              ^
     *                              ^
     */
    private final String name;

    /**
     * Get the instance of this class that represents an unnamed unknown and
     * unqualified type.
     *
     * @return The instance of <code>UnknownType</code> class.
     */
    public static UnknownType unnamed() {
        return UNNAMED_INSTANCE;
    }

    /**
     * Initializes this object with the given name.
     *
     * @param name Name that is used to refer to the type.
     */
    protected UnknownType(String name) {
        this(false, false, name);
    }

    /**
     * Initializes this object with given parameters.
     *
     * @param constQualified <code>true</code> if and only if this type is
     *                       const-qualified.
     * @param volatileQualified <code>true</code> if and only if this type is
     *                          volatile-qualified.
     * @param name Name that is used to refer to this type.
     * @throws NullPointerException Given name is null.
     * @throws IllegalArgumentException Given name is an empty string.
     */
    protected UnknownType(boolean constQualified, boolean volatileQualified,
            String name) {
        super(constQualified, volatileQualified);

        checkNotNull(name, "name of an unknown type cannot be null");
        checkArgument(!name.isEmpty(), "name of an unknown type cannot be an empty string");

        this.name = name;
    }

    /**
     * Get the name of the type used in the place of its declaration.
     *
     * @return Name of the type.
     */
    public final String getName() {
        return name;
    }

    /**
     * Get the same type as this but with its name replaced with an identifier
     * that indicates an unknown name. Qualification of this type is preserved.
     *
     * @return Newly created instance of this type but with its name removed.
     */
    public UnknownType removeName() {
        return new UnknownType(isConstQualified(), isVolatileQualified(),
                NAME_SUBSTITUTE);
    }

    @Override
    public boolean isUnknownArithmeticType() {
        return false;
    }

    @Override
    public boolean isUnknownIntegerType() {
        return false;
    }

    @Override
    public UnknownType promote() {
        return this;
    }

    @Override
    public UnknownType addQualifiers(boolean addConst, boolean addVolatile,
            boolean addRestrict) {

        return new UnknownType(addConstQualifier(addConst),
                addVolatileQualifier(addVolatile), name);
    }

    @Override
    public UnknownType removeQualifiers(boolean removeConst, boolean removeVolatile,
            boolean removeRestrict) {

        return new UnknownType(removeConstQualifier(removeConst),
                removeVolatileQualifier(removeVolatile), name);
    }

    @Override
    public final boolean isModifiable() {
        return !isConstQualified();
    }

    @Override
    public final UnknownType decay() {
        return this;
    }

    @Override
    public final boolean isUnknownType() {
        return true;
    }

    @Override
    public final boolean isComplete() {
        return true;
    }

    @Override
    public final boolean isObjectType() {
        return true;
    }

    @Override
    public final boolean isCompatibleWith(Type otherType) {
        if (!super.isCompatibleWith(otherType)) {
            return false;
        }

        final UnknownType otherUnknown = (UnknownType) otherType;
        return getName().equals(otherUnknown.getName());
    }

    @Override
    public final boolean isArithmetic() {
        return false;
    }

    @Override
    public final boolean isIntegerType() {
        return false;
    }

    @Override
    public final boolean isSignedIntegerType() {
        return false;
    }

    @Override
    public final boolean isUnsignedIntegerType() {
        return false;
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final boolean isVoid() {
        return false;
    }

    @Override
    public final boolean isRealType() {
        return false;
    }

    @Override
    public final boolean isFloatingType() {
        return false;
    }

    @Override
    public final boolean isArrayType() {
        return false;
    }

    @Override
    public final boolean isPointerType() {
        return false;
    }

    @Override
    public final boolean isScalarType() {
        return false;
    }

    @Override
    public final boolean isFunctionType() {
        return false;
    }

    @Override
    public final boolean isDerivedType() {
        return false;
    }

    @Override
    public final boolean isFieldTagType() {
        return false;
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
