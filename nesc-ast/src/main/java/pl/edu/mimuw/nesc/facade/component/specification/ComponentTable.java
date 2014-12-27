package pl.edu.mimuw.nesc.facade.component.specification;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRef;
import pl.edu.mimuw.nesc.ast.gen.RpInterface;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.declaration.object.InterfaceRefDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.object.ObjectKind;
import pl.edu.mimuw.nesc.facade.iface.InterfaceEntity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Class that represents an abstract component table.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
abstract class ComponentTable<E> {
    /**
     * Logger for this class.
     */
    private static final Logger LOG = Logger.getLogger(ComponentTable.class);

    /**
     * Map with elements that reflect the specification of a component.
     */
    protected final ImmutableMap<String, E> elements;

    /**
     * Initialize member fields using the given builder.
     *
     * @param builder Builder with necessary information.
     */
    protected ComponentTable(PrivateBuilder<E> builder) {
        this.elements = builder.buildElements();
    }

    /**
     * Get the object with information about the element with given name.
     *
     * @param name Name of the element.
     * @return Object with information about the element with given name.
     *         The object is absent if there is no element with given name.
     * @throws NullPointerException Given name is null.
     * @throws IllegalArgumentException Given name is an empty string.
     */
    public Optional<E> get(String name) {
        checkNotNull(name, "name cannot be null");
        checkArgument(!name.isEmpty(), "name cannot be an empty string");

        return Optional.fromNullable(elements.get(name));
    }

    /**
     * <p>Get immutable set with information about all elements from the
     * specification.</p>
     *
     * @return Immutable set with all information about the specification of
     *         a component.
     */
    public ImmutableSet<Map.Entry<String, E>> getAll() {
        return elements.entrySet();
    }

    /**
     * Mark the implementation requirement associated with element with given
     * name as fulfilled.
     *
     * @param name Name of the element.
     * @throws NullPointerException Given name is null.
     * @throws IllegalArgumentException Given name is an empty string.
     */
    public abstract void markFulfilled(String name);

    /**
     * Interface with operations of building separate elements of a component
     * table.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private interface PrivateBuilder<E> {
        ImmutableMap<String, E> buildElements();
    }

    /**
     * A base class of a component table builder.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static abstract class Builder<E, T extends ComponentTable<E>> {
        /**
         * Get the builder for the elements of a component table.
         *
         * @return Builder that will build elements of a component table.
         */
        protected abstract PrivateBuilder<E> getComponentTablePrivateBuilder();

        /**
         * Create a new instance of the table.
         *
         * @return Newly created table object.
         */
        protected abstract T create();

        /**
         * Perform the whole process of building a component table.
         *
         * @return Newly built component table.
         */
        public final T build() {
            return create();
        }
    }

    /**
     * Builder for a component table created from the specification of
     * a component.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static abstract class FromSpecificationBuilder<E, T extends ComponentTable<E>> extends Builder<E, T> {
        /**
         * Objects necessary to build the analyzer.
         */
        private final List<RpInterface> specification = new ArrayList<>();

        /**
         * <p>Add declarations that will be used to build a component table.
         * Only declarations from the given list that are instances of
         * {@link RpInterface} class are used.</p>
         *
         * @param declarations List with declarations to add.
         * @return <code>this</code>
         */
        public Builder<E, T> addDeclarations(List<? extends Declaration> declarations) {
            for (Declaration declaration : declarations) {
                if (declaration instanceof RpInterface) {
                    specification.add((RpInterface) declaration);
                }
            }

            return this;
        }

        /**
         * Add the elements that are implied by the given declaration to the
         * given builder.
         *
         * @param declaration Reference to an interface.
         * @param builder Builder to add the elements to.
         */
        protected abstract void addInterfaceElements(InterfaceRefDeclaration declaration,
                ImmutableMap.Builder<String, E> builder);

        /**
         * Add the elements that are implied by the given declaration to the
         * given builder.
         *
         * @param declaration Declaration object that represents a bare command
         *                    or event.
         * @param kind Kind of the bare element (whether it is a command or an
         *             event).
         * @param builder Builder to add the elements to.
         */
        protected abstract void addBareElements(FunctionDeclaration declaration,
                InterfaceEntity.Kind kind, ImmutableMap.Builder<String, E> builder);

        @Override
        protected final PrivateBuilder<E> getComponentTablePrivateBuilder() {
            return new FromSpecificationPrivateBuilder();
        }

        /**
         * Builder of particular elements of a component table.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private class FromSpecificationPrivateBuilder implements PrivateBuilder<E> {
            /**
             * Variables used for the building process.
             */
            private Set<String> analyzedNames;
            private ImmutableMap.Builder<String, E> builder;

            @Override
            public ImmutableMap<String, E> buildElements() {
                builder = ImmutableMap.builder();
                analyzedNames = new HashSet<>();

                // Traverse the declarations and build the map
                for (RpInterface rp : specification) {
                    for (Declaration specElement : rp.getDeclarations()) {
                        if (specElement instanceof InterfaceRef) {
                            // An interface is provided or used
                            buildFromInterfaceRef((InterfaceRef) specElement);
                        } else if (specElement instanceof DataDecl) {
                            // Bare command or event
                            buildFromDataDecl((DataDecl) specElement);
                        } else if (LOG.isTraceEnabled()) {
                            LOG.trace(format("Ignoring a declaration of class %s", specElement.getClass()));
                        }

                    }
                }

                return builder.build();
            }


            private void buildFromInterfaceRef(InterfaceRef ifaceRef) {
                final InterfaceRefDeclaration declaration = ifaceRef.getDeclaration();

                // Ignore a redeclaration
                if (!analyzedNames.add(declaration.getName())) {
                    return;
                }

                FromSpecificationBuilder.this.addInterfaceElements(ifaceRef.getDeclaration(), builder);
            }

            private void buildFromDataDecl(DataDecl dataDecl) {

                for (Declaration declaration : dataDecl.getDeclarations()) {
                    if (declaration instanceof VariableDecl) {
                        final ObjectDeclaration objDecl = ((VariableDecl) declaration).getDeclaration();
                        if (objDecl.getKind() != ObjectKind.FUNCTION) {
                            continue;
                        }

                        final FunctionDeclaration funDecl = (FunctionDeclaration) objDecl;
                        final InterfaceEntity.Kind kind;

                        switch (funDecl.getFunctionType()) {
                            case COMMAND:
                                kind = InterfaceEntity.Kind.COMMAND;
                                break;
                            case EVENT:
                                kind = InterfaceEntity.Kind.EVENT;
                                break;
                            default:
                                continue;
                        }

                        // Ignore redeclaration
                        if (!analyzedNames.add(funDecl.getName())) {
                            continue;
                        }

                        FromSpecificationBuilder.this.addBareElements(funDecl, kind, builder);

                    } else if (LOG.isTraceEnabled()) {
                        LOG.trace(format("Ignoring a declaration of class %s at %s:%s:%s", declaration.getClass(),
                                dataDecl.getLocation().getFilePath(), dataDecl.getLocation().getLine(),
                                dataDecl.getLocation().getColumn()));
                    }
                }
            }
        }
    }

    /**
     * Builder responsible for creating a copy of the table.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static abstract class CopyingBuilder<E, T extends ComponentTable<E>> extends Builder<E, T> {
        /**
         * Map that will be copied.
         */
        private final ImmutableMap<String, E> specimenElements;

        /**
         * Implementation of the builder that will build particular elements
         * of the component table.
         */
        private final PrivateBuilder<E> privateBuilder = new PrivateBuilder<E>() {
            @Override
            public final ImmutableMap<String, E> buildElements() {
                final ImmutableMap.Builder<String, E> elementsBuilder = ImmutableMap.builder();

                for (Map.Entry<String, E> elementEntry : specimenElements.entrySet()) {
                    final E elementCopy = CopyingBuilder.this.copyElement(elementEntry.getValue());
                    elementsBuilder.put(elementEntry.getKey(), elementCopy);
                }

                return elementsBuilder.build();
            }
        };

        /**
         * Initializes this builder to copy the given table.
         *
         * @param specimen Object to copy.
         */
        protected CopyingBuilder(T specimen) {
            this.specimenElements = specimen.elements;
        }

        /**
         * Get the copy of an element of the table.
         *
         * @param specimen Element to be copied.
         * @return A newly created copy of the given element. It must be new
         *         instance.
         */
        protected abstract E copyElement(E specimen);

        @Override
        protected final PrivateBuilder<E> getComponentTablePrivateBuilder() {
            return privateBuilder;
        }
    }
}
