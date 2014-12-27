package pl.edu.mimuw.nesc.facade.component.specification;

import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>A class that represents a task element of modules implementation.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class TaskElement extends ImplementationElement {
    /**
     * Name of the task interface reference created for this task.
     */
    private Optional<String> interfaceRefName = Optional.absent();

    @Override
    TaskElement deepCopy() {
        final TaskElement result = new TaskElement();

        result.interfaceRefName = this.interfaceRefName;

        if (isImplemented()) {
            result.implemented();
        }

        if (getUniqueName().isPresent()) {
            result.setUniqueName(getUniqueName().get());
        }

        return result;
    }

    /**
     * Set the name of the task interface reference created for this task.
     * It can be set exactly once.
     *
     * @param name Alias to set for the interface.
     * @throws NullPointerException Name is <code>null</code>.
     * @throws IllegalArgumentException Name is an empty string.
     * @throws IllegalStateException The alias has been already set.
     */
    void setInterfaceRefName(String name) {
        checkNotNull(name, "name cannot be null");
        checkArgument(!name.isEmpty(), "name cannot be an empty string");
        checkState(!interfaceRefName.isPresent(), "interface alias has been already set");

        this.interfaceRefName = Optional.of(name);
    }

    /**
     * Get the name of the interface reference created for this task.
     *
     * @return Name of the interface reference created for this task. The
     *         object will be absent if the name has not been set yet.
     */
    public Optional<String> getInterfaceRefName() {
        return interfaceRefName;
    }
}
