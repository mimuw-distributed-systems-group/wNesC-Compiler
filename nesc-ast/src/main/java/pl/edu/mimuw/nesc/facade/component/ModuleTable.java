package pl.edu.mimuw.nesc.facade.component;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.LinkedList;
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
import pl.edu.mimuw.nesc.facade.iface.InterfaceRefFacade;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * <p>A class that is responsible for storing information about a module
 * implementation. It handles a list of all commands and events that can be
 * implemented in a module and provides information about them.</p>
 *
 * <p>It is a kind of a high level symbol table.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ModuleTable {
    /**
     * Logger for this class.
     */
    private static final Logger LOG = Logger.getLogger(ModuleTable.class);

    /**
     * <p>A map whose set of entries is the least set that fulfills the
     * following conditions:</p>
     *
     * <ul>
     *     <li>the map contains an entry for each command or event of each
     *     interface the component provides or uses</li>
     *     <li>the map contains an entry for each bare command or event
     *     the component provides</li>
     * </ul>
     *
     * <p>In other words, the content of the map depicts all commands and events
     * that a module can implement (including default implementations).</p>
     *
     * <p>The keys of the map are constructed in the following way. For
     * a command or an event from an interface the key is the name of the
     * interface reference and the name of the command or event separated by
     * a dot. If it is a bare command or event, the key is simply its name.</p>
     *
     * <p>For the following declarations:</p>
     * <pre>
     *     interface I { command void c(); event void e(); }
     *
     *     module M
     *     {
     *         provides interface I as I1;
     *         provides interface I as I2;
     *         provides command void c();
     *         provides event void e();
     *     }
     *     implementation { &hellip; }
     * </pre>
     * <p>the set of keys shall be equal to:</p>
     * <pre>{ I1.c, I1.e, I2.c, I2.e, c, e }</pre>
     */
    private final ImmutableMap<String, ImplementationElement> entities;

    /**
     * Get the object that will create a module implementation analyzer.
     *
     * @return Newly created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Initialize this instance using the builder.
     *
     * @param builder Builder with necessary information.
     */
    private ModuleTable(Builder builder) {
        this.entities = builder.buildEntities();
    }

    /**
     * Get information about implementation of a command or event with the given
     * name (with a dot if from an interface).
     *
     * @param name Name of the command or event.
     * @return Information about an implementation element with the given name.
     *         It is absent if a module must not implement a command or event
     *         with given name.
     */
    public Optional<ImplementationElement> get(String name) {
        checkNotNull(name, "name cannot be null");
        checkArgument(!name.isEmpty(), "name cannot be an empty string");

        return Optional.fromNullable(entities.get(name));
    }

    /**
     * Get information about elements from the module.
     *
     * @return Immutable set with information about commands and events that can
     *         or must be implemented in a module. Keys are names of commands
     *         and events, e.g. <code>I1.c</code>, <code>c</code>.
     */
    public ImmutableSet<Map.Entry<String, ImplementationElement>> getAll() {
        return entities.entrySet();
    }

    /**
     * Mark the implementation element with given name as implemented.
     *
     * @param name Name of a command or event (with dot if from an interface).
     * @throws NullPointerException Name is null.
     * @throws IllegalArgumentException Name is an empty string.
     * @throws IllegalStateException Element with given name has been already
     *                               marked as implemented.
     */
    public void markImplemented(String name) {
        checkNotNull(name, "name cannot be null");
        checkArgument(!name.isEmpty(), "name cannot be an empty string");

        final Optional<ImplementationElement> optElement = get(name);
        checkArgument(optElement.isPresent(), "'%s' is not a valid name of a command or event", name);

        final ImplementationElement element = optElement.get();

        checkState(!element.isImplemented(), "element '%s' is already marked as implemented");
        element.implemented();
    }

    /**
     * Builder for a module table. It shall be built after the specification of
     * a module is fully parsed and analyzed and interfaces references facades
     * are set.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder {
        /**
         * Objects necessary to build the analyzer.
         */
        private final List<RpInterface> specification = new LinkedList<>();

        /**
         * Variables used for the building process.
         */
        private Set<String> analyzedNames;
        private ImmutableMap.Builder<String, ImplementationElement> builder;

        /**
         * Private constructor to prevent this class from an unauthorized
         * instantiation.
         */
        private Builder() {
        }

        /**
         * Add declarations from the specification of a module that define
         * commands or events a module can or must implement. Only instances
         * of <code>RpInterface</code> from the given list are used.
         *
         * @param declarations List with declarations from the specification.
         * @return <code>this</code>
         */
        public Builder addDeclarations(List<? extends Declaration> declarations) {
            for (Declaration declaration : declarations) {
                if (declaration instanceof RpInterface) {
                    specification.add((RpInterface) declaration);
                }
            }

            return this;
        }

        /**
         * Build the table that reflects added declarations.
         *
         * @return Newly created module table.
         */
        public ModuleTable build() {
            return new ModuleTable(this);
        }

        private ImmutableMap<String, ImplementationElement> buildEntities() {
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

            final InterfaceRefFacade facade = declaration.getFacade();

            for (Map.Entry<String, InterfaceEntity> ifaceEntry : facade.getAll()) {
                final String ifaceEntityName = ifaceEntry.getKey();
                final InterfaceEntity ifaceEntity = ifaceEntry.getValue();

                final String name = format("%s.%s", facade.getInstanceName(), ifaceEntityName);
                final boolean isProvided = facade.isProvided() && ifaceEntity.getKind() == InterfaceEntity.Kind.COMMAND
                        || !facade.isProvided() && ifaceEntity.getKind() == InterfaceEntity.Kind.EVENT;

                final ImplementationElement value = new ImplementationElement(isProvided,
                        ifaceEntity.getKind(), Optional.of(facade.getInterfaceName()));

                builder.put(name, value);
            }
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

                    final ImplementationElement value = new ImplementationElement(funDecl.isProvided().get(),
                            kind, Optional.<String>absent());

                    builder.put(funDecl.getName(), value);

                } else if (LOG.isTraceEnabled()) {
                    LOG.trace(format("Ignoring a declaration of class %s at %s:%s:%s", declaration.getClass(),
                            dataDecl.getLocation().getFilePath(), dataDecl.getLocation().getLine(),
                            dataDecl.getLocation().getColumn()));
                }
            }
        }
    }
}
