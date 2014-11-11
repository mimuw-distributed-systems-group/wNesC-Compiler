package pl.edu.mimuw.nesc.facade.component.specification;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class that represents a single element from the specification of
 * a configuration that must be wired.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class WiringElement {
    /**
     * Value indicating if this element has been already wired in
     * a configuration.
     */
    private boolean isWired = false;

    /**
     * Kind of this element of wiring.
     */
    private final Kind kind;

    /**
     * Initialize this object by storing given values in member fields.
     *
     * @param kind Kind of this wiring element.
     */
    WiringElement(Kind kind) {
        checkNotNull(kind, "kind of the wiring element cannot be null");
        this.kind = kind;
    }

    /**
     * Check if this wiring element has been already wired.
     *
     * @return Value indicating if this wiring element has been already wired.
     */
    public boolean isWired() {
        return isWired;
    }

    /**
     * Get the kind of this wiring element.
     *
     * @return The kind of this wiring element.
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Set that this wiring element has been wired. Elements can be wired
     * multiple times so there is no exception thrown if this element has been
     * already wired.
     */
    void wired() {
        isWired = true;
    }

    /**
     * Type that represents kind of an element that can be wired.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum Kind {
        INTERFACE,
        BARE_COMMAND,
        BARE_EVENT,
    }
}
