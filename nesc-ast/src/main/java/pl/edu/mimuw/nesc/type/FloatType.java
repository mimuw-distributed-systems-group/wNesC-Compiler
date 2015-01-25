package pl.edu.mimuw.nesc.type;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.external.ExternalScheme;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reflects the <code>float</code> type.
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class FloatType extends FloatingType {
    public FloatType(boolean constQualified, boolean volatileQualified,
            Optional<ExternalScheme> externalScheme) {
        super(constQualified, volatileQualified, externalScheme);
    }

    public FloatType() {
        this(false, false, Optional.<ExternalScheme>absent());
    }

    @Override
    public final boolean isRealType() {
        return true;
    }

    @Override
    public final FloatType addQualifiers(boolean addConst, boolean addVolatile,
                                         boolean addRestrict) {
        return new FloatType(
                addConstQualifier(addConst),
                addVolatileQualifier(addVolatile),
                getExternalScheme()
        );
    }

    @Override
    public final FloatType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                            boolean removeRestrict) {
        return new FloatType(
                removeConstQualifier(removeConst),
                removeVolatileQualifier(removeVolatile),
                getExternalScheme()
        );
    }

    @Override
    public final FloatType addExternalScheme(ExternalScheme externalScheme) {
        checkNotNull(externalScheme, "external scheme cannot be null");
        return new FloatType(isConstQualified(), isVolatileQualified(), Optional.of(externalScheme));
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
