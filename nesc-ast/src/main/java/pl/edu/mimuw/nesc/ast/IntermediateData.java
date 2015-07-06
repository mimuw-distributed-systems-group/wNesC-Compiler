package pl.edu.mimuw.nesc.ast;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.facade.iface.InterfaceEntity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Information about the command or event an intermediate function corresponds
 * to.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class IntermediateData {
    /**
     * Name of the component that specifies the command or event the
     * intermediate function corresponds to.
     */
    private final String componentName;

    /**
     * Name of the reference to the interface that contains the command or
     * event the intermediate function corresponds to.
     */
    private final Optional<String> interfaceRefName;

    /**
     * Name of the command or event the intermediate function corresponds to.
     */
    private final String entityName;

    /**
     * Value indicating if intermediate function corresponds to a command or
     * event.
     */
    private final InterfaceEntity.Kind kind;

    /**
     * Value indicating if the command or event the intermediate function
     * corresponds to is implemented.
     */
    private Optional<Boolean> isImplemented;

    public IntermediateData(String componentName, Optional<String> interfaceRefName,
            String entityName, InterfaceEntity.Kind kind) {
        checkNotNull(componentName, "name of the component cannot be null");
        checkNotNull(interfaceRefName, "name of the interface reference cannot be null");
        checkNotNull(entityName, "name of the entity cannot be null");
        checkNotNull(kind, "kind cannot be null");
        checkArgument(!componentName.isEmpty(), "name of the component cannot be an empty string");
        checkArgument(!interfaceRefName.isPresent() || !interfaceRefName.get().isEmpty(),
                "name of the interface reference cannot be an empty string");
        checkArgument(!entityName.isEmpty(), "name of the command or event cannot be an empty string");;

        this.componentName = componentName;
        this.interfaceRefName = interfaceRefName;
        this.entityName = entityName;
        this.kind = kind;
        this.isImplemented = Optional.absent();
    }

    /**
     * Get the name of the component that specifies the command or event the
     * intermediate function corresponds to.
     *
     * @return Name of the component.
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * Get the name of the reference of the interface that contains the command
     * or event the intermediate function corresponds to. The object is absent
     * if the intermediate function corresponds to a bare command or a bare
     * event.
     *
     * @return Name of the interface reference.
     */
    public Optional<String> getInterfaceRefName() {
        return interfaceRefName;
    }

    /**
     * Get the name of the command or event the intermediate function
     * corresponds to.
     *
     * @return Name of the command or event.
     */
    public String getEntityName() {
        return entityName;
    }

    /**
     * Get the value indicating if the intermediate function corresponds to
     * a command or event.
     *
     * @return Kind of the entity: a command or an event.
     */
    public InterfaceEntity.Kind getKind() {
        return kind;
    }

    /**
     * Set the value indicating if the command or event the intermediate
     * function corresponds to is implemented, i.e. it is connected or
     * a default implementation is provided.
     *
     * @param value Value to set.
     * @throws IllegalStateException The value has been already set.
     */
    public void setIsImplemented(boolean value) {
        checkState(!this.isImplemented.isPresent(), "the value has been already set");
        this.isImplemented = Optional.of(value);
    }

    /**
     * Check if the command or event the intermediate function corresponds to
     * is implemented, i.e. it is connected or a default implementation is
     * provided for it.
     *
     * @return <code>true</code> if and only if the command or event the
     *         intermediate function corresponds to is properly implemented
     *         either by connecting it or providing a default implementation.
     */
    public boolean isImplemented() {
        checkState(this.isImplemented.isPresent(), "the value has not been set yet");
        return this.isImplemented.get();
    }
}
