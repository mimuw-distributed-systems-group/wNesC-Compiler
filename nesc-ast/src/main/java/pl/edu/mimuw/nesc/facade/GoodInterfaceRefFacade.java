package pl.edu.mimuw.nesc.facade;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.edu.mimuw.nesc.ast.type.*;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.declaration.object.InterfaceRefDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectKind;
import pl.edu.mimuw.nesc.environment.Environment;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;

/**
 * <p>Facade for an interface reference that is correct and refers to
 * a correctly defined interface.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class GoodInterfaceRefFacade extends AbstractInterfaceRefFacade {
    /**
     * Environment from the body of the referred interface definition.
     */
    private final Environment bodyEnvironment;

    /**
     * Map with types that are to be substituted for generic parameters of
     * referred interfaces.
     */
    private final ImmutableMap<String, Optional<Type>> substitution;

    /**
     * Map with already produced types of commands and events. It is used not to
     * substitute types multiple times for the same command or event.
     */
    private final Map<String, FunctionType> typesCache = new HashMap<>();

    /**
     * Get the builder for a good interface reference facade.
     *
     * @return Newly created builder that will create a good interface reference
     *         facade.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Initializes this object with information from builder.
     *
     * @param builder Builder with necessary information.
     */
    private GoodInterfaceRefFacade(Builder builder) {
        super(builder.declaration);

        this.bodyEnvironment = builder.bodyEnvironment;
        this.substitution = builder.buildSubstitution();
    }

    @Override
    public boolean goodInterfaceRef() {
        return true;
    }

    @Override
    public boolean contains(String name) {
        checkName(name);
        return lookForEntry(name).isPresent();
    }

    @Override
    public Optional<InterfaceEntityKind> getKind(String name) {
        checkName(name);

        final Optional<FunctionDeclaration> funDecl = lookForEntry(name);

        if (!funDecl.isPresent()) {
            return Optional.absent();
        }

        switch (funDecl.get().getFunctionType()) {
            case COMMAND:
                return Optional.of(InterfaceEntityKind.COMMAND);
            case EVENT:
                return Optional.of(InterfaceEntityKind.EVENT);
            default:
                throw new RuntimeException(format("got an interface entry that is not a command or event: %s",
                        funDecl.get().getFunctionType()));
        }
    }

    @Override
    public Optional<Type> getReturnType(String name) {
        checkName(name);
        final Optional<FunctionType> funType = getEntryType(name);
        return funType.isPresent()
                ? Optional.of(funType.get().getReturnType())
                : Optional.<Type>absent();
    }

    @Override
    public Optional<ImmutableList<Optional<Type>>> getArgumentsTypes(String name) {
        checkName(name);
        final Optional<FunctionType> funType = getEntryType(name);
        return funType.isPresent()
                ? Optional.of(funType.get().getArgumentsTypes())
                : Optional.<ImmutableList<Optional<Type>>>absent();
    }

    /**
     * Get the function declaration for a command or event with given name
     * declared in the referred interface. It does not contain a command or
     * event with given name, then the object is absent.
     *
     * @param name Name of a command or event to look for.
     * @return Object that represents the command or event with given name or
     *         absent value if the interface does not contain it.
     */
    private Optional<FunctionDeclaration> lookForEntry(String name) {
        final Optional<? extends ObjectDeclaration> optObjectDecl =
                bodyEnvironment.getObjects().get(name, true);

        if (!optObjectDecl.isPresent()) {
            return Optional.absent();
        }

        final ObjectDeclaration objectDecl = optObjectDecl.get();

        if (objectDecl.getKind() != ObjectKind.FUNCTION) {
            return Optional.absent();
        }

        final FunctionDeclaration funDecl = (FunctionDeclaration) objectDecl;
        final FunctionDeclaration.FunctionType funType = funDecl.getFunctionType();

        return funType == FunctionDeclaration.FunctionType.COMMAND
                || funType == FunctionDeclaration.FunctionType.EVENT
                ? Optional.of(funDecl)
                : Optional.<FunctionDeclaration>absent();
    }

    /**
     * Get the type of a command or event after performing the necessary
     * substitution.
     *
     * @param name Name of a command or an event.
     * @return Type of the command or event with given name after substitution.
     */
    private Optional<FunctionType> getEntryType(String name) {
        // Make use of the cache
        final Optional<FunctionType> cachedType = Optional.fromNullable(typesCache.get(name));
        if (cachedType.isPresent()) {
            return cachedType;
        }

        // If the type is not cached, try to construct the type

        final Optional<FunctionDeclaration> optFunDecl = lookForEntry(name);
        if (!optFunDecl.isPresent()) {
            return Optional.absent();
        }
        final FunctionDeclaration funDecl = optFunDecl.get();

        if (!funDecl.getType().isPresent()) {
            return Optional.absent();
        }
        checkState(funDecl.getType().get().isFunctionType(), "'%s' type used as a type for a function, a function type is expected",
                funDecl.getType().get());
        final FunctionType funType = (FunctionType) funDecl.getType().get();

        // Perform the substitution

        final Optional<Type> newReturnType = substituteType(funType.getReturnType());
        final List<Optional<Type>> newArgumentsTypes = new LinkedList<>();
        final Optional<FunctionType> result;

        for (Optional<Type> argType : funType.getArgumentsTypes()) {
            newArgumentsTypes.add(substituteType(argType));
        }

        result = Optional.fromNullable(
                  newReturnType.isPresent()
                ? new FunctionType(newReturnType.get(), newArgumentsTypes, funType.getVariableArguments())
                : null
        );

        if (result.isPresent()) {
            typesCache.put(name, result.get());
        }

        return result;
    }

    private Optional<Type> substituteType(Optional<Type> type) {
        return type.isPresent()
                ? substituteType(type.get())
                : type;
    }

    private Optional<Type> substituteType(Type type) {
        final TypeSubstituteVisitor substituteVisitor = new TypeSubstituteVisitor();
        return type.accept(substituteVisitor, null);
    }

    /**
     * Visitor that is responsible for substituting unknown types that occur
     * in a given type. It performs an action that is similar to cloning and
     * then applying some small changes. The substitution is defined by
     * {@link GoodInterfaceRefFacade#substitution substitution}.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class TypeSubstituteVisitor implements TypeVisitor<Optional<Type>, Void> {

        // Primitive types

        @Override
        public Optional<Type> visit(CharType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(SignedCharType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(UnsignedCharType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(ShortType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(UnsignedShortType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(IntType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(UnsignedIntType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(LongType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(UnsignedLongType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(LongLongType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(UnsignedLongLongType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(EnumeratedType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(FloatType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(DoubleType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(LongDoubleType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(VoidType type, Void arg) {
            return Optional.<Type>of(type);
        }

        // Derived types

        @Override
        public Optional<Type> visit(final ArrayType oldType, Void arg) {
            final Function<Type, Type> arrayTypeTransform = new Function<Type, Type>() {
                @Override
                public Type apply(Type newElementType) {
                    return newElementType != oldType.getElementType()
                        ? new ArrayType(newElementType, oldType.ofKnownSize)
                        : oldType;
                }
            };

            return oldType.getElementType().accept(this, null)
                        .transform(arrayTypeTransform);
        }

        @Override
        public Optional<Type> visit(final PointerType oldType, Void arg) {
            final Function<Type, Type> pointerTypeTransform = new Function<Type, Type>() {
                @Override
                public Type apply(Type newReferencedType) {
                    return newReferencedType != oldType.getReferencedType()
                        ? new PointerType(oldType.isConstQualified(), oldType.isVolatileQualified(),
                                          oldType.isRestrictQualified(), newReferencedType)
                        : oldType;
                }
            };

            return oldType.getReferencedType().accept(this, null)
                        .transform(pointerTypeTransform);
        }

        @Override
        public Optional<Type> visit(StructureType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(UnionType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(ExternalStructureType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(ExternalUnionType type, Void arg) {
            return Optional.<Type>of(type);
        }

        @Override
        public Optional<Type> visit(FunctionType type, Void arg) {
            boolean change;

            // Process return type
            final Optional<Type> returnType = type.getReturnType().accept(this, null);
            if (!returnType.isPresent()) {
                return returnType;
            }
            change = returnType.get() != type.getReturnType();

            // Process arguments types
            final List<Optional<Type>> argsTypes = new ArrayList<>(type.getArgumentsTypes().size());
            for (Optional<Type> argType : type.getArgumentsTypes()) {
                if (argType.isPresent()) {
                    final Optional<Type> newArgType = argType.get().accept(this, null);
                    argsTypes.add(newArgType);
                    change = change || !newArgType.isPresent() || newArgType.get() != argType.get();
                } else {
                    argsTypes.add(argType);
                }
            }

            final Type result = change
                    ? new FunctionType(returnType.get(), argsTypes, type.getVariableArguments())
                    : type;

            return Optional.of(result);
        }

        // Artificial types

        @Override
        public Optional<Type> visit(ComponentType type, Void arg) {
            throw new RuntimeException("unexpected artificial type");
        }

        @Override
        public Optional<Type> visit(InterfaceType type, Void arg) {
            throw new RuntimeException("unexpected artificial type");
        }

        @Override
        public Optional<Type> visit(TypeDefinitionType type, Void arg) {
            throw new RuntimeException("unexpected artificial type");
        }

        // Unknown types

        @Override
        public Optional<Type> visit(UnknownType type, Void arg) {
            return doSubstitution(type);
        }

        @Override
        public Optional<Type> visit(UnknownArithmeticType type, Void arg) {
            return doSubstitution(type);
        }

        @Override
        public Optional<Type> visit(UnknownIntegerType type, Void arg) {
            return doSubstitution(type);
        }

        private Optional<Type> doSubstitution(UnknownType type) {
            return Optional.fromNullable(substitution.get(type.getName()))
                        .or(Optional.<Type>absent());
        }
    }

    /**
     * Builder for the good interface facade.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder {
        /**
         * Data necessary to build the facade.
         */
        private InterfaceRefDeclaration declaration;
        private Environment bodyEnvironment;
        private final List<String> parametersNames = new ArrayList<>();
        private final List<Optional<Type>> substitutedTypes = new ArrayList<>();

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder() {
        }

        /**
         * Set the declaration object that represents the interface reference.
         *
         * @param declaration Declaration to set.
         * @return <code>this</code>
         */
        public Builder ifaceRefDeclaration(InterfaceRefDeclaration declaration) {
            this.declaration = declaration;
            return this;
        }

        /**
         * Set the body environment of the referred interface.
         *
         * @param environment Body environment of the referred interface.
         * @return <code>this</code>
         */
        public Builder bodyEnvironment(Environment environment) {
            this.bodyEnvironment = environment;
            return this;
        }

        /**
         * <p>Add the next name of a generic parameter in the interface
         * definition:</p>
         *
         * <pre>
         *     interface Read&lt;val_t&gt; { &hellip; }
         *                      ▲
         *                      |
         *                      |
         *                      |
         *
         * </pre>
         *
         * @param nextName Name of the generic paramter.
         * @return <code>this</code>
         */
        public Builder addParameterName(String nextName) {
            this.parametersNames.add(nextName);
            return this;
        }

        /**
         * <p>Add the next type from the instantiation of the referred
         * interface:</p>
         *
         * <pre>
         *      provides interface Read&lt;uint32_t&gt;;
         *                                  ▲
         *                                  |
         *                                  |
         *                                  |
         * </pre>
         *
         * @param nextType Type to be added.
         * @return <code>this</code>
         */
        public Builder addInstantiationType(Optional<Type> nextType) {
            this.substitutedTypes.add(nextType);
            return this;
        }

        private void validate() {
            checkNotNull(declaration, "declaration of the interface reference cannot be null");
            checkNotNull(bodyEnvironment, "body environment of the interface cannot be null");
            checkState(parametersNames.size() == substitutedTypes.size(),
                    "count of the parameters names is different from the count of instantiation types (%d:%d)",
                    parametersNames.size(), substitutedTypes.size());

            final Set<String> usedNames = new HashSet<>();

            /* Check if parameters names are unique (relevant during
               construction of the substitution) */
            for (String paramName : parametersNames) {
                checkState(!usedNames.contains(paramName), "'%s' generic parameter name added multiple times",
                        paramName);
                usedNames.add(paramName);
            }
        }

        public GoodInterfaceRefFacade build() {
            validate();
            return new GoodInterfaceRefFacade(this);
        }

        private ImmutableMap<String, Optional<Type>> buildSubstitution() {
            final ImmutableMap.Builder<String, Optional<Type>> substBuilder = ImmutableMap.builder();

            for (int i = 0; i < parametersNames.size(); ++i) {
                substBuilder.put(parametersNames.get(i), substitutedTypes.get(i));
            }

            return substBuilder.build();
        }
    }
}
