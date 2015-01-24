package pl.edu.mimuw.nesc.instantiation;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import java.util.Iterator;
import pl.edu.mimuw.nesc.ast.gen.ComponentRef;
import pl.edu.mimuw.nesc.ast.gen.ComponentsUses;
import pl.edu.mimuw.nesc.ast.gen.Configuration;
import pl.edu.mimuw.nesc.ast.gen.ConfigurationImpl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class that represents a configuration with generic components that are
 * currently being instantiated.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class ConfigurationNode {
    /**
     * Iterator for the elements of the configuration implementation.
     */
    private final Iterator<ComponentsUses> usesIterator;

    /**
     * Iterator for references of components extracted from
     * <code>ComponentsUses</code> obtained from <code>usesIterator</code>.
     */
    private Optional<Iterator<ComponentRef>> referencesIterator = Optional.absent();

    /**
     * AST node of the implementation of the configuration that is represented
     * by this node.
     */
    private final ConfigurationImpl implementation;

    /**
     * Data object associated with the same configuration that this node is.
     */
    private final ComponentData componentData;

    /**
     * Create a configuration node for a start, non-generic configuration. The
     * component references that are iterated are from the configuration
     * associated with given data object.
     *
     * @param componentData Component data object associated with a non-generic
     *                      configuration.
     * @return Newly created configuration node.
     * @throws NullPointerException Given data object is null.
     * @throws IllegalArgumentException Given data object is not associated with
     *                                  a non-generic configuration.
     */
    public static ConfigurationNode forStartConfiguration(ComponentData componentData) {
        checkNotNull(componentData, "component data object cannot be null");
        checkArgument(componentData.getComponent() instanceof Configuration,
                "given component data is not associated with a configuration");
        checkArgument(!componentData.getComponent().getIsAbstract(),
                "start configuration cannot be generic");

        final ConfigurationImpl impl = (ConfigurationImpl) componentData.getComponent().getImplementation();
        return new ConfigurationNode(componentData, impl);
    }

    /**
     * Create a configuration node for a newly instantiated configuration. The
     * given configuration shall not be the same as in the given data object.
     * The latter shall be associated with the original generic configuration.
     *
     * @param specimenData Data object for configuration that has been copied to
     *                     obtain the given configuration.
     * @param newConfiguration Newly created configuration.
     * @return Newly created configuration node.
     * @throws NullPointerException One of the arguments is null.
     */
    public static ConfigurationNode forNewConfiguration(ComponentData specimenData,
            Configuration newConfiguration) {
        checkNotNull(specimenData, "specimen data cannot be null");
        checkNotNull(newConfiguration, "new configuration cannot be null");

        final ConfigurationImpl impl = (ConfigurationImpl) newConfiguration.getImplementation();
        return new ConfigurationNode(specimenData, impl);
    }

    /**
     * <p>Initializes this node to iterate over components references of the
     * given configuration implementation. This node object is associated with
     * given component data object.</p>
     *
     * @param componentData Component data object associated with
     *                      a configuration.
     * @param impl Implementation of a configuration to iterate over.
     */
    public ConfigurationNode(ComponentData componentData, ConfigurationImpl impl) {
        this.componentData = componentData;
        this.usesIterator = FluentIterable.from(impl.getDeclarations())
                .filter(ComponentsUses.class)
                .iterator();
        this.implementation = impl;
    }

    /**
     * Get the data object of the associated configuration.
     *
     * @return Data object of the associated configuration.
     */
    public ComponentData getComponentData() {
        return componentData;
    }

    /**
     * Get the AST node of implementation of the configuration that is
     * represented by this node.
     *
     * @return The implementation AST node.
     */
    public ConfigurationImpl getConfigurationImpl() {
        return implementation;
    }

    /**
     * Get the next component reference from the configuration associated with
     * this node.
     *
     * @return Next component reference from the configuration associated with
     *         this node or absent object if there are no more component
     *         references in the associated configuration.
     */
    public Optional<ComponentRef> nextComponentRef() {
        while (usesIterator.hasNext() && (!referencesIterator.isPresent()
                || !referencesIterator.get().hasNext())) {

            referencesIterator = Optional.of(usesIterator.next().getComponents().iterator());
        }

        return referencesIterator.isPresent() && referencesIterator.get().hasNext()
                ? Optional.of(referencesIterator.get().next())
                : Optional.<ComponentRef>absent();

    }
}
