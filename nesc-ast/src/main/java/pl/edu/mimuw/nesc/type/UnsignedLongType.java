package pl.edu.mimuw.nesc.type;

import com.google.common.base.Optional;
import com.google.common.collect.Range;
import java.math.BigInteger;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.external.ExternalScheme;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reflects the <code>unsigned long</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UnsignedLongType extends UnsignedIntegerType {
    public UnsignedLongType(boolean constQualified, boolean volatileQualified, Optional<ExternalScheme> externalScheme) {
        super(constQualified, volatileQualified, externalScheme);
    }

    public UnsignedLongType() {
        this(false, false, Optional.<ExternalScheme>absent());
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final UnsignedLongType addQualifiers(boolean addConst, boolean addVolatile,
                                                boolean addRestrict) {
        return new UnsignedLongType(
                addConstQualifier(addConst),
                addVolatileQualifier(addVolatile),
                getExternalScheme()
        );
    }

    @Override
    public final UnsignedLongType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                                   boolean removeRestrict) {
        return new UnsignedLongType(
                removeConstQualifier(removeConst),
                removeVolatileQualifier(removeVolatile),
                getExternalScheme()
        );
    }

    @Override
    public final UnsignedLongType addExternalScheme(ExternalScheme externalScheme) {
        checkNotNull(externalScheme, "external scheme cannot be null");
        return new UnsignedLongType(
                isConstQualified(),
                isVolatileQualified(),
                Optional.of(externalScheme)
        );
    }

    @Override
    public final int getIntegerRank() {
        return LongType.INTEGER_RANK;
    }

    @Override
    public final Range<BigInteger> getRange(ABI abi) {
        return abi.getLong().getUnsignedRange();
    }

    @Override
    public final LongType getSignedIntegerType() {
        return new LongType(isConstQualified(), isVolatileQualified(), Optional.<ExternalScheme>absent());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
