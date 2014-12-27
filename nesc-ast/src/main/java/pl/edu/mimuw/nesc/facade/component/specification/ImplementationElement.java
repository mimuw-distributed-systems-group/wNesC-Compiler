package pl.edu.mimuw.nesc.facade.component.specification;

import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Class that represents a single implementation element of a module.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class ImplementationElement {
    /**
     * Value indicating if the command or event has been implemented.
     */
    private boolean isImplemented = false;

    /**
     * Unique name of this implementation element.
     */
    private Optional<String> uniqueName = Optional.absent();

    /**
     * Create the deep copy of this implementation element.
     *
     * @return Newly created, deep copy of this implementation element. Class
     *         of the returned element is the same as the concrete class of this
     *         element.
     */
    abstract ImplementationElement deepCopy();

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

    /**
     * Get the unique name of this implementation element. The returned name is
     * intended to be the name of the intermediate function used for
     * implementation of this element.
     *
     * @return Unique name of this implementation element. If it hasn't been set
     *         yet, the object is absent.
     */
    public Optional<String> getUniqueName() {
        return uniqueName;
    }

    /**
     * Set the unique name of this implementation element.
     *
     * @param uniqueName Name to set.
     * @throws NullPointerException Unique name is <code>null</code>.
     * @throws IllegalArgumentException Unique name is an empty string.
     * @throws IllegalStateException The unique name has been already set.
     */
    void setUniqueName(String uniqueName) {
        checkNotNull(uniqueName, "unique name cannot be null");
        checkArgument(!uniqueName.isEmpty(), "unique name cannot be an empty string");
        checkState(!this.uniqueName.isPresent(), "unique name has been already set");

        this.uniqueName = Optional.of(uniqueName);
    }
}
