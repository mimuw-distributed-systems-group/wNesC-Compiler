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
     * Real types are: integer and real floating types.
     *
     * @return <code>true</code> if and only if this type is a real type.
     */
    boolean isRealType();

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
     * @return <code>true</code> if and only if this type is an array type. If
     *         so, it can be casted to a proper class without error.
     */
    boolean isArrayType();

    /**
     * @return <code>true</code> if and only if this type is an object type.
     *         Artificial types are not considered object types.
     */
    boolean isObjectType();

    /**
     * @return <code>true</code> if and only if this type is an artificial type.
     *         If so, it can be casted to a proper class without error.
     * @see ArtificialType
     */
    boolean isArtificialType();

    /**
     * @return <code>true</code> if and only if this type is a function type.
     */
    boolean isFunctionType();

    /**
     * Check if an lvalue of this type could be entirely modified without
     * violating <code>const</code> qualifiers applied to it or its parts if it
     * is a derived type.
     *
     * @return <p><code>true</code> if and only if all of the following
     *         conditions are fulfilled:</p>
     *         <ul>
     *             <li>this type is an object type</li>
     *             <li>this type is complete</li>
     *             <li>an lvalue of this type could be entirely modified without
     *             violating any <code>const</code> qualifiers</li>
     *         </ul>
     */
    boolean isModifiable();

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
     * Check if this type has all type qualifiers of the given type. If this
     * type is an artificial type, <code>false</code> is returned regardless of
     * type from the argument.
     *
     * @param otherType Other type with qualifiers that this type should have.
     * @return <code>true</code> if and only if this type has all type
     *         qualifiers that the type from the argument has.
     * @throws NullPointerException Given argument is null.
     */
    boolean hasAllQualifiers(Type otherType);

    /**
     * Checks if a type is complete as defined in the ISO C standard.
     *
     * @return <code>true</code> if and only if this type is not an artificial
     *         type and is complete at the current moment of the processing.
     */
    boolean isComplete();

    /**
     * @return Newly created object that represents the same type as this object
     *         but with given qualifiers added if necessary. If a type does not
     *         use a qualifier, the parameter related to it is ignored.
     *         If a parameter is <code>false</code> the presence of the
     *         corresponding qualifier is not affected.
     * @throws UnsupportedOperationException Method invoked on an object that
     *                                       represents an artificial type.
     */
    Type addQualifiers(boolean addConstQualifier, boolean addVolatileQualifier,
                       boolean addRestrictQualifier);

    /**
     * Create a new instance of the same type with qualifiers added from the
     * other type.
     *
     * @param otherType Another type.
     * @return Newly created object that represents the same type as this but
     *         with new qualifiers added if necessary. All qualifiers that the
     *         given type has are added and those that are present in this type
     *         are preserved. If a type does not use a qualifier, it is not
     *         added.
     * @throws UnsupportedOperationException Method invoked on an object that
     *                                       represents an artificial type.
     * @throws NullPointerException Given argument is null.
     */
    Type addQualifiers(Type otherType);

    /**
     * @return Newly created object that represents the same type as this object
     *         but with given qualifiers removed if necessary. If a type does
     *         not use a qualifier, the parameter related to it is ignored. If
     *         a parameter is <code>false</code> the presence of the
     *         corresponding qualifier is not affected.
     * @throws UnsupportedOperationException Method invoked on an object that
     *                                       represents an artificial type.
     */
    Type removeQualifiers(boolean removeConstQualifier, boolean removeVolatileQualifier,
                          boolean removeRestrictQualifier);

    /**
     * @return Newly created object that represents the same type as this object
     *         but with all qualifiers removed. Equivalent to
     *         <code>removeQualifiers(true, true, true);</code>.
     */
    Type removeQualifiers();

    /**
     * Performs integer promotions. If this type is other than
     * <code>char</code>, <code>signed char</code>, <code>unsigned char</code>,
     * <code>short</code>, <code>unsigned short</code> and an enumerated type,
     * <code>this</code> will be returned.
     *
     * @return An object representing this type after performing the integer
     *         promotion on it.
     * @throws UnsupportedOperationException Method invoked on an object that
     *                                       represents an artificial type.
     */
    Type promote();

    /**
     * Decays this type. If this type is not a function type or an array type
     * and not an artificial type, <code>this</code> is returned. If this type
     * is a function type, a pointer type that refers to this type is returned.
     * If this type is an array type, a pointer type with the referenced type
     * being the element type of the array type is returned.
     *
     * @return An object representing this type after decaying it.
     * @throws UnsupportedOperationException Method invoked on an artificial
     *                                       type.
     */
    Type decay();

    /**
     * @return <code>true</code> if and only if this type is compatible with the
     *         given type as defined in the ISO C standard.
     * @throws UnsupportedOperationException Method invoked on an object that
     *                                       represents an artificial type.
     * @throws NullPointerException Given argument is null.
     */
    boolean isCompatibleWith(Type type);

    /**
     * Method that allows using the types class hierarchy in the Visitor design
     * pattern. It shall contain only a single statement:
     * <code>return visitor.visit(this, arg);</code>
     *
     * @return Value returned by the given visitor.
     */
    <R, A> R accept(TypeVisitor<R, A> visitor, A arg);
}
