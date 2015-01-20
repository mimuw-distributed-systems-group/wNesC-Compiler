package pl.edu.mimuw.nesc.type;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import java.math.BigInteger;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.declaration.object.ConstantDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.EnumDeclaration;

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
     * Variant of this type object.
     */
    private Variant variant;

    /**
     * Enum declaration object that is associated with this enumerated type.
     * It shall be inside the symbol table if and only if it is named.
     * Never null. There shall be only one <code>EnumDeclaration</code> object
     * per definition.
     */
    private final Optional<EnumDeclaration> enumType;

    /**
     * Expressions used to initialize consecutive enumerators of this enumerated
     * type.
     */
    private Optional<ImmutableList<Optional<Expression>>> values = Optional.absent();

    /**
     * Initializes this object with given parameters.
     *
     * @param enumType Object that reflects the definition of the enumeration
     *                 type (enumerated types cannot be forward-declared).
     * @throws NullPointerException <code>enumType</code> is null.
     */
    public EnumeratedType(boolean constQualified, boolean volatileQualified,
                          EnumDeclaration enumType) {
        super(constQualified, volatileQualified);
        checkNotNull(enumType, "enumeration declaration cannot be null");
        this.enumType = Optional.of(enumType);
        this.variant = Variant.ONLY_DECLARATION;
    }

    public EnumeratedType(EnumDeclaration enumType) {
        this(false, false, enumType);
    }

    public EnumeratedType(boolean constQualified, boolean volatileQualified,
            ImmutableList<Optional<Expression>> values) {
        super(constQualified, volatileQualified);
        checkNotNull(values, "values cannot be null");
        this.enumType = Optional.absent();
        this.variant = Variant.ONLY_VALUES;
    }

    public EnumeratedType(ImmutableList<Optional<Expression>> values) {
        this(false, false, values);
    }

    public final EnumDeclaration getEnumDeclaration() {
        checkState(variant == Variant.FULL || variant == Variant.ONLY_DECLARATION,
                "invalid variant of this enumeration type object");
        return enumType.get();
    }

    /**
     * Get list with expressions for constants of this field. If a constant has
     * not got any expression that specifies its value, the object is absent
     * for it. The expressions are in the list in proper order.
     *
     * @return List with expressions that specify values for subsequent
     *         constants.
     * @throws IllegalStateException This type is not yet fully complete.
     */
    public final ImmutableList<Optional<Expression>> getConstantsValues() {
        checkState(values.isPresent(), "cannot get values of constants from" +
                "an enumerated type that is not yet fully complete");
        return values.get();
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
        switch (variant) {
            case ONLY_DECLARATION:
                return new EnumeratedType(addConstQualifier(addConst), addVolatileQualifier(addVolatile),
                    getEnumDeclaration());
            case FULL:
            case ONLY_VALUES:
                return new EnumeratedType(addConstQualifier(addConst), addVolatileQualifier(addVolatile),
                        this.values.get());
            default:
                throw new RuntimeException("unexpected variant of enumerated type");
        }
    }

    @Override
    public final EnumeratedType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                                 boolean removeRestrict) {
        switch (variant) {
            case ONLY_DECLARATION:
                return new EnumeratedType(removeConstQualifier(removeConst), removeVolatileQualifier(removeVolatile),
                    getEnumDeclaration());
            case FULL:
            case ONLY_VALUES:
                return new EnumeratedType(removeConstQualifier(removeConst), removeVolatileQualifier(removeVolatile),
                        this.values.get());
            default:
                throw new RuntimeException("unexpected variant of enumerated type");
        }
    }

    @Override
    public final int getIntegerRank() {
        return INTEGER_RANK;
    }

    @Override
    public final BigInteger getMinimumValue() {
        throw new UnsupportedOperationException("getting the minimum value is unsupported for an enumerated type");
    }

    @Override
    public final BigInteger getMaximumValue() {
        throw new UnsupportedOperationException("getting the maximum value is unsupported for an enumerated type");
    }

    @Override
    public final Range<BigInteger> getRange() {
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
        return variant == Variant.ONLY_DECLARATION || variant == Variant.FULL
            || enumType.get().isDefined();
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    @Override
    public final void fullyComplete() {
        checkState(isComplete(), "cannot fully complete an incomplete type");

        if (!values.isPresent()) {
            final ImmutableList.Builder<Optional<Expression>> valuesBuilder = ImmutableList.builder();
            for (ConstantDeclaration constant : enumType.get().getEnumerators().get()) {
                valuesBuilder.add(constant.getEnumerator().getValue());
            }

            this.values = Optional.of(valuesBuilder.build());
            this.variant = Variant.FULL;
        }
    }

    /**
     * Enumeration type that specifies the variant of this enumerated type.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     * @see FieldTagType.Variant
     */
    public enum Variant {
        ONLY_DECLARATION,
        ONLY_VALUES,
        FULL,
    }
}
