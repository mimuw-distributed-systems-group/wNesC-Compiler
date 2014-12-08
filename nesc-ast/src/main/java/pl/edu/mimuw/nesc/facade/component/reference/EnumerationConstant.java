package pl.edu.mimuw.nesc.facade.component.reference;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>An enumeration constant from the specification of a component:</p>
 *
 * <pre>
 *     module IO
 *     {
 *         &hellip;
 *         enum { READ = 0, WRITE = 1, READWRITE = 2 }
 *         &hellip;
 *     }
 *     implementation
 *     {
 *         &hellip;
 *     }
 * </pre>
 *
 * <p><code>READ</code>, <code>WRITE</code> and <code>READWRITE</code> objects
 * will be represented by <code>EnumerationConstant</code> objects if the
 * <code>IO</code> is referred in a configuration.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class EnumerationConstant {
    /**
     * Unmangled and original name of the constant.
     */
    private final String name;

    /**
     * Mangled name of the constant.
     */
    private final String uniqueName;

    EnumerationConstant(String name, String uniqueName) {
        checkNotNull(name, "name cannot be null");
        checkNotNull(uniqueName, "unique name cannot be null");
        checkArgument(!name.isEmpty(), "name cannot be an empty string");
        checkArgument(!uniqueName.isEmpty(), "unique name cannot be an empty string");

        this.name = name;
        this.uniqueName = uniqueName;
    }

    /**
     * <p>Get the name of this enumeration constant used in the NesC code
     * (before mangling).</p>
     *
     * @return Original name of this enumeration constant.
     */
    public String getName() {
        return name;
    }

    /**
     * <p>Get the unique name of this enumeration constant (after mangling).</p>
     *
     * @return Unique name of this enumeration constant.
     */
    public String getUniqueName() {
        return uniqueName;
    }
}
