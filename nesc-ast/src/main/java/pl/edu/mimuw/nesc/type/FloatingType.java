package pl.edu.mimuw.nesc.type;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.external.ExternalScheme;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class FloatingType extends ArithmeticType {
    protected FloatingType(boolean constQualified, boolean volatileQualified,
            Optional<ExternalScheme> externalScheme) {
        super(constQualified, volatileQualified, externalScheme);
    }

    @Override
    public final boolean isFloatingType() {
        return true;
    }

    @Override
    public final boolean isIntegerType() {
        return false;
    }

    @Override
    public final boolean isCharacterType() {
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
    public final FloatingType promote() {
        return this;
    }

    @Override
    public final boolean isComplete() {
        return true;
    }
}
