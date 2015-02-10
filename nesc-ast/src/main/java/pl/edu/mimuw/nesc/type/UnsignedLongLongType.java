package pl.edu.mimuw.nesc.type;

import com.google.common.base.Optional;
import com.google.common.collect.Range;
import java.math.BigInteger;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.external.ExternalScheme;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reflects the <code>unsigned long long</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UnsignedLongLongType extends UnsignedIntegerType {
    public UnsignedLongLongType(boolean constQualified, boolean volatileQualified, Optional<ExternalScheme> externalScheme) {
        super(constQualified, volatileQualified, externalScheme);
    }

    public UnsignedLongLongType() {
        this(false, false, Optional.<ExternalScheme>absent());
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final UnsignedLongLongType addQualifiers(boolean addConst, boolean addVolatile,
                                                    boolean addRestrict) {
        return new UnsignedLongLongType(
                addConstQualifier(addConst),
                addVolatileQualifier(addVolatile),
                getExternalScheme()
        );
    }

    @Override
    public final UnsignedLongLongType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                                       boolean removeRestrict) {
        return new UnsignedLongLongType(
                removeConstQualifier(removeConst),
                removeVolatileQualifier(removeVolatile),
                getExternalScheme()
        );
    }

    @Override
    public final UnsignedLongLongType addExternalScheme(ExternalScheme externalScheme) {
        checkNotNull(externalScheme, "external scheme cannot be null");
        return new UnsignedLongLongType(
                isConstQualified(),
                isVolatileQualified(),
                Optional.of(externalScheme)
        );
    }

    @Override
    public final int getIntegerRank() {
        return LongLongType.INTEGER_RANK;
    }

    @Override
    public final Range<BigInteger> getRange(ABI abi) {
        return abi.getLongLong().getUnsignedRange();
    }

    @Override
    public final LongLongType getSignedIntegerType() {
        return new LongLongType(isConstQualified(), isVolatileQualified(), Optional.<ExternalScheme>absent());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
