package pl.edu.mimuw.nesc.codesize;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Object with information about estimation of functions sizes.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class CodeSizeEstimation {
    /**
     * Set with functions that are inlined in this estimation.
     */
    private final ImmutableSet<String> inlineFunctions;

    /**
     * Map with the estimation. Keys are names of functions and values are
     * their estimated sizes.
     */
    private final ImmutableMap<String, Range<Integer>> functionsSizes;

    /**
     * Get a builder that will create a new code size estimation.
     *
     * @return Newly created instance of a code size estimation builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    private CodeSizeEstimation(ImmutableSet<String> inlineFunctions,
                ImmutableMap<String, Range<Integer>> functionsSizes) {
        this.inlineFunctions = inlineFunctions;
        this.functionsSizes = functionsSizes;
    }

    /**
     * Get the set with functions that are to be inlined and not included in the
     * final program.
     *
     * @return Set with unique names of inline functions.
     */
    public ImmutableSet<String> getInlineFunctions() {
        return inlineFunctions;
    }

    /**
     * Get the estimation of functions sizes. Functions with names returned by
     * {@link CodeSizeEstimation#getInlineFunctions} are not present in the map
     * as they are intended to by completely inlined and not appear in the final
     * program.
     *
     * @return Map with functions names as keys and estimations of their sizes
     *         as values. If a mapping <code>key -> [V1, V2]</code> is present,
     *         it means that the size of function named <code>key</code> has
     *         size at least <code>V1</code> bytes and at most <code>V2</code>
     *         bytes.
     */
    public ImmutableMap<String, Range<Integer>> getFunctionsSizes() {
        return functionsSizes;
    }

    /**
     * Object that builds a code size estimation.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder {
        /**
         * Data necessary for building a code size estimation.
         */
        private final ImmutableSet.Builder<String> inlineFunctionsBuilder = ImmutableSet.builder();
        private final Map<String, Range<Integer>> functionsSizes = new HashMap<>();

        /**
         * Private constructor to limit its accessibility.
         */
        private Builder() {
        }

        /**
         * Add the given function as an inline function.
         *
         * @param funUniqueName Unique name of function to add.
         * @return <code>this</code>
         */
        public Builder addInlineFunction(String funUniqueName) {
            checkNotNull(funUniqueName, "unique name of the function cannot be null");
            checkArgument(!funUniqueName.isEmpty(), "unique name of the function cannot be an empty string");

            this.inlineFunctionsBuilder.add(funUniqueName);
            return this;
        }

        /**
         * Add functions that are to be inlined.
         *
         * @param inlineFunctions Iterable with unique names of inline
         *                        functions.
         * @return <code>this</code>
         */
        public Builder addAllInlineFunctions(Iterable<String> inlineFunctions) {
            checkNotNull(inlineFunctions, "inline functions iterable cannot be null");
            for (String funUniqueName : inlineFunctions) {
                addInlineFunction(funUniqueName);
            }
            return this;
        }

        /**
         * Add estimation of function with given name.
         *
         * @param funUniqueName Unique name of the function whose size
         *                      estimation is added.
         * @param estimation Estimation of the size of the function.
         * @return <code>this</code>
         */
        public Builder putFunctionSize(String funUniqueName, Range<Integer> estimation) {
            checkNotNull(funUniqueName, "unique name of the function cannot be null");
            checkNotNull(estimation, "estimation cannot be null");
            checkArgument(!funUniqueName.isEmpty(), "unique name of the function cannot be an empty string");
            checkState(!functionsSizes.containsKey(funUniqueName), "estimation for function '%s' has been already added",
                    funUniqueName);

            this.functionsSizes.put(funUniqueName, estimation);
            return this;
        }

        /**
         * Add all entries with information about the estimation from the given
         * map.
         *
         * @param sizes Map with estimation information to add.
         * @return <code>this</code>
         */
        public Builder putAllFunctionsSizes(Map<String, Range<Integer>> sizes) {
            checkNotNull(sizes, "sizes cannot be null");
            for (Map.Entry<String, Range<Integer>> estimationEntry : sizes.entrySet()) {
                putFunctionSize(estimationEntry.getKey(), estimationEntry.getValue());
            }
            return this;
        }

        public CodeSizeEstimation build() {
            return new CodeSizeEstimation(inlineFunctionsBuilder.build(),
                    ImmutableMap.copyOf(functionsSizes));
        }
    }
}
