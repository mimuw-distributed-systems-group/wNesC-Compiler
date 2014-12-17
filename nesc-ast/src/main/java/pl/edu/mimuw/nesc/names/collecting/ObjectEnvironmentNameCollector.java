package pl.edu.mimuw.nesc.names.collecting;

import com.google.common.collect.FluentIterable;
import java.util.Collection;
import java.util.Map;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.environment.Environment;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>A name collector that collects names of all objects in current scopes
 * of encountered environments.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ObjectEnvironmentNameCollector extends AbstractNameCollector<Environment> {
    @Override
    public void collect(Environment environment) {
        checkNotNull(environment, "the environment cannot be null");

        for (Map.Entry<String, ObjectDeclaration> objEntry : environment.getObjects().getAll()) {
            names.add(objEntry.getKey());
        }
    }

    @Override
    public void collect(Collection<?> objects) {
        checkCollection(objects);

        final Iterable<Environment> environments = FluentIterable.from(objects)
                .filter(Environment.class);

        for (Environment environment : environments) {
            collect(environment);
        }
    }
}
