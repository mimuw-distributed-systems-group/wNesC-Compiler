package pl.edu.mimuw.nesc.type;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.math.BigInteger;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.declaration.object.ConstantDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.EnumDeclaration;
import pl.edu.mimuw.nesc.external.ExternalScheme;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Reflects all enumerations, e.g. <code>enum E { E1, E2, }</code>.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class EnumeratedType extends IntegerType {
    public static final int INTEGER_RANK = 15;

    /**
     * Enum declaration object that is associated with this enumerated type.
     * It shall be inside the symbol table if and only if it is named.
     * Never null. There shall be only one <code>EnumDeclaration</code> object
     * per definition.
     */
    private final EnumDeclaration enumType;

    /**
     * Initializes this object with given parameters.
     *
     * @param enumType Object that reflects the definition of the enumeration
     *                 type (enumerated types cannot be forward-declared).
     * @throws NullPointerException <code>enumType</code> is null.
     */
    public EnumeratedType(boolean constQualified, boolean volatileQualified,
                          EnumDeclaration enumType) {
        super(constQualified, volatileQualified, Optional.<ExternalScheme>absent());
        checkNotNull(enumType, "enumeration declaration cannot be null");
        this.enumType = enumType;
    }

    public EnumeratedType(EnumDeclaration enumType) {
        this(false, false, enumType);
    }

    public final EnumDeclaration getEnumDeclaration() {
        return enumType;
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
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final EnumeratedType addQualifiers(boolean addConst, boolean addVolatile,
                                              boolean addRestrict) {
        return new EnumeratedType(addConstQualifier(addConst), addVolatileQualifier(addVolatile),
                    getEnumDeclaration());
    }

    @Override
    public final EnumeratedType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                                 boolean removeRestrict) {
        return new EnumeratedType(removeConstQualifier(removeConst), removeVolatileQualifier(removeVolatile),
                    getEnumDeclaration());
    }

    @Override
    public final int getIntegerRank() {
        return INTEGER_RANK;
    }

    @Override
    public final Range<BigInteger> getRange(ABI abi) {
        throw new UnsupportedOperationException("getting the range of values is unsupported for an enumerated type");
    }

    @Override
    public final boolean isCompatibleWith(Type type) {
        if (!super.isCompatibleWith(type)) {
            return false;
        }

        final EnumeratedType enumType = (EnumeratedType) type;
        return enumType.getEnumDeclaration() == getEnumDeclaration();
    }

    @Override
    public final boolean isComplete() {
        return enumType.isDefined();
    }

    @Override
    public final EnumeratedType addExternalScheme(ExternalScheme externalScheme) {
        throw new UnsupportedOperationException("cannot add an external scheme to an enumerated type");
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
