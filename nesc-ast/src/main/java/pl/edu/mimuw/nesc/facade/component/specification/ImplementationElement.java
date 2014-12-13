package pl.edu.mimuw.nesc.facade.component.specification;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.facade.iface.InterfaceEntity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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
