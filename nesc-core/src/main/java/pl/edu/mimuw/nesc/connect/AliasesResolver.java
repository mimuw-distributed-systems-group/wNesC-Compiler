package pl.edu.mimuw.nesc.connect;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.gen.ComponentRef;
import pl.edu.mimuw.nesc.ast.gen.ComponentsUses;
import pl.edu.mimuw.nesc.ast.gen.ConfigurationImpl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.astutil.AliasesCollector;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Resolver of internal components names. It translates the internal names to
 * the global one.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class AliasesResolver {
    /**
     * Mapping of aliases to real names of components.
     */
    private final ImmutableMap<String, String> namesMap;

    /**
     * <p>Initialize this aliases resolver with information about components
     * from given configuration implementation.</p>
     */
    AliasesResolver(ConfigurationImpl configurationImpl) {
        checkNotNull(configurationImpl, "configuration implementation AST node cannot be null");
        this.namesMap = new AliasesCollector(configurationImpl).collect();
    }

    /**
     * <p>Get the global name of the component whose internal name is the one
     * given in parameter.</p>
     *
     * @param name Internal name of the component in the configuration.
     * @return Real name of the component.
     * @throws NullPointerException Name is null.
     * @throws IllegalArgumentException Name is an empty string or there is no
     *                                  component with given internal name.
     */
    public String resolve(String name) {
        checkNotNull(name, "internal component name cannot be null");
        checkArgument(!name.isEmpty(), "internal component name cannot be an empty string");

        final Optional<String> globalName = Optional.fromNullable(namesMap.get(name));
        checkArgument(globalName.isPresent(), "a component with internal name '%s' does not exist", name);

        return globalName.get();
    }
}
