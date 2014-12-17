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
import pl.edu.mimuw.nesc.ast.gen.Declaration;

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
     * <p>Get a builder that will build an aliases resolver.</p>
     *
     * @return Newly created builder that will build an aliases resolver.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * <p>Initialize this aliases resolver with information from given builder.
     * </p>
     *
     * @param builder Builder with necessary information.
     */
    private AliasesResolver(Builder builder) {
        this.namesMap = builder.buildNamesMap();
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

    /**
     * Builder for an aliases resolver.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    static final class Builder {
        /**
         * Data needed to build an aliases resolver.
         */
        private final List<ComponentRef> referredComponents = new ArrayList<>();

        /**
         * Private constructor to limits its accessibility.
         */
        private Builder() {
        }

        /**
         * <p>Add all components references contained in the given list of
         * declarations. All declarations other than <code>ComponentsUses</code>
         * are ignored.</p>
         *
         * @param declarations Declarations (possibly with
         *                     <code>ComponentsUses</code> to add).
         * @return <code>this</code>
         */
        public Builder addImplementationDeclarations(List<? extends Declaration> declarations) {
            final FluentIterable<ComponentsUses> componentsConstructs = FluentIterable.from(declarations)
                    .filter(ComponentsUses.class);
            for (ComponentsUses usedComponents : componentsConstructs) {
                referredComponents.addAll(usedComponents.getComponents());
            }

            return this;
        }

        private void validate() {
            // Check if the names of aliases are unique

            final Set<String> usedNames = new HashSet<>();

            for (ComponentRef componentRef : referredComponents) {
                final String name = componentRef.getAlias().isPresent()
                        ? componentRef.getAlias().get().getName()
                        : componentRef.getName().getName();

                if (!usedNames.add(name)) {
                    throw new IllegalStateException("added component references do not have unique names");
                }
            }
        }

        public AliasesResolver build() {
            validate();
            return new AliasesResolver(this);
        }

        private ImmutableMap<String, String> buildNamesMap() {
            final ImmutableMap.Builder<String, String> namesMapBuilder = ImmutableMap.builder();

            for (ComponentRef componentRef : referredComponents) {
                final String alias = componentRef.getAlias().isPresent()
                        ? componentRef.getAlias().get().getName()
                        : componentRef.getName().getName();
                final String name = componentRef.getName().getName();

                namesMapBuilder.put(alias, name);
            }

            return namesMapBuilder.build();
        }
    }
}
