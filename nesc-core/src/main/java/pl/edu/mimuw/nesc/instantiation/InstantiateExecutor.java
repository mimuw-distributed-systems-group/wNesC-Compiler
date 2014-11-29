package pl.edu.mimuw.nesc.instantiation;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.analysis.NameMangler;
import pl.edu.mimuw.nesc.ast.gen.BinaryComponent;
import pl.edu.mimuw.nesc.ast.gen.Component;
import pl.edu.mimuw.nesc.ast.gen.ComponentRef;
import pl.edu.mimuw.nesc.ast.gen.Configuration;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.ast.gen.RemanglingVisitor;
import pl.edu.mimuw.nesc.ast.gen.SubstitutionManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * <p>Class that performs the instantiation of generic components.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InstantiateExecutor {
    /**
     * Logger for instantiate executors.
     */
    private static final Logger LOG = Logger.getLogger(InstantiateExecutor.class);

    /**
     * Function that returns the alias (if it is absent, then name) of the
     * referred component.
     */
    private static final Function<ComponentRef, String> RETRIEVE_COMPONENT_ALIAS = new Function<ComponentRef, String>() {
        @Override
        public String apply(ComponentRef componentRef) {
            checkNotNull(componentRef, "component reference cannot be null");

            return componentRef.getAlias().isPresent()
                    ? componentRef.getAlias().get().getName()
                    : componentRef.getName().getName();
        }
    };

    /**
     * Function used for mangling names in created components. Returns name
     * after mangling.
     */
    private static final Function<String, String> MANGLING_FUN = new Function<String, String>() {
        private final NameMangler mangler = NameMangler.getInstance();

        @Override
        public String apply(String name) {
            return mangler.mangle(name);
        }
    };

    /**
     * Current path that leads from a non-generic configuration through
     * instantiated generic components. It is used as a stack for DFS algorithm.
     * The only component that is not a result of instantiation is the first
     * component that has been placed on the stack. It is used to detect cycles
     * during the creation of components.
     */
    private final Deque<ConfigurationNode> currentPath = new ArrayDeque<>();

    /**
     * Immutable map with information about components to process. Keys are
     * names of components and values are data objects associated with them.
     */
    private final ImmutableMap<String, ComponentData> components;

    /**
     * Set that contains instantiated components (new components that previously
     * hasn't existed). It is absent before performing the instantiation.
     */
    private Optional<Set<Component>> instantiatedComponents = Optional.absent();

    /**
     * Set to accumulate newly created components.
     */
    private final Set<Component> componentsAccumulator = new HashSet<>();

    /**
     * Get a builder that will create an instantiate executor.
     *
     * @return Newly created builder of an instantiate executor.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Initializes this executor with information provided by the builder.
     *
     * @param builder Builder with necessary information.
     */
    private InstantiateExecutor(Builder builder) {
        this.components = builder.buildComponentsMap();
    }

    /**
     * <p>Performs the instantiation of all generic components that are
     * created in components that have been added to the builder. References
     * to generic parameters of instantiated components are replaced with
     * provided types and expressions in <code>new</code> construct.</p>
     *
     * <p>The given components are modified to correctly refer to instantiated
     * components.</p>
     *
     * @return Set with new components, created as the result of instantiation.
     */
    public Set<Component> instantiate() throws CyclePresentException {
        if (instantiatedComponents.isPresent()) {
            return instantiatedComponents.get();
        }

        componentsAccumulator.clear();
        currentPath.clear();

        // Loop over all components and start paths in non-generic configurations
        for (ComponentData componentData : components.values()) {
            final Component currentComponent = componentData.getComponent();

            if (!currentComponent.getIsAbstract() && currentComponent instanceof Configuration) {
                instantiateFor(componentData);
            } else {
                LOG.debug("Skipping component '" + currentComponent.getName().getName() + "'");
            }
        }

        instantiatedComponents = Optional.of(componentsAccumulator);
        return componentsAccumulator;
    }

    private void instantiateFor(ComponentData configurationData) throws CyclePresentException {
        final String configurationName = configurationData.getComponent().getName().getName();
        LOG.debug("Starting instantiation of components for '" + configurationName + "'");

        // Place the starting configuration on the stack
        currentPath.addLast(ConfigurationNode.forStartConfiguration(configurationData));

        // Perform the instantiation
        while (!currentPath.isEmpty()) {
            final Optional<ComponentRef> nextReference = currentPath.getLast().nextComponentRef();

            if (nextReference.isPresent() && nextReference.get().getIsAbstract()) {
                performComponentInstantiation(nextReference.get());
            } else if (!nextReference.isPresent()) {
                final ConfigurationNode lastNode = currentPath.removeLast();
                lastNode.getComponentData().setCurrentlyInstantiated(false);
                logPath();
            } else {
                LOG.trace(format("Skipping component reference '%s'",
                        RETRIEVE_COMPONENT_ALIAS.apply(nextReference.get())));
            }
        }

        LOG.debug("Instantiation of components for '" + configurationName + "' successfully ended");
    }

    private void performComponentInstantiation(ComponentRef genericRef) throws CyclePresentException {
        // Locate the new component
        final Optional<ComponentData> optInstantiatedComponentData =
                Optional.fromNullable(components.get(genericRef.getName().getName()));
        if (!optInstantiatedComponentData.isPresent()) {
            throw new IllegalStateException("unknown component '" + genericRef.getName().getName()
                    + "' is instantiated");
        }
        final ComponentData instantiatedComponentData = optInstantiatedComponentData.get();

        // Check if there is no cycle
        checkState(instantiatedComponentData.getComponent().getIsAbstract(), "instantiating non-generic component '%s'",
                genericRef.getName().getName());
        if (instantiatedComponentData.isCurrentlyInstantiated()) {
            throw CyclePresentException.newInstance(currentPath, genericRef.getName().getName());
        }

        // FIXME copy and prepare the component
        logInstantiationInfo(genericRef);
        final Component newComponent = copyComponent(instantiatedComponentData.getComponent(), genericRef);

        // Update state
        componentsAccumulator.add(newComponent);
        if (newComponent instanceof Configuration) {
            instantiatedComponentData.setCurrentlyInstantiated(true);
            final ConfigurationNode newNode = ConfigurationNode.forNewConfiguration(instantiatedComponentData,
                    (Configuration) newComponent);
            currentPath.addLast(newNode);
            logPath();
        }
    }

    /**
     * Copy the given component and make all necessary changes to the copy.
     *
     * @param specimen Generic component that will be copied.
     * @param genericRef Reference to the generic component that causes the copy
     *                   to be created (the <code>new</code> construct).
     * @return Properly modified and prepared copy of the given component.
     */
    private Component copyComponent(Component specimen, ComponentRef genericRef) {
        final Component copy = specimen.deepCopy();
        final RemanglingVisitor manglingVisitor = new RemanglingVisitor(MANGLING_FUN);
        final SubstitutionManager substitution = GenericParametersSubstitution.builder()
                .componentRef(genericRef)
                .genericComponent(specimen)
                .build();

        copy.setIsAbstract(false);
        copy.setParameters(Optional.<LinkedList<Declaration>>absent());
        copy.accept(manglingVisitor, null);
        copy.substitute(substitution);

        return copy;
    }

    private void logPath() {
        if (LOG.isDebugEnabled()) {
            final StringBuilder path = new StringBuilder();
            path.append("Current path: ");

            if (!currentPath.isEmpty()) {
                boolean isFirst = true;

                for (ConfigurationNode node : currentPath) {
                    if (!isFirst) {
                        path.append(" -> ");
                    }
                    path.append(node.getComponentData().getComponent().getName().getName());
                    isFirst = false;
                }
            } else {
                path.append("<empty>");
            }

            LOG.debug(path);
        }
    }

    private void logInstantiationInfo(ComponentRef componentRef) {
        if (LOG.isDebugEnabled()) {
            final String componentRefName = RETRIEVE_COMPONENT_ALIAS.apply(componentRef);
            final String componentName = componentRef.getName().getName();

            if (!componentName.equals(componentRefName)) {
                LOG.debug(format("Instantiating component '%s' aliased as '%s'", componentName,
                        componentRefName));
            } else {
                LOG.debug(format("Instantiating component '%s'", componentName));
            }
        }
    }

    /**
     * Builder for an instantiate executor.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder {
        /**
         * Data needed to build an instantiate executor.
         */
        private final List<Component> components = new ArrayList<>();

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder() {
        }

        /**
         * Adds the given component to take part in the instantiation process.
         *
         * @param component Component to add.
         * @return <code>this</code>
         */
        public Builder addComponent(Component component) {
            this.components.add(component);
            return this;
        }

        /**
         * Add components from the given list of nodes. Only nodes from the
         * given list that represent components are added.
         *
         * @param nodes List with nodes (and possibly components) to add.
         * @return <code>this</code>
         */
        public Builder addComponents(List<? extends Node> nodes) {
            FluentIterable.from(nodes)
                    .filter(Component.class)
                    .copyInto(components);
            return this;
        }

        private void validate() {
            final Set<String> names = new HashSet<>();

            for (Component component : components) {
                checkNotNull(component, "null has been added as a component");

                if (!names.add(component.getName().getName())) {
                    throw new IllegalStateException("names of added components are not unique");
                }
            }
        }

        public InstantiateExecutor build() {
            validate();
            return new InstantiateExecutor(this);
        }

        private ImmutableMap<String, ComponentData> buildComponentsMap() {
            final ImmutableMap.Builder<String, ComponentData> builder = ImmutableMap.builder();

            for (Component component : components) {
                if (component instanceof BinaryComponent) {
                    continue;
                }

                builder.put(component.getName().getName(), new ComponentData(component));
            }

            return builder.build();
        }
    }
}
