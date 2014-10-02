package pl.edu.mimuw.nesc.ast.type;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Some useful methods that do some work on types. If follows the Utility design
 * pattern.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class TypeUtils {
    /**
     * Performs the usual arithmetic conversions as defined in the ISO
     * C standard. The conversions make sense only for arithmetic types.
     *
     * @param type1 The first type to take part in the conversion.
     * @param type2 The second type to take part in the conversion.
     * @return The common type for the given ones that is the result of the
     *         usual arithmetic conversions. The returned type has no type
     *         qualifiers regardless of qualifiers from the arguments.
     * @throws NullPointerException One of the arguments is null.
     */
    public static ArithmeticType doUsualArithmeticConversions(ArithmeticType type1,
            ArithmeticType type2) {
        // Validate arguments
        checkNotNull(type1, "the first type cannot be null");
        checkNotNull(type2, "the second type cannot be null");

        // Do the conversions
        if (type1 instanceof LongDoubleType || type2 instanceof LongDoubleType) {
            return new LongDoubleType();
        } else if (type1 instanceof DoubleType || type2 instanceof DoubleType) {
            return new DoubleType();
        } else if (type1 instanceof FloatType || type2 instanceof FloatType) {
            return new FloatType();
        } else {
            final IntegerType promoted1 = (IntegerType) type1.promote(),
                              promoted2 = (IntegerType) type2.promote();

            if (promoted1.isCompatibleWith(promoted2)) {
                return promoted1;
            } else if (promoted1.isSignedIntegerType() && promoted2.isSignedIntegerType()
                    || promoted1.isUnsignedIntegerType() && promoted2.isUnsignedIntegerType()) {
                return   promoted1.getIntegerRank() > promoted2.getIntegerRank()
                       ? promoted1
                       : promoted2;
            }

            final DifferentSignTypes types =
                      promoted1.isSignedIntegerType()
                    ? new DifferentSignTypes((SignedIntegerType) promoted1,
                                             (UnsignedIntegerType) promoted2)
                    : new DifferentSignTypes((SignedIntegerType) promoted2,
                                             (UnsignedIntegerType) promoted1);

            if (types.unsignedType.getIntegerRank() >= types.signedType.getIntegerRank()) {
                return types.unsignedType;
            } else if (types.signedType.getIntegerRank() > types.unsignedType.getIntegerRank()) {
                return types.signedType;
            } else {
                return types.signedType.getUnsignedIntegerType();
            }
        }
    }

    /**
     * Private constructor to prevent this class from being instantiated.
     */
    private TypeUtils() {
    }

    /**
     * Simple helper class that combines an signed integer type with an unsigned
     * integer type.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class DifferentSignTypes {
        private final SignedIntegerType signedType;
        private final UnsignedIntegerType unsignedType;

        private DifferentSignTypes(SignedIntegerType signedType,
                                   UnsignedIntegerType unsignedType) {
            this.signedType = signedType;
            this.unsignedType = unsignedType;
        }
    }
}
