package pl.edu.mimuw.nesc.names.mangling;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Name mangler that mangles names by counting usages of names and appending
 * the current value of the counter for a name to the mangled name.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class CountingNameMangler extends AbstractNameMangler {
    /**
     * Separator used to combine the mangled name and the unique suffix.
     */
    private static final String SEPARATOR = "__";

    /**
     * Map used to generate unique names.
     */
    private final Map<String, Integer> counters = new HashMap<>();

    /**
     * Initializes this name mangler to forbid no names.
     */
    public CountingNameMangler() {
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
    public CountingNameMangler(Collection<String> forbiddenNames) {
        super(forbiddenNames);  // throws if forbidden names are invalid
    }

    @Override
    public String mangle(String name) {
        checkName(name);

        // Retrieve the number of this instantiation and update state
        final int number = counters.containsKey(name)
                ? counters.get(name)
                : 1;
        counters.put(name, number + 1);

        String uniqueName;
        int separatorsCount = 1;

        // Generate the unique name
        do {
            final StringBuilder nameBuilder = new StringBuilder();
            nameBuilder.append(name);
            for (int i = 0; i < separatorsCount; ++i) {
                nameBuilder.append(SEPARATOR);
            }
            nameBuilder.append(number);

            uniqueName = nameBuilder.toString();
            ++separatorsCount;
        } while(forbiddenNames.contains(uniqueName));

        return uniqueName;
    }

    @Override
    public String remangle(String mangledName) {
        throw new UnsupportedOperationException();
    }
}
