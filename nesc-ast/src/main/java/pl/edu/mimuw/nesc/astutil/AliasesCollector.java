package pl.edu.mimuw.nesc.astutil;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import pl.edu.mimuw.nesc.ast.gen.ComponentRef;
import pl.edu.mimuw.nesc.ast.gen.ComponentsUses;
import pl.edu.mimuw.nesc.ast.gen.ConfigurationImpl;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class responsible for collecting aliases and names of components inside
 * a configuration implementation and mapping them to components names.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class AliasesCollector {
    /**
     * Implementation of the configuration to collect aliases and names of
     * components from.
     */
    private final ConfigurationImpl configurationImpl;

    /**
     * Initializes this collector to collect components names and aliased from
     * the given configuration implementation.
     *
     * @param impl Implementation of the configuration to collect the aliases
     *             from.
     */
    public AliasesCollector(ConfigurationImpl impl) {
        checkNotNull(impl, "implementation cannot be null");
        this.configurationImpl = impl;
    }

    /**
     * Collects names and aliases of components in the configuration
     * implementation given at construction and returns them in an immutable
     * map.
     *
     * @return Immutable map with keys that are aliases or names of referred
     *         components (if no alias is associated with a component reference)
     *         and values are names of components.
     */
    public ImmutableMap<String, String> collect() {
        final ImmutableMap.Builder<String, String> namesMapBuilder = ImmutableMap.builder();
        final FluentIterable<ComponentsUses> filteredDeclarations =
                FluentIterable.from(configurationImpl.getDeclarations())
                        .filter(ComponentsUses.class);

        for (ComponentsUses componentsUses : filteredDeclarations) {
            for (ComponentRef componentRef : componentsUses.getComponents()) {
                final String alias = componentRef.getAlias().isPresent()
                        ? componentRef.getAlias().get().getName()
                        : componentRef.getName().getName();
                final String name = componentRef.getName().getName();

                namesMapBuilder.put(alias, name);
            }
        }

        return namesMapBuilder.build();
    }
}
