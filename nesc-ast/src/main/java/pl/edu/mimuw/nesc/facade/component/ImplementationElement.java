package pl.edu.mimuw.nesc.facade.component;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.facade.iface.InterfaceEntity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that represents a single implementation element of a module.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ImplementationElement {
    /**
     * Value indicating if the command or event has been implemented.
     */
    private boolean isImplemented = false;

    /**
     * Value indicating if the module must implement the command or event. It
     * shall be <code>true</code> for commands from provided interfaces, events
     * from used interfaces and provided bare commands and events.
     */
    private final boolean isProvided;

    /**
     * Kind of the implementation element.
     */
    private final InterfaceEntity.Kind kind;

    /**
     * Name of the interface that the command or event comes from (not its
     * alias used in the specification). The object is absent for a bare command
     * or event.
     */
    private final Optional<String> interfaceName;

    /**
     * Initialize the object by storing given values in its state.
     *
     * @param kind Kind of the element that must be implemented.
     * @param interfaceName Name of the interface the command or event comes
     *                      from (absent if bare command or event).
     * @throws NullPointerException One of the arguments is null.
     * @throws IllegalArgumentException The interface name is present and
     *                                  empty.
     */
    ImplementationElement(boolean isProvided, InterfaceEntity.Kind kind,
            Optional<String> interfaceName) {
        checkNotNull(kind, "kind cannot be null");
        checkNotNull(interfaceName, "name of the interface cannot be null");
        checkArgument(!interfaceName.isPresent() || !interfaceName.get().isEmpty(),
                      "the interface name cannot be empty");

        this.isProvided = isProvided;
        this.kind = kind;
        this.interfaceName = interfaceName;
    }

    /**
     * Check if the command or event must be implemented in a module in order to
     * satisfy specification requirements.
     *
     * @return Value indicating if a command or event must be implemented.
     */
    public boolean isProvided() {
        return isProvided;
    }

    /**
     * Check if this object corresponds to a command or event.
     *
     * @return Kind of this entity.
     */
    public InterfaceEntity.Kind getKind() {
        return kind;
    }

    /**
     * Get the interface name if this object corresponds to a command or event
     * from an interface. This method can also be used to check if this object
     * represents a bare command or event.
     *
     * @return Name of the interface that this command or event comes from (if
     *         not bare).
     */
    public Optional<String> getInterfaceName() {
        return interfaceName;
    }

    /**
     * Check if this command or event has been implemented.
     *
     * @return Value indicating if a command or event has been implemented.
     */
    public boolean isImplemented() {
        return isImplemented;
    }

    /**
     * Set that the command or event has been implemented.
     */
    void implemented() {
        isImplemented = true;
    }
}
