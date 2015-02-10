package pl.edu.mimuw.nesc.type;

import com.google.common.base.Optional;
import com.google.common.collect.Range;
import java.math.BigInteger;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.external.ExternalScheme;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reflects the <code>int</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class IntType extends SignedIntegerType {
    public static final int INTEGER_RANK = 15;

    public IntType(boolean constQualified, boolean volatileQualified, Optional<ExternalScheme> externalScheme) {
        super(constQualified, volatileQualified, externalScheme);
    }

    public IntType() {
        this(false, false, Optional.<ExternalScheme>absent());
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final IntType addQualifiers(boolean addConst, boolean addVolatile,
                                       boolean addRestrict) {
        return new IntType(addConstQualifier(addConst), addVolatileQualifier(addVolatile),
                getExternalScheme());
    }

    @Override
    public final IntType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                          boolean removeRestrict) {
        return new IntType(
                removeConstQualifier(removeConst),
                removeVolatileQualifier(removeVolatile),
                getExternalScheme()
        );
    }

    @Override
    public final IntType addExternalScheme(ExternalScheme externalScheme) {
        checkNotNull(externalScheme, "external scheme cannot be null");
        return new IntType(
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
        return abi.getInt().getSignedRange();
    }

    @Override
    public final UnsignedIntType getUnsignedIntegerType() {
        return new UnsignedIntType(isConstQualified(), isVolatileQualified(), Optional.<ExternalScheme>absent());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
