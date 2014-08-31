package pl.edu.mimuw.nesc.analysis.type;

import pl.edu.mimuw.nesc.declaration.tag.EnumDeclaration;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reflects all enumerations, e.g. <code>enum E { E1, E2, }</code>.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class EnumeratedType extends IntegerType {
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
        super(constQualified, volatileQualified);
        checkNotNull(enumType, "enumeration declaration cannot be null");
        this.enumType = enumType;
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
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
