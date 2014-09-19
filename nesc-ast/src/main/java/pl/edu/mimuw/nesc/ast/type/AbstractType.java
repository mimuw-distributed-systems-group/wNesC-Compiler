package pl.edu.mimuw.nesc.ast.type;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class for all true types. All types can be const and volatile-qualified
 * and this class allows specifying it.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class AbstractType implements Type {
    /**
     * <code>true</code> if and only if this type is const-qualified.
     */
    private final boolean constQualified;

    /**
     * <code>true</code> if and only if this type is volatile-qualified.
     */
    private final boolean volatileQualified;

    /**
     * Copies values from parameters to internal variables.
     */
    protected AbstractType(boolean constQualified, boolean volatileQualified) {
        this.constQualified = constQualified;
        this.volatileQualified = volatileQualified;
    }

    /**
     * @return <code>true</code> if and only if this type is const-qualified or
     *         the qualifier is to be added.
     */
    protected final boolean addConstQualifier(boolean add) {
        return isConstQualified() || add;
    }

    /**
     * @return <code>true</code> if and only if this type is volatile-qualified
     *         or the qualifier it to be added.
     */
    protected final boolean addVolatileQualifier(boolean add) {
        return isVolatileQualified() || add;
    }

    /**
     * @return <code>true</code> if and only if this type is const-qualified and
     *         the qualifier is not to be removed.
     */
    protected final boolean removeConstQualifier(boolean remove) {
        return !remove && isConstQualified();
    }

    /**
     * @return <code>true</code> if and only if this type is volatile-qualified
     *         and the qualifier is not to be removed.
     */
    protected final boolean removeVolatileQualifier(boolean remove) {
        return !remove && isVolatileQualified();
    }

    @Override
    public final Type removeQualifiers() {
        return removeQualifiers(true, true, true);
    }

    @Override
    public final boolean isTypeDefinition() {
        return false;
    }

    @Override
    public final boolean isArtificialType() {
        return false;
    }

    @Override
    public final boolean isConstQualified() {
        return constQualified;
    }

    @Override
    public final boolean isVolatileQualified() {
        return volatileQualified;
    }

    @Override
    public boolean isCompatibleWith(Type type) {
        checkNotNull(type, "type checked for compatibility cannot be null");

        return getClass() == type.getClass()
               && isConstQualified() == type.isConstQualified()
               && isVolatileQualified() == type.isVolatileQualified();
    }

    @Override
    public final String toString() {
        final PrintVisitor visitor = new PrintVisitor();
        accept(visitor, false);
        return visitor.get();
    }
}
