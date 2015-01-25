package pl.edu.mimuw.nesc.type;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.external.ExternalScheme;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class ArithmeticType extends AbstractType {
    /**
     * The external scheme associated with this type.
     */
    private final Optional<ExternalScheme> externalScheme;

    protected ArithmeticType(boolean constQualified, boolean volatileQualified,
            Optional<ExternalScheme> externalScheme) {
        super(constQualified, volatileQualified);
        checkNotNull(externalScheme, "external scheme cannot be null");
        this.externalScheme = externalScheme;
    }

    @Override
    public final boolean isArithmetic() {
        return true;
    }

    @Override
    public final boolean isScalarType() {
        return true;
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
    public final boolean isPointerType() {
        return false;
    }

    @Override
    public final boolean isArrayType() {
        return false;
    }

    @Override
    public final boolean isObjectType() {
        return true;
    }

    @Override
    public final boolean isFunctionType() {
        return false;
    }

    @Override
    public final boolean isUnknownType() {
        return false;
    }

    @Override
    public final boolean isUnknownArithmeticType() {
        return false;
    }

    @Override
    public final boolean isUnknownIntegerType() {
        return false;
    }

    @Override
    public final boolean isModifiable() {
        return isComplete() && !isConstQualified();
    }

    @Override
    public abstract ArithmeticType promote();

    @Override
    public final Type decay() {
        return this;
    }

    /**
     * Get the external scheme associated with this type. It is present if and
     * only if this type is an external base type.
     *
     * @return The external scheme associated with this type.
     */
    public final Optional<ExternalScheme> getExternalScheme() {
        return this.externalScheme;
    }

    /**
     * Create a new instance of the same type that differs only in the external
     * scheme. The given one is contained in the returned type.
     *
     * @param externalScheme External scheme to associate with the returned
     *                       type.
     * @return Newly created instance of the same type as this that differs only
     *         in that it is associated with the given external type.
     * @throws NullPointerException The given argument is <code>null</code>.
     * @throws UnsupportedOperationException The method is invoked on an
     *                                       enumerated type.
     */
    public abstract ArithmeticType addExternalScheme(ExternalScheme externalScheme);

    @Override
    public final boolean isExternal() {
        return this.externalScheme.isPresent();
    }

    @Override
    public final boolean isExternalBaseType() {
        return this.externalScheme.isPresent();
    }
}
