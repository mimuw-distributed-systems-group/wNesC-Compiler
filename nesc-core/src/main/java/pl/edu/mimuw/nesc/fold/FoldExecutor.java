package pl.edu.mimuw.nesc.fold;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import pl.edu.mimuw.nesc.ast.gen.Node;

/**
 * <p>Class responsible for computing values of constant functions:
 * <code>unique</code>, <code>uniqueN</code> and <code>uniqueCount</code>.
 * Evaluated values are stored in field
 * {@link pl.edu.mimuw.nesc.ast.gen.ConstantFunctionCall#value} of AST nodes
 * that represent calls to these functions.</p>
 * <p>If an AST node appears multiple times in nodes, then the value for it is
 * computed only once. Values are computed only for AST nodes with the value
 * of the mentioned field set to <code>null</code></p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class FoldExecutor {
    /**
     * List with all nodes that may contain calls to constant functions.
     */
    private final ImmutableList<Node> nodes;

    /**
     * <p>Get a builder that will build a fold executor object.</p>
     *
     * @return Newly created builder that will build a fold executor object.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * <p>Initialize this object with information from the given builder.</p>
     *
     * @param builder Builder with all necessary information.
     */
    private FoldExecutor(Builder builder) {
        this.nodes = builder.buildNodesList();
    }

    /**
     * <p>Perform the evaluation of calls to NesC constant functions. Values are
     * computed for nodes added previously to the builder. They are saved in
     * field {@link pl.edu.mimuw.nesc.ast.gen.ConstantFunctionCall#value}.
     * Moreover, {@link pl.edu.mimuw.nesc.ast.gen.ConstantFunctionCall#identifier}
     * is set to a proper string.</p>
     *
     * @return Map with counts of numbers allocated for each identifier used as
     *         the first argument for a NesC constant function.
     */
    public ImmutableMap<String, Long> fold() {
        // Evaluate 'unique' and 'uniqueN'
        final ValuesProcessor valuesProcessor = new ValuesProcessor();
        for (Node node : nodes) {
            node.traverse(valuesProcessor, null);
        }

        // Evaluate 'uniqueCount'
        final ImmutableMap<String, Long> counters = valuesProcessor.getCounters();
        final CountsProcessor countsProcessor = new CountsProcessor(counters);
        for (Node node : nodes) {
            node.traverse(countsProcessor, null);
        }

        return counters;
    }

    /**
     * <p>Builder for a fold executor.</p>
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder {
        /**
         * Data needed to build a fold executor.
         */
        private final ImmutableList.Builder<Node> nodesListBuilder = ImmutableList.builder();

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder() {
        }

        /**
         * <p>Add the given node to perform evaluation of constant functions
         * used in it by the built fold executor.</p>
         *
         * @param nodeToAdd Node to add.
         * @return <code>this</code>
         */
        public Builder addNode(Node nodeToAdd) {
            nodesListBuilder.add(nodeToAdd);
            return this;
        }

        /**
         * <p>Add all nodes from the given collection for evaluation of constant
         * functions that appear in them by the built fold executor.</p>
         *
         * @param nodes Nodes to add.
         * @return <code>this</code>
         */
        public Builder addNodes(Collection<? extends Node> nodes) {
            nodesListBuilder.addAll(nodes);
            return this;
        }

        public FoldExecutor build() {
            return new FoldExecutor(this);
        }

        private ImmutableList<Node> buildNodesList() {
            return nodesListBuilder.build();
        }
    }
}
