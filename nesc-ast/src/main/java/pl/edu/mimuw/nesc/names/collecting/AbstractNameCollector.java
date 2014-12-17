package pl.edu.mimuw.nesc.names.collecting;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>A skeletal implementation of the name collector interface that provides
 * a set with currently collected names.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class AbstractNameCollector<T> implements NameCollector<T> {
    /**
     * Set with names gathered so far.
     */
    protected final Set<String> names = new HashSet<>();

    @Override
    public Set<String> get() {
        return names;
    }

    protected void checkCollection(Collection<?> objects) {
        checkNotNull(objects, "the collection cannot be null");
        for (Object object : objects) {
            checkArgument(object != null, "an object in the collection is null");
        }
    }
}
