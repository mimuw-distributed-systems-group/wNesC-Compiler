package pl.edu.mimuw.nesc.type;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.external.ExternalScheme;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class UnsignedIntegerType extends IntegerType {
    protected UnsignedIntegerType(boolean constQualified, boolean volatileQualified,
            Optional<ExternalScheme> externalScheme) {
        super(constQualified, volatileQualified, externalScheme);
    }

    @Override
    public final boolean isSignedIntegerType() {
        return false;
    }

    @Override
    public final boolean isUnsignedIntegerType() {
        return true;
    }

    @Override
    public final boolean isComplete() {
        return true;
    }

    /**
     * @return Newly created object that represents the corresponding signed
     *         integer type with the same type qualifiers. The returned type is
     *         not external.
     */
    public abstract SignedIntegerType getSignedIntegerType();
}
