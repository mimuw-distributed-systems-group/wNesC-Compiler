package pl.edu.mimuw.nesc.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>A class that contains information about entities necessary for
 * <code>atomic</code> transformation.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class AtomicSpecification {
    /**
     * The specification of atomic used in TinyOS.
     */
    public static final AtomicSpecification DEFAULT_SPECIFICATION = new AtomicSpecification(
            "__nesc_atomic_t",
            "__nesc_atomic_start",
            "__nesc_atomic_end"
    );

    /**
     * String with the name of the type of the value returned by the atomic
     * start function.
     */
    private final String typename;

    /**
     * String with the name of the function called at the beginning of
     * <code>atomic</code> block.
     */
    private final String startFunctionName;

    /**
     * String with the name of the function called at the end of each
     * <code>atomic</code> block.
     */
    private final String endFunctionName;

    public AtomicSpecification(String typename, String startFunctionName, String endFunctionName) {
        checkNotNull(typename, "the name of the type cannot be null");
        checkNotNull(startFunctionName, "name of the start function cannot be null");
        checkNotNull(endFunctionName, "name of the end function cannot be null");
        checkArgument(!typename.isEmpty(), "name of the type cannot be an empty string");
        checkArgument(!startFunctionName.isEmpty(), "name of the start function cannot be an empty string");
        checkArgument(!endFunctionName.isEmpty(), "name of the end function cannot be an empty string");

        this.typename = typename;
        this.startFunctionName = startFunctionName;
        this.endFunctionName = endFunctionName;
    }

    /**
     * Get the name of the type of the value returned by the atomic start
     * function.
     *
     * @return Name of the type of the value returned by the atomic start
     *         function.
     */
    public String getTypename() {
        return typename;
    }

    /**
     * Get the name of the atomic start function to call at the start of each
     * atomic block.
     *
     * @return Name of the atomic start function.
     */
    public String getStartFunctionName() {
        return startFunctionName;
    }

    /**
     * Get the name of the atomic end function to call at the end of each
     * atomic block.
     *
     * @return Name of the atomic end function.
     */
    public String getEndFunctionName() {
        return endFunctionName;
    }
}
