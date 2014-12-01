package pl.edu.mimuw.nesc.instantiation;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.gen.NescDecl;
import pl.edu.mimuw.nesc.ast.gen.Node;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class responsible for mangling names that belong to the component
 * namespace. The names obtained after mangling are unique with respect to
 * names of interfaces or components added to the builder.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ComponentNameMangler {
    /**
     * Separator used to combine the mangled name and the unique suffix.
     */
    private static final String SEPARATOR = "__";

    /**
     * Names of interfaces and components (either generic or not) used in
     * the program.
     */
    private final ImmutableSet<String> usedNames;

    /**
     * Map used to generate unique names for components.
     */
    private final Map<String, Integer> instantiationCounters = new HashMap<>();

    /**
     * Get the builder that will build a component name mangler.
     *
     * @return Newly created builder that will build a component name mangler.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Initialize this mangler with information provided by given builder.
     *
     * @param builder Builder with necessary information.
     */
    private ComponentNameMangler(Builder builder) {
        this.usedNames = builder.buildUsedNames();
    }

    /**
     * <p>Mangles the given component name to a unique name that differs from
     * all previously returned values and names of all components and interfaces
     * added to the builder of this mangler.</p>
     *
     * @param componentName Name of the component to mangle.
     * @return Name after mangling.
     * @throws NullPointerException Component name is null.
     * @throws IllegalArgumentException Component name is an empty string.
     */
    public String mangle(String componentName) {
        checkNotNull(componentName, "name of the component cannot be null");
        checkArgument(!componentName.isEmpty(), "name of the component cannot be an empty string");

        // Retrieve the number of this instantiation and update state
        final int number;
        if (instantiationCounters.containsKey(componentName)) {
            number = instantiationCounters.get(componentName);
        } else {
            number = 1;
        }
        instantiationCounters.put(componentName, number + 1);

        String uniqueName;
        int separatorsCount = 1;

        // Generate the unique name
        do {
            final StringBuilder nameBuilder = new StringBuilder();
            nameBuilder.append(componentName);
            for (int i = 0; i < separatorsCount; ++i) {
                nameBuilder.append(SEPARATOR);
            }
            nameBuilder.append(number);

            uniqueName = nameBuilder.toString();
            ++separatorsCount;
        } while(usedNames.contains(uniqueName));

        return uniqueName;
    }

    /**
     * Builder for a component mangler.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder {
        /**
         * Data needed to build a component name mangler.
         */
        private final List<NescDecl> nescDeclarations = new ArrayList<>();

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder() {
        }

        /**
         * <p>Add the nodes of interfaces and components that contain names that
         * are used and cannot be repeated. Other nodes from the given list are
         * ignored.</p>
         *
         * @param nodes List with interfaces and components nodes.
         * @return <code>this</code>
         */
        public Builder addNescDeclarations(List<? extends Node> nodes) {
            FluentIterable.from(nodes)
                    .filter(NescDecl.class)
                    .copyInto(nescDeclarations);
            return this;
        }

        public ComponentNameMangler build() {
            return new ComponentNameMangler(this);
        }

        private ImmutableSet<String> buildUsedNames() {
            final ImmutableSet.Builder<String> usedNamesBuilder = ImmutableSet.builder();

            for (NescDecl nescDecl : nescDeclarations) {
                usedNamesBuilder.add(nescDecl.getName().getName());
            }

            return usedNamesBuilder.build();
        }
    }
}
