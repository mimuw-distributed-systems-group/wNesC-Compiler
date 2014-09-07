package pl.edu.mimuw.nesc.ast.type;

/**
 * An artificial type of type definitions, e.g.
 * <code>typedef unsigned long long size_t;</code>.
 *
 * It follows the Singleton design pattern.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class TypeDefinitionType implements Type {
    /**
     * The only instance of this class.
     */
    private static final TypeDefinitionType instance = new TypeDefinitionType();

    /**
     * @return The only instance of this class.
     */
    public static TypeDefinitionType getInstance() {
        return instance;
    }

    /**
     * Private constructor for the Singleton design pattern.
     */
    private TypeDefinitionType() {}

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
    public final boolean isFloatingType() {
        return false;
    }

    @Override
    public final boolean isRealType() {
        return false;
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final boolean isScalarType() {
        return false;
    }

    @Override
    public final boolean isVoid() {
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
    public final boolean isTypeDefinition() {
        return true;
    }

    @Override
    public final boolean isPointerType() {
        return false;
    }

    @Override
    public final Type addQualifiers(boolean constQualified,
            boolean volatileQualified, boolean restrictQualified) {
        throw new UnsupportedOperationException("adding qualifiers to an artificial type");
    }

    @Override
    public final Type promote() {
        throw new UnsupportedOperationException("promoting an artificial type");
    }

    @Override
    public final boolean isConstQualified() {
        return false;
    }

    @Override
    public final boolean isVolatileQualified() {
        return false;
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    @Override
    public final String toString() {
        final PrintVisitor visitor = new PrintVisitor();
        accept(visitor, false);
        return visitor.get();
    }
}
