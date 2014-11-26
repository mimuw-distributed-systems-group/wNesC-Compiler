package pl.edu.mimuw.nesc.analysis;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * <p>Class responsible for creating unique names for identifiers of the
 * following entities:</p>
 * <ul>
 *     <li>variables</li>
 *     <li>type definitions</li>
 *     <li>tags</li>
 *     <li>functions</li>
 *     <li>generic components instantiated by <code>new</code></li>
 * </ul>
 *
 * <p>It follows the Singleton design pattern.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class NameMangler {
    /**
     * The only instance of this class.
     */
    private static final NameMangler instance = new NameMangler();

    /**
     * Unique suffix appended to mangled names. Each returned name contain
     * a different suffix.
     */
    private char[] uniqueSuffix = { 'a' };

    /**
     * Get the only instance of this class.
     *
     * @return The only instance of this class.
     */
    public static NameMangler getInstance() {
        return instance;
    }

    /**
     * Private constructor for the Singleton pattern.
     */
    private NameMangler() {
    }

    /**
     * Mangle the name given by parameter. All strings returned by multiple
     * calls of this method are not empty and unique.
     *
     * @param unmangledName Name to mangle.
     * @return The mangled name for the given name.
     * @throws NullPointerException Given argument is null.
     * @throws IllegalArgumentException Given argument is an empty string.
     */
    public String mangle(String unmangledName) {
        checkNotNull(unmangledName, "unmangled name cannot be null");
        checkArgument(!unmangledName.isEmpty(), "unmangled name cannot be empty");

        final String result = format("%s__%s", unmangledName, String.copyValueOf(uniqueSuffix));
        nextSuffix();

        return result;
    }

    /**
     * Changes the content of <code>uniqueSuffix</code> array to a new unique
     * suffix.
     */
    private void nextSuffix() {
        int i = uniqueSuffix.length - 1;

        while (i >= 0 && uniqueSuffix[i] == 'z') {
            uniqueSuffix[i] = 'a';
            --i;
        }

        if (i == -1) {
            /* Increase the length of the suffix as all shorter suffices have
               been used. */
            final char[] oldArray = uniqueSuffix;
            uniqueSuffix = new char[oldArray.length + 1];
            uniqueSuffix[0] = 'a';
            System.arraycopy(oldArray, 0, uniqueSuffix, 1, oldArray.length);
        } else {
            // Advance the first character (from the end) not equal to 'z'
            ++uniqueSuffix[i];
        }
    }
}
