package pl.edu.mimuw.nesc.basicreduce;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.gen.Configuration;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.common.SchedulerSpecification;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>An executor class responsible for coordination of doing the basic reduce
 * actions:</p>
 *
 * <ul>
 *     <li>reversing mangling of global entities or entities with @C()
 *     attribute</li>
 *     <li>transforming tasks into implementations of the task interface</li>
 *     <li>transforming atomic statements into statements that guarantee the
 *     atomicity</li>
 * </ul>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class BasicReduceExecutor {
    /**
     * Configurations that form a NesC application.
     */
    private final ImmutableList<Configuration> configurations;

    /**
     * List with other nodes of the application that aren't configurations.
     */
    private final ImmutableList<Node> otherNodes;

    /**
     * Name mangler for generating new names.
     */
    private final NameMangler mangler;

    /**
     * Map from unique names of entities that will be located in the global
     * scope to their original global names.
     */
    private final ImmutableMap<String, String> globalNames;

    /**
     * Specification of scheduler that will be used for running tasks.
     */
    private final SchedulerSpecification schedulerSpecification;

    /**
     * Get a new builder that will build a basic reduce executor.
     *
     * @return Newly created builder that will build a basic reduce executor.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Initialize this executor with information from the given builder.
     *
     * @param builder Builder with information necessary to initialize the
     *                object.
     */
    private BasicReduceExecutor(Builder builder) {
        this.globalNames = builder.globalNamesBuilder.build();
        this.configurations = builder.configurationsBuilder.build();
        this.otherNodes = builder.otherNodesBuilder.build();
        this.mangler = builder.mangler;
        this.schedulerSpecification = builder.schedulerSpecification;
    }

    /**
     * Performs the operation of basic reduce for the nodes that have been added
     * to the builder. This method shall be called exactly once after building
     * the executor.
     *
     * @see BasicReduceExecutor
     */
    public void reduce() {
        final BasicReduceVisitor reduceVisitor = BasicReduceVisitor.builder()
                .nameMangler(mangler)
                .putGlobalNames(globalNames)
                .build();

        // Traverse nodes other than configuration first
        for (Node node : otherNodes) {
            node.traverse(reduceVisitor, null);
        }

        // Finally traverse configurations
        for (Configuration configuration : configurations) {
            configuration.traverse(reduceVisitor, null);
        }
    }

    /**
     * Builder class for a basic reduce executor.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder {
        /**
         * Data needed to build a basic reduce executor.
         */
        private final ImmutableList.Builder<Configuration> configurationsBuilder = ImmutableList.builder();
        private final ImmutableList.Builder<Node> otherNodesBuilder = ImmutableList.builder();
        private final ImmutableMap.Builder<String, String> globalNamesBuilder = ImmutableMap.builder();
        private NameMangler mangler;
        private SchedulerSpecification schedulerSpecification;

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder() {
        }

        /**
         * Adds nodes from the given collection for the basic reduce operation.
         *
         * @param nodes Collection of nodes to add.
         * @return <code>this</code>
         */
        public Builder addNodes(Collection<? extends Node> nodes) {
            for (Node node : nodes) {
                if (node instanceof Configuration) {
                    configurationsBuilder.add((Configuration) node);
                } else {
                    checkArgument(node != null, "one of the nodes in the collection is null");
                    otherNodesBuilder.add(node);
                }
            }

            return this;
        }

        /**
         * Set the name mangler that will be used for generating new names. It
         * shall be the same object that has generated unique names in added
         * nodes.
         *
         * @param nameMangler Name mangler to set.
         * @return <code>this</code>
         */
        public Builder nameMangler(NameMangler nameMangler) {
            this.mangler = nameMangler;
            return this;
        }

        /**
         * Store all key-value mappings from the given map as mapping from
         * unique names of an entities that will be located in the global scope
         * to their original global names. If a mapping from the same unique
         * name is added multiple times, then the basic reduce executor will
         * not be built successfully.
         *
         * @param globalNames Mappings to add.
         * @return <code>this</code>
         */
        public Builder putGlobalNames(Map<String, String> globalNames) {
            this.globalNamesBuilder.putAll(globalNames);
            return this;
        }

        /**
         * Set the specification of scheduler to use for tasks.
         *
         * @param schedulerSpec Specification of the scheduler to set.
         * @return <code>this</code>
         */
        public Builder schedulerSpecification(SchedulerSpecification schedulerSpec) {
            this.schedulerSpecification = schedulerSpec;
            return this;
        }

        private void validate() {
            checkState(mangler != null, "name mangler has not been set or set to null");
            checkState(schedulerSpecification != null, "the scheduler specification has not been set or is set to null");
        }

        public BasicReduceExecutor build() {
            validate();
            return new BasicReduceExecutor(this);
        }
    }
}
