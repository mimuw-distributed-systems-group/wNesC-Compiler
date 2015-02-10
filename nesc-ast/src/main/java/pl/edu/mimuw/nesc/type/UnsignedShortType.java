package pl.edu.mimuw.nesc.type;

import com.google.common.base.Optional;
import com.google.common.collect.Range;
import java.math.BigInteger;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.external.ExternalScheme;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reflects the <code>unsigned short int</code> type.
 */
public final class UnsignedShortType extends UnsignedIntegerType {
    public UnsignedShortType(boolean constQualified, boolean volatileQualified, Optional<ExternalScheme> externalScheme) {
        super(constQualified, volatileQualified, externalScheme);
    }

    public UnsignedShortType() {
        this(false, false, Optional.<ExternalScheme>absent());
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final UnsignedShortType addQualifiers(boolean addConst, boolean addVolatile,
                                                 boolean addRestrict) {
        return new UnsignedShortType(
                addConstQualifier(addConst),
                addVolatileQualifier(addVolatile),
                getExternalScheme()
        );
    }

    @Override
    public final UnsignedShortType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                                    boolean removeRestrict) {
        return new UnsignedShortType(
                removeConstQualifier(removeConst),
                removeVolatileQualifier(removeVolatile),
                getExternalScheme()
        );
    }

    @Override
    public final UnsignedShortType addExternalScheme(ExternalScheme externalScheme) {
        checkNotNull(externalScheme, "external scheme cannot be null");
        return new UnsignedShortType(
                isConstQualified(),
                isVolatileQualified(),
                Optional.of(externalScheme)
        );
    }

    @Override
    public final int getIntegerRank() {
        return ShortType.INTEGER_RANK;
    }

    @Override
    public final Range<BigInteger> getRange(ABI abi) {
        return abi.getShort().getUnsignedRange();
    }

    @Override
    public final ShortType getSignedIntegerType() {
        return new ShortType(isConstQualified(), isVolatileQualified(), Optional.<ExternalScheme>absent());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
