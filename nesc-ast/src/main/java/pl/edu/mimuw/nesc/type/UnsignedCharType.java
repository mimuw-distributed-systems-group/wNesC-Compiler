package pl.edu.mimuw.nesc.type;

import com.google.common.base.Optional;
import com.google.common.collect.Range;
import java.math.BigInteger;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.external.ExternalScheme;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UnsignedCharType extends UnsignedIntegerType {
    public UnsignedCharType(boolean constQualified, boolean volatileQualified, Optional<ExternalScheme> externalScheme) {
        super(constQualified, volatileQualified, externalScheme);
    }

    public UnsignedCharType() {
        this(false, false, Optional.<ExternalScheme>absent());
    }

    @Override
    public final boolean isCharacterType() {
        return true;
    }

    @Override
    public final UnsignedCharType addQualifiers(boolean addConst, boolean addVolatile,
                                                boolean addRestrict) {
        return new UnsignedCharType(
                addConstQualifier(addConst),
                addVolatileQualifier(addVolatile),
                getExternalScheme()
        );
    }

    @Override
    public final UnsignedCharType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                                   boolean removeRestrict) {
        return new UnsignedCharType(
                removeConstQualifier(removeConst),
                removeVolatileQualifier(removeVolatile),
                getExternalScheme()
        );
    }

    @Override
    public final UnsignedCharType addExternalScheme(ExternalScheme externalScheme) {
        checkNotNull(externalScheme, "external scheme cannot be null");
        return new UnsignedCharType(
                isConstQualified(),
                isVolatileQualified(),
                Optional.of(externalScheme)
        );
    }

    @Override
    public final int getIntegerRank() {
        return SignedCharType.INTEGER_RANK;
    }

    @Override
    public final Range<BigInteger> getRange(ABI abi) {
        return abi.getChar().getUnsignedRange();
    }

    @Override
    public final SignedCharType getSignedIntegerType() {
        return new SignedCharType(
                isConstQualified(),
                isVolatileQualified(),
                Optional.<ExternalScheme>absent()
        );
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
