package pl.edu.mimuw.nesc.facade.iface;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.HashMap;
import java.util.Map;

import pl.edu.mimuw.nesc.type.*;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.declaration.object.InterfaceRefDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectKind;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.facade.Substitution;

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
     * Substitution for the generic parameters.
     */
    private final Substitution substitution;

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
        this.substitution = builder.substitution;
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

        final Optional<FunctionType> funType = substitution.substituteType(funDecl.getType())
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
        private Substitution substitution;

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
         * Set the substitution that will be used to substitute generic
         * parameters of the referred interface to actual types.
         *
         * @param substitution Substitution to set.
         * @return <code>this</code>
         */
        public Builder substitution(Substitution substitution) {
            this.substitution = substitution;
            return this;
        }

        private void validate() {
            checkNotNull(declaration, "declaration of the interface reference cannot be null");
            checkNotNull(bodyEnvironment, "body environment of the interface cannot be null");
            checkNotNull(substitution, "substitution cannot be null");
        }

        public GoodInterfaceRefFacade build() {
            validate();
            return new GoodInterfaceRefFacade(this);
        }
    }
}
