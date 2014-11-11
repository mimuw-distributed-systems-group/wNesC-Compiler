package pl.edu.mimuw.nesc.facade.component.reference;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.ast.type.FunctionType;
import pl.edu.mimuw.nesc.ast.type.InterfaceType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.declaration.object.ComponentRefDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ConstantDeclaration;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.declaration.object.InterfaceRefDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.object.TypenameDeclaration;
import pl.edu.mimuw.nesc.declaration.object.VariableDeclaration;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.facade.Substitution;
import pl.edu.mimuw.nesc.facade.iface.InterfaceEntity;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * <p>A facade for a correct component reference and the component itself.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class GoodComponentRefFacade extends AbstractComponentRefFacade {

    /**
     * Map with all interfaces or bare commands or events defined in the
     * specification of the referred components (after all types substitutions).
     */
    private final ImmutableMap<String, SpecificationEntity> entities;

    /**
     * Map with all enumeration constants declared in the specification of the
     * referred component.
     */
    private final ImmutableMap<String, ConstantDeclaration> constants;

    /**
     * Map with all type definitions from the specification of the referred
     * component after all type substitutions.
     */
    private final ImmutableMap<String, Optional<Type>> typeDefinitions;

    /**
     * Get the builder for a good component reference facade.
     *
     * @return Newly created builder that will build a good component reference
     *         facade.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Initialize this object.
     *
     * @param builder Builder with information necessary to build a good
     *                component reference facade.
     */
    private GoodComponentRefFacade(Builder builder) {
        super(builder.declaration);

        builder.buildAll();

        this.entities = builder.entities;
        this.constants = builder.constants;
        this.typeDefinitions = builder.typedefs;
    }

    @Override
    public boolean goodComponentRef() {
        return true;
    }

    @Override
    public boolean containsConstant(String name) {
        checkName(name);
        return constants.containsKey(name);
    }

    @Override
    public Optional<Optional<Type>> getTypedef(String name) {
        checkName(name);
        return Optional.fromNullable(typeDefinitions.get(name));
    }

    @Override
    public Optional<SpecificationEntity> get(String name) {
        checkName(name);
        return Optional.fromNullable(entities.get(name));
    }

    @Override
    public ImmutableSet<Map.Entry<String, SpecificationEntity>> getAll() {
        return entities.entrySet();
    }

    /**
     * Builder for a good component reference facade.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder {
        /**
         * Logger for the builder class.
         */
        private static final Logger LOG = Logger.getLogger(Builder.class);

        /**
         * Data necessary to build a good component reference facade.
         */
        private ComponentRefDeclaration declaration;
        private Environment specificationEnvironment;
        private Substitution substitution;

        /**
         * Results of building process.
         */
        private ImmutableMap<String, SpecificationEntity> entities;
        private ImmutableMap<String, ConstantDeclaration> constants;
        private ImmutableMap<String, Optional<Type>> typedefs;

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder() {
        }

        /**
         * Set the component reference declaration that represents the component
         * reference.
         *
         * @param declaration Declaration to set.
         * @return <code>this</code>
         */
        public Builder declaration(ComponentRefDeclaration declaration) {
            this.declaration = declaration;
            return this;
        }

        /**
         * Set the environment of the specification of the referred component.
         *
         * @param environment Environment to set.
         * @return <code>this</code>
         */
        public Builder specificationEnvironment(Environment environment) {
            this.specificationEnvironment = environment;
            return this;
        }

        /**
         * Set the substitution for the generic components of the referred
         * component.
         *
         * @param substitution Substitution to set.
         * @return <code>this</code>
         */
        public Builder substitution(Substitution substitution) {
            this.substitution = substitution;
            return this;
        }

        private void validate() {
            checkNotNull(declaration, "declaration cannot be null");
            checkNotNull(specificationEnvironment, "specification environment cannot be null");
            checkNotNull(substitution, "substitution cannot be null");
        }

        public GoodComponentRefFacade build() {
            validate();
            return new GoodComponentRefFacade(this);
        }

        private void buildAll() {
            final BuilderDeclarationVisitor builderVisitor = new BuilderDeclarationVisitor();

            for (Map.Entry<String, ObjectDeclaration> entry : specificationEnvironment.getObjects().getAll()) {
                entry.getValue().accept(builderVisitor, null);
            }

            this.entities = builderVisitor.builderEntities.build();
            this.constants = builderVisitor.builderConstants.build();
            this.typedefs = builderVisitor.builderTypedefs.build();

            if (LOG.isDebugEnabled()) {
                LOG.debug(format("Good facade for '%s' built: %d entities, %d typedefs, %d constants",
                        declaration.getName(), entities.size(), typedefs.size(), constants.size()));
            }
        }

        /**
         * Visitor that builds lists with information about the specification of
         * a component.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private final class BuilderDeclarationVisitor implements ObjectDeclaration.Visitor<Void, Void> {

            private final ImmutableMap.Builder<String, SpecificationEntity> builderEntities = ImmutableMap.builder();
            private final ImmutableMap.Builder<String, ConstantDeclaration> builderConstants = ImmutableMap.builder();
            private final ImmutableMap.Builder<String, Optional<Type>> builderTypedefs = ImmutableMap.builder();

            @Override
            public Void visit(ConstantDeclaration declaration, Void arg) {
                builderConstants.put(declaration.getName(), declaration);
                return null;
            }

            @Override
            public Void visit(TypenameDeclaration declaration, Void arg) {
                final Optional<Type> afterSubst = substitution.substituteType(declaration.getDenotedType());
                builderTypedefs.put(declaration.getName(), afterSubst);
                return null;
            }

            @Override
            public Void visit(InterfaceRefDeclaration declaration, Void arg) {
                final Optional<Type> ifaceType = substitution.substituteType(declaration.getType());

                checkState(ifaceType.isPresent(), "type of an interface reference declaration is absent");
                checkState(ifaceType.get() instanceof InterfaceType, "unexpected type of an interface reference '%s'",
                        ifaceType.get().getClass());

                final SpecificationEntity entity = new InterfaceRefEntity(declaration.isProvides(),
                        declaration.getName(), (InterfaceType) ifaceType.get(),
                        substitution.substituteTypes(declaration.getInstanceParameters()));

                builderEntities.put(declaration.getName(), entity);
                return null;
            }

            @Override
            public Void visit(FunctionDeclaration declaration, Void arg) {
                if (!declaration.isProvided().isPresent()) {
                    return null;
                }

                final InterfaceEntity.Kind ifaceEntityKind;

                switch (declaration.getFunctionType()) {
                    case COMMAND:
                        ifaceEntityKind = InterfaceEntity.Kind.COMMAND;
                        break;
                    case EVENT:
                        ifaceEntityKind = InterfaceEntity.Kind.EVENT;
                        break;
                    default:
                        return null;
                }

                final Optional<Type> type = substitution.substituteType(declaration.getType());
                checkState(!type.isPresent() || type.get() instanceof FunctionType,
                        "unexpected type of a function '%s'", type.get());
                final Optional<FunctionType> funType =
                        Optional.fromNullable((FunctionType) type.orNull());

                final SpecificationEntity entity = new BareEntity(declaration.isProvided().get(),
                        declaration.getName(), funType, ifaceEntityKind,
                        substitution.substituteTypes(declaration.getInstanceParameters()));

                builderEntities.put(declaration.getName(), entity);
                return null;
            }

            @Override
            public Void visit(ComponentRefDeclaration declaration, Void arg) {
                return null;
            }

            @Override
            public Void visit(VariableDeclaration declaration, Void arg) {
                return null;
            }
        }
    }
}
