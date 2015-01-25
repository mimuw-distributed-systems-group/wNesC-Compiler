package pl.edu.mimuw.nesc.type;

import com.google.common.base.Optional;
import com.google.common.collect.Range;
import java.math.BigInteger;
import pl.edu.mimuw.nesc.external.ExternalScheme;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class IntegerType extends ArithmeticType {
    /**
     * The external scheme associated with this type.
     */
    private Optional<ExternalScheme> externalScheme;

    protected IntegerType(boolean constQualified, boolean volatileQualified,
            Optional<ExternalScheme> externalScheme) {
        super(constQualified, volatileQualified);
        checkNotNull(externalScheme, "external scheme cannot be null");
        this.externalScheme = externalScheme;
    }

    @Override
    public final boolean isIntegerType() {
        return true;
    }

    @Override
    public final boolean isFloatingType() {
        return false;
    }

    @Override
    public final boolean isRealType() {
        return true;
    }

    /**
     * @return The integer rank of this type as defined in the ISO C standard.
     */
    public abstract int getIntegerRank();

    /**
     * Get the potential minimum value of this integer type.
     *
     * @return A value that could be the minimum value of this integer type.
     * @throws UnsupportedOperationException This method is invoked on type
     *                              <code>char</code> or an enumerated type.
     */
    public abstract BigInteger getMinimumValue();

    /**
     * Get the potential maximum value of this integer type.
     *
     * @return A value that could be the maximum value of this integer type.
     * @throws UnsupportedOperationException This method is invoked on type
     *                              <code>char</code> or an enumerated type.
     */
    public abstract BigInteger getMaximumValue();

    /**
     * Get the range of potential values of this integer type.
     *
     * @return Range that defines values this integer type could have.
     * @throws UnsupportedOperationException This method is invoked on type
     *                              <code>char</code> or an enumerated type.
     */
    public abstract Range<BigInteger> getRange();

    @Override
    public final IntegerType promote() {
        final boolean properIntegerType = !(this instanceof IntType)
                && !(this instanceof UnsignedIntType);

        return   getIntegerRank() <= IntType.INTEGER_RANK && properIntegerType
               ? new IntType(isConstQualified(), isVolatileQualified(), Optional.<ExternalScheme>absent())
               : this;
    }

    /**
     * Get the external scheme associated with this type. It is present if and
     * only if this type is an external base type.
     *
     * @return The external scheme associated with this type.
     */
    public final Optional<ExternalScheme> getExternalScheme() {
        return this.externalScheme;
    }

    /**
     * Create a new instance of the same type that differs only in the external
     * scheme. The given one is associated with the returned type.
     *
     * @param externalScheme External scheme to associate with the returned
     *                       type.
     * @return Newly created instance of the same type as this that differs only
     *         in that it is associated with the given external type.
     * @throws NullPointerException The given argument is <code>null</code>.
     * @throws UnsupportedOperationException The method is invoked on an
     *                                       enumerated type.
     */
    public abstract IntegerType addExternalScheme(ExternalScheme externalScheme);

    @Override
    public final boolean isExternal() {
        return this.externalScheme.isPresent();
    }

    @Override
    public final boolean isExternalBaseType() {
        return this.externalScheme.isPresent();
    }
}
