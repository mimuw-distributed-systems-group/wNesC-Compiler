package pl.edu.mimuw.nesc.ast.type;

/**
 * Interface that represents a C or nesC type. Objects of all classes that
 * implement this interface shall be immutable.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface Type {
    /**
     * Arithmetic types are integer and floating types.
     *
     * @return <code>true</code> if and only if this type is an arithmetic type.
     *         If so, a cast to a proper derived class is possible without
     *         error.
     */
    boolean isArithmetic();

    /**
     * Integer types: <code>char</code>, signed integer types, unsigned integer
     * types and enumerated types.
     *
     * @return <code>true</code> if and only if this type is an integer type.
     *         If so, a cast to a proper derived class is possible without
     *         error.
     */
    boolean isIntegerType();

    /**
     * Signed integer types: <code>signed char</code>, <code>short int</code>,
     * <code>int</code>, <code>long int</code>, <code>long long int</code>.
     *
     * @return <code>true</code> if and only if this type is a signed integer
     *         type. If so, a cast to a proper derived class can be made without
     *         error.
     */
    boolean isSignedIntegerType();

    /**
     * Unsigned integer types: <code>unsigned char</code>,
     * <code>unsigned short int</code>, <code>unsigned int</code>,
     * <code>unsigned long int</code>, <code>unsigned long long int</code>.
     *
     * @return <code>true</code> if and only if this type is an unsigned integer
     *         type. If so, a cast to a proper derived class can be made without
     *         error.
     */
    boolean isUnsignedIntegerType();

    /**
     * Floating types are: <code>float</code>, <code>double</code>,
     * <code>long double</code> and complex types.
     *
     * @return <code>true</code> if and only if this type is a floating type.
     *         If so, a cast to a proper derived class is possible without
     *         error.
     */
    boolean isFloatingType();

    /**
     * Character types are: <code>char</code>, <code>signed char</code>
     * and <code>unsigned char</code>.
     *
     * @return <code>true</code> if and only if this type is a character type.
     */
    boolean isCharacterType();

    /**
     * Scalar types are arithmetic types and pointer types.
     *
     * @return <code>true</code> if and only if this type is a scalar type.
     */
    boolean isScalarType();

    /**
     * @return <code>true</code> if and only if this type is the
     *         <code>void</code> type. If so, it can be casted to
     *         <code>VoidType</code> class without error.
     */
    boolean isVoid();

    /**
     * Derived types are: array types, structure types, union types, function
     * types and pointer types.
     *
     * @return <code>true</code> if and only if this type is a derived type.
     *         If so, it can be casted to a proper class without error.
     */
    boolean isDerivedType();

    /**
     * Field tag types are: structures, unions, external structures and external
     * unions.
     *
     * @return <code>true</code> if and only if this type is a field tag type.
     *         If so, it can be casted to a proper class without error.
     */
    boolean isFieldTagType();

    /**
     * @return <code>true</code> if and only if this type is the type definition
     *         type. If so, it can be casted to a proper class without error.
     */
    boolean isTypeDefinition();

    /**
     * @return <code>true</code> if and only if this type is a pointer type. If
     *         so, it can be casted to a proper class without error.
     */
    boolean isPointerType();

    /**
     * @return Newly created object that represents the same type as this object
     *         but with given qualifiers added if necessary. If a type does not
     *         use a qualifier, the parameter related to it is ignored.
     * @throws UnsupportedOperationException Method invoked on an object that
     *                                       represents an artificial type.
     */
    Type addQualifiers(boolean constQualifier, boolean volatileQualifier,
                       boolean restrictQualifier);

    /**
     * @return <code>true</code> if and only if this type is const-qualified,
     *         e.g. <code>const int</code>.
     */
    boolean isConstQualified();

    /**
     * @return <code>true</code> if and only if this type is volatile-qualified,
     *         e.g. <code>volatile unsigned int</code>.
     */
    boolean isVolatileQualified();

    /**
     * Method that allows using the types class hierarchy in the Visitor design
     * pattern. It shall contain only a single statement:
     * <code>return visitor.visit(this, arg);</code>
     *
     * @return Value returned by the given visitor.
     */
    <R, A> R accept(TypeVisitor<R, A> visitor, A arg);
}
