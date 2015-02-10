package pl.edu.mimuw.nesc.type;

import com.google.common.base.Optional;
import com.google.common.collect.Range;
import java.math.BigInteger;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.external.ExternalScheme;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reflects the <code>long int</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class LongType extends SignedIntegerType {
    public static final int INTEGER_RANK = 20;

    public LongType(boolean constQualified, boolean volatileQualified, Optional<ExternalScheme> externalScheme) {
        super(constQualified, volatileQualified, externalScheme);
    }

    public LongType() {
        this(false, false, Optional.<ExternalScheme>absent());
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final LongType addQualifiers(boolean addConst, boolean addVolatile,
                                        boolean addRestrict) {
        return new LongType(
                addConstQualifier(addConst),
                addVolatileQualifier(addVolatile),
                getExternalScheme()
        );
    }

    @Override
    public final LongType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                           boolean removeRestrict) {
        return new LongType(
                removeConstQualifier(removeConst),
                removeVolatileQualifier(removeVolatile),
                getExternalScheme()
        );
    }

    @Override
    public final LongType addExternalScheme(ExternalScheme externalScheme) {
        checkNotNull(externalScheme, "external scheme cannot be null");
        return new LongType(
                isConstQualified(),
                isVolatileQualified(),
                Optional.of(externalScheme)
        );
    }

    @Override
    public final int getIntegerRank() {
        return INTEGER_RANK;
    }

    @Override
    public final Range<BigInteger> getRange(ABI abi) {
        return abi.getLong().getSignedRange();
    }

    @Override
    public final UnsignedLongType getUnsignedIntegerType() {
        return new UnsignedLongType(isConstQualified(), isVolatileQualified(), Optional.<ExternalScheme>absent());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
