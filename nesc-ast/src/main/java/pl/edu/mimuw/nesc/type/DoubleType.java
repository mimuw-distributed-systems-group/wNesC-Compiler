package pl.edu.mimuw.nesc.type;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.external.ExternalScheme;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reflects the <code>double</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class DoubleType extends FloatingType {
    public DoubleType(boolean constQualified, boolean volatileQualified,
            Optional<ExternalScheme> externalScheme) {
        super(constQualified, volatileQualified, externalScheme);
    }

    public DoubleType() {
        this(false, false, Optional.<ExternalScheme>absent());
    }

    @Override
    public final boolean isRealType() {
        return true;
    }

    @Override
    public final DoubleType addQualifiers(boolean addConst, boolean addVolatile,
                                          boolean addRestrict) {
        return new DoubleType(
                addConstQualifier(addConst),
                addVolatileQualifier(addVolatile),
                getExternalScheme()
        );
    }

    @Override
    public final DoubleType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                           boolean removeRestrict) {
        return new DoubleType(
                removeConstQualifier(removeConst),
                removeVolatileQualifier(removeVolatile),
                getExternalScheme()
        );
    }

    @Override
    public final DoubleType addExternalScheme(ExternalScheme externalScheme) {
        checkNotNull(externalScheme, "external scheme cannot be null");
        return new DoubleType(isConstQualified(), isVolatileQualified(), Optional.of(externalScheme));
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
