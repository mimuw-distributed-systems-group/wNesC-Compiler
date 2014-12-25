package pl.edu.mimuw.nesc.names.mangling;

import java.util.Collection;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * <p>A name mangler that creates names by appending a unique suffix that
 * contains only standard 26 letters of the alphabet.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class AlphabeticNameMangler extends AbstractNameMangler {
    /**
     * A string used to separate the name that is mangled and the unique suffix
     * appended to it.
     */
    private static final String SEPARATOR = "__";

    /**
     * Unique suffix appended to mangled names. Each returned name contain
     * a different suffix.
     */
    private char[] uniqueSuffix = { 'a' };

    /**
     * Initializes this mangler to forbid no names in the initial state.
     */
    public AlphabeticNameMangler() {
    }

    /**
     * Initializes this name mangler by adding the names from the given argument
     * as forbidden.
     *
     * @param forbiddenNames Names that will be forbidden after construction.
     * @throws NullPointerException Given argument is <code>null</code>.
     * @throws IllegalArgumentException One of the forbidden names is
     *                                  <code>null</code> or an empty string.
     */
    public AlphabeticNameMangler(Collection<String> forbiddenNames) {
        super(forbiddenNames);  // throws if forbidden names are invalid
    }

    @Override
    public String mangle(String name) {
        checkName(name);

        String uniqueName;

        do {
            uniqueName = name + SEPARATOR + String.copyValueOf(uniqueSuffix);
            nextSuffix();
        } while(forbiddenNames.contains(uniqueName));

        return uniqueName;
    }

    @Override
    public String remangle(String mangledName) {
        checkName(mangledName);

        final int separatorIndex = mangledName.lastIndexOf(SEPARATOR);
        checkArgument(separatorIndex != -1, "name '%s' is not the result of mangling a name by this mangler",
                mangledName);

        return mangle(mangledName.substring(0, separatorIndex));
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
