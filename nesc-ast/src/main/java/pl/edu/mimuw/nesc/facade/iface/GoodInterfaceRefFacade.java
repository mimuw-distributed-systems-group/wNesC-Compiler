package pl.edu.mimuw.nesc.facade.iface;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
     * Function that checks if the given instance represents a function type and
     * throws an exception if not. If so, it returns the same object.
     */
    private static final Function<Type, FunctionType> TO_FUNCTION_TYPE = new Function<Type, FunctionType>() {
        @Override
        public FunctionType apply(Type type) {
            checkArgument(type instanceof FunctionType, "type of a command or event after substitution is not a function type");
            return (FunctionType) type;
        }
    };

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
     * Map with objects that are results of lookup operations of commands and
     * events. It is used to avoid repeating the same operations.
     */
    private final Map<String, Optional<InterfaceEntity>> entitiesCache = new HashMap<>();

    /**
     * Map that represents all commands and events from the referred interface
     * with all necessary type substitutions. It is created lazily.
     */
    private Optional<ImmutableMap<String, InterfaceEntity>> allEntities = Optional.absent();

    /**
     * Function that transforms given function declaration to an interface
     * entity object with the usage of
     * {@link GoodInterfaceRefFacade#newInterfaceEntity} method.
     */
    private final Function<FunctionDeclaration, InterfaceEntity> TO_INTERFACE_ENTITY = new Function<FunctionDeclaration, InterfaceEntity>() {
        @Override
        public InterfaceEntity apply(FunctionDeclaration funDecl) {
            return newInterfaceEntity(funDecl);
        }
    };

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
    public Optional<InterfaceEntity> get(String name) {
        checkName(name);

        // Try to use the immutable map with all commands and events

        if (allEntities.isPresent()) {
            return Optional.fromNullable(allEntities.get().get(name));
        }

        // Try to use the entities cache

        final Optional<Optional<InterfaceEntity>> optResult =
                Optional.fromNullable(entitiesCache.get(name));

        if (optResult.isPresent()) {
            return optResult.get();
        }

        /* Check if the entity is contained in the interface and if so construct
           a new interface entity object. */

        final Optional<InterfaceEntity> result = lookForEntry(name)
                .transform(TO_INTERFACE_ENTITY);

        // Update the cache and return result

        entitiesCache.put(name, result);
        return result;
    }

    /**
     * Create a new interface entity object with information from the given
     * function declaration.
     *
     * @param funDecl Declaration object with necessary information.
     * @return Newly created interface entity object.
     */
    private InterfaceEntity newInterfaceEntity(FunctionDeclaration funDecl) {
        final InterfaceEntity.Kind kind;

        switch (funDecl.getFunctionType()) {
            case COMMAND:
                kind = InterfaceEntity.Kind.COMMAND;
                break;
            case EVENT:
                kind = InterfaceEntity.Kind.EVENT;
                break;
            default:
                throw new RuntimeException(format("got function that is not a command or event: %s",
                        funDecl.getFunctionType()));
        }

        final Optional<FunctionType> funType = substituteType(funDecl.getType())
                .transform(TO_FUNCTION_TYPE);

        return new InterfaceEntity(kind, funType, funDecl.getName());
    }

    @Override
    public ImmutableSet<Map.Entry<String, InterfaceEntity>> getAll() {
        if (allEntities.isPresent()) {
            return allEntities.get().entrySet();
        }

        // Create the map with all commands and events

        final ImmutableMap.Builder<String, InterfaceEntity> builder = ImmutableMap.builder();

        for (Map.Entry<String, ObjectDeclaration> entry : bodyEnvironment.getObjects().getAll()) {
            if (entry.getValue().getKind() != ObjectKind.FUNCTION) {
                continue;
            }

            final FunctionDeclaration funDecl = (FunctionDeclaration) entry.getValue();

            if (funDecl.getFunctionType() != FunctionDeclaration.FunctionType.COMMAND
                    && funDecl.getFunctionType() != FunctionDeclaration.FunctionType.EVENT) {
                continue;
            }

            final Optional<Optional<InterfaceEntity>> optObject =
                    Optional.fromNullable(entitiesCache.get(entry.getKey()));
            final InterfaceEntity currentEntity;

            if (optObject.isPresent()) {
                checkState(optObject.get().isPresent(), "inconsistent state with regards to the existence of interface entity '%s'",
                           entry.getKey());
                currentEntity = optObject.get().get();
            } else {
                currentEntity = newInterfaceEntity(funDecl);
            }

            builder.put(entry.getKey(), currentEntity);
        }

        // Finish

        allEntities = Optional.of(builder.build());
        return allEntities.get().entrySet();
    }

    /**
     * Get the function declaration for a command or event with given name
     * declared in the referred interface. If it does not contain a command or
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

        private Optional<Type> doSubstitution(final UnknownType type) {
            final Function<Type, Type> enrichTypeTransform = new Function<Type, Type>() {
                @Override
                public Type apply(Type newType) {
                    return newType.addQualifiers(type);
                }
            };

            return Optional.fromNullable(substitution.get(type.getName()))
                        .or(Optional.<Type>absent())
                        .transform(enrichTypeTransform);
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
