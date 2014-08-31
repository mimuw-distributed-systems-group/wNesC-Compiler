package pl.edu.mimuw.nesc.analysis.type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An artificial type that represents a component, e.g.
 * <code>components new C(int) as Y</code>.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ComponentType extends NescType {
    /**
     * Name of the component that is referenced. Never null or empty.
     */
    private final String componentName;

    public ComponentType(String componentName) {
        checkNotNull(componentName, "component name cannot be null");
        checkArgument(!componentName.isEmpty(), "component name cannot be empty");

        this.componentName = componentName;
    }

    /**
     * @return Name of the component referenced by this type. Never null or
     *         empty.
     */
    public final String getComponentName() {
        return componentName;
    }
}
