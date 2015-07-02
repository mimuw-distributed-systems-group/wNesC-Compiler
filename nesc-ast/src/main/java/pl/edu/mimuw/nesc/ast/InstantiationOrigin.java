package pl.edu.mimuw.nesc.ast;

import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Object with information about an element in the instantiation chain of
 * a component. It specifies the name of the component and the name of its
 * reference.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InstantiationOrigin {
    /**
     * Name of the component. This is the name as it appears in the NesC
     * program, without any mangling.
     */
    private final String componentName;

    /**
     * Name of the reference to the component from a configuration. It is absent
     * if this is the first element of the instantiation chain.
     */
    private final Optional<String> componentRefName;

    /**
     * Create an instantiation origin for the first element of an instantiation
     * chain. The first element if always a non-generic configuration.
     *
     * @param nonGenericConfigurationName Name of the non-generic configuration.
     * @return Instantiation origin object with given name of the component and
     *         absent component reference.
     */
    public static InstantiationOrigin forFirstElement(String nonGenericConfigurationName) {
        return new InstantiationOrigin(nonGenericConfigurationName, Optional.<String>absent());
    }

    /**
     * Create an instantiation origin object for an element of an instantiation
     * chain that is not the first element. It always corresponds to
     * a specification element from the configuration the precedes it in the
     * chain.
     *
     * @param genericComponentName Name of the generic component.
     * @param componentRefName Name of the reference in the configuration that
     *                         precedes this element in the instantiation chain.
     * @return Instantiation origin object with given name of component and name
     *         of component reference.
     */
    public static InstantiationOrigin forNextElement(String genericComponentName,
            String componentRefName) {
        return new InstantiationOrigin(genericComponentName, Optional.of(componentRefName));
    }

    private InstantiationOrigin(String componentName, Optional<String> componentRefName) {
        checkNotNull(componentName, "name of the component cannot be null");
        checkNotNull(componentRefName, "name of the component reference cannot be null");
        checkArgument(!componentName.isEmpty(), "name of the component cannot be an empty string");
        checkArgument(!componentRefName.isPresent() || !componentRefName.get().isEmpty(),
                "name of the component reference cannot be an empty string");
        this.componentName = componentName;
        this.componentRefName = componentRefName;
    }

    /**
     * Get the name of the component. It is either a name of a non-generic
     * configuration (and, then, there the component reference name is absent)
     * or a name of a generic component.
     *
     * @return Name of the component.
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * Name of the component reference of the component in the configuration
     * that precedes this element in the instantiation chain. It is absent if
     * and only if this element is the first element of the instantiation chain.
     *
     * @return Name of the component reference from previous instantiation
     *         origin in the instantiation chain.
     */
    public Optional<String> getComponentRefName() {
        return componentRefName;
    }
}
