package pl.edu.mimuw.nesc.instantiation;

import pl.edu.mimuw.nesc.ast.gen.Component;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class that is supposed to contain information about a component associated
 * with its instantiation.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class ComponentData {
    /**
     * Component associated with this data object.
     */
    private final Component component;

    /**
     * Value indicating if a generic component created by the component
     * associated with this data object is currently instantiated. This flag is
     * meaningful only for configurations.
     */
    private boolean currentlyInstantiated = false;

    /**
     * Initialize this data object with all flags lowered and with the given
     * component.
     *
     * @param component Component associated with this data object.
     * @throws NullPointerException Component is null.
     */
    public ComponentData(Component component) {
        checkNotNull(component, "component cannot be null");
        this.component = component;
    }

    /**
     * Get the component associated with this data object.
     *
     * @return Component associated with this data object.
     */
    public Component getComponent() {
        return component;
    }

    /**
     * Get the current value of <code>currentlyInstantiated</code> flag.
     *
     * @return Value of the <code>currentlyInstantiated</code> flag.
     */
    public boolean isCurrentlyInstantiated() {
        return currentlyInstantiated;
    }

    /**
     * Set the value of <code>currentlyInstantiated</code> flag.
     *
     * @param value Value of <code>currentlyInstantiated</code> flag to set.
     */
    public void setCurrentlyInstantiated(boolean value) {
        this.currentlyInstantiated = value;
    }
}
