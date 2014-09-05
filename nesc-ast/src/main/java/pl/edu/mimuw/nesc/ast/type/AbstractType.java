package pl.edu.mimuw.nesc.ast.type;

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

    @Override
    public final boolean isTypeDefinition() {
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
    public final String toString() {
        final PrintVisitor visitor = new PrintVisitor();
        accept(visitor, false);
        return visitor.get();
    }
}
