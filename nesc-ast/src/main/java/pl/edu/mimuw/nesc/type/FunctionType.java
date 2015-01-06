package pl.edu.mimuw.nesc.type;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Reflects a type of a function, e.g. <code>int(char, unsigned int)</code>.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class FunctionType extends DerivedType {
    /**
     * Type of the value that is returned by a function of this function type.
     * Never null.
     */
    private final Type returnType;

    /**
     * Types of arguments that a function of this function type must be given.
     * Never null and all of the elements of the array also never null.
     */
    private final ImmutableList<Optional<Type>> argumentsTypes;

    /**
     * <code>true</code> if and only if a function of this type takes variable
     * arguments.
     */
    private final boolean variableArguments;

    /**
     * Initializes this function type.
     *
     * @throws NullPointerException One of the arguments is null.
     * @throws IllegalArgumentException One of the elements of the given list is
     *                                  null.
     */
    public FunctionType(Type returnType, List<Optional<Type>> argumentsTypes,
                        boolean variableArguments) {
        super(false, false);

        // Validate the arguments
        checkNotNull(returnType, "return type cannot be null");
        checkNotNull(argumentsTypes, "arguments types cannot be null");
        for (Optional<Type> type : argumentsTypes) {
            checkArgument(type != null, "a type of an argument of a function type cannot be null");
        }

        // Initialize this object
        this.returnType = returnType;
        this.argumentsTypes = ImmutableList.copyOf(argumentsTypes);
        this.variableArguments = variableArguments;
    }

    /**
     * A convenient constructor for initializing a function type object. It
     * features a usage of the Java array type which is being used less and
     * less. However, it is used only for passing values.
     *
     * @throws NullPointerException One of the arguments is null
     *                              (except <code>variableArguments</code>).
     * @throws IllegalArgumentException One of the types in the array is null.
     */
    public FunctionType(Type returnType, Type[] argumentsTypes, boolean variableArguments) {
        super(false, false);

        // Validate arguments
        checkNotNull(returnType, "return type cannot be null");
        checkNotNull(argumentsTypes, "arguments types cannot be null");
        for (Type type : argumentsTypes) {
            checkArgument(type != null, "a type of an argument of a function type cannot be null");
        }

        // Create the list of arguments
        final ImmutableList.Builder<Optional<Type>> argsTypesBuilder = ImmutableList.builder();
        for (Type type : argumentsTypes) {
            argsTypesBuilder.add(Optional.of(type));
        }

        // Initialize this object
        this.returnType = returnType;
        this.argumentsTypes = argsTypesBuilder.build();
        this.variableArguments = variableArguments;
    }

    /**
     * @return Object that represents the type of values that a function of this
     *         function type returns.
     */
    public final Type getReturnType() {
        return returnType;
    }

    /**
     * @return Immutable list with types of arguments that a function of this
     *         type takes.
     */
    public final ImmutableList<Optional<Type>> getArgumentsTypes() {
        return argumentsTypes;
    }

    /**
     * @return <code>true</code> if and only if a function of this type takes
     *         variable arguments.
     */
    public final boolean getVariableArguments() {
        return variableArguments;
    }

    @Override
    public final boolean isScalarType() {
        return false;
    }

    @Override
    public final boolean isFieldTagType() {
        return false;
    }

    @Override
    public final boolean isPointerType() {
        return false;
    }

    @Override
    public final boolean isArrayType() {
        return false;
    }

    @Override
    public final boolean isObjectType() {
        return false;
    }

    @Override
    public final boolean isFunctionType() {
        return true;
    }

    @Override
    public final boolean isModifiable() {
        return false;
    }

    @Override
    public final FunctionType addQualifiers(boolean addConst, boolean addVolatile,
                                            boolean addRestrict) {
        return this;
    }

    @Override
    public final FunctionType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                               boolean removeRestrict) {
        return this;
    }

    @Override
    public final PointerType decay() {
        return new PointerType(this);
    }

    @Override
    public boolean isCompatibleWith(Type type) {
        if (!super.isCompatibleWith(type)) {
            return false;
        }

        // Return types
        final FunctionType funType = (FunctionType) type;
        if (!getReturnType().isCompatibleWith(funType.getReturnType())) {
            return false;
        }

        // Parameters
        final List<Optional<Type>> params = getArgumentsTypes(),
                                   otherParams = funType.getArgumentsTypes();
        if (params.size() != otherParams.size()) {
            return false;
        }
        for (int i = 0; i < params.size(); ++i) {
            final Optional<Type> param = params.get(i),
                                 otherParam = otherParams.get(i);
            if (param.isPresent() && otherParam.isPresent()) {
                final Type adjustedParam = param.get().decay().removeQualifiers(),
                           adjustedOtherParam = otherParam.get().decay().removeQualifiers();

                if (!adjustedParam.isCompatibleWith(adjustedOtherParam)) {
                    return false;
                }
            }
        }

        return getVariableArguments() == funType.getVariableArguments();
    }

    @Override
    public final boolean isComplete() {
        /* The value returned here should not affect anything because the
           completeness of a type makes sense only for object types. */
        return true;
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
