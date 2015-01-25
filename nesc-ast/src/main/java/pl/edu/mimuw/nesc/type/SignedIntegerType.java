package pl.edu.mimuw.nesc.type;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.external.ExternalScheme;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class SignedIntegerType extends IntegerType {
    protected SignedIntegerType(boolean constQualified, boolean volatileQualified,
            Optional<ExternalScheme> externalScheme) {
        super(constQualified, volatileQualified, externalScheme);
    }

    @Override
    public final boolean isSignedIntegerType() {
        return true;
    }

    @Override
    public final boolean isUnsignedIntegerType() {
        return false;
    }

    @Override
    public final boolean isComplete() {
        return true;
    }

    /**
     * @return Newly created object that represents the corresponding unsigned
     *         integer type (with the same type qualifiers as this type).
     */
    public abstract UnsignedIntegerType getUnsignedIntegerType();
}
