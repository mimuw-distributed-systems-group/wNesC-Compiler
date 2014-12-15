package pl.edu.mimuw.nesc.names.mangling;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Skeletal implementation of the name mangling interface. It helps providing
 * support for forbidden names.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class AbstractNameMangler implements NameMangler {
    /**
     * Set that contains all names that are currently forbidden.
     */
    protected final Set<String> forbiddenNames = new HashSet<>();

    /**
     * Initializes this mangler not to contain any forbidden names.
     */
    protected AbstractNameMangler() {
    }

    /**
     * Initializes this mangler to forbid names in the given collection.
     *
     * @param names Names that will be forbidden after initialization of this
     *              mangler.
     * @throws NullPointerException The argument is <code>null</code>.
     * @throws IllegalArgumentException One of the names from the collection is
     *                                  <code>null</code> or empty string.
     */
    protected AbstractNameMangler(Collection<String> names) {
        addForbiddenNames(names);
    }

    @Override
    public boolean addForbiddenName(String name) {
        checkName(name);
        return forbiddenNames.add(name);
    }

    @Override
    public void addForbiddenNames(Collection<String> names) {
        checkNotNull(names, "the collection of names cannot be null");
        for (String name : names) {
            checkArgument(name != null, "one of the names in the collection is null");
            checkArgument(!name.isEmpty(), "one of the names in the collection is an empty string");
        }

        forbiddenNames.addAll(names);
    }

    protected void checkName(String name) {
        checkNotNull(name, "name cannot be null");
        checkArgument(!name.isEmpty(), "name cannot be an empty string");
    }
}
