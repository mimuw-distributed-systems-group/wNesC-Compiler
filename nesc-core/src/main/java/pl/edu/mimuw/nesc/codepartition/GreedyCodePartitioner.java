package pl.edu.mimuw.nesc.codepartition;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.codesize.CodeSizeEstimation;
import pl.edu.mimuw.nesc.common.AtomicSpecification;
import pl.edu.mimuw.nesc.refsgraph.EntityNode;
import pl.edu.mimuw.nesc.refsgraph.Reference;
import pl.edu.mimuw.nesc.refsgraph.ReferencesGraph;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Code partitioner that makes use of local characteristics to provide the
 * partition of functions.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class GreedyCodePartitioner implements CodePartitioner {
    /**
     * Bank schema assumed by this partitioner.
     */
    private final BankSchema bankSchema;

    /**
     * Common bank allocator used by this code partitioner.
     */
    private final CommonBankAllocator commonBankAllocator;

    /**
     * PreValue used by this partitioner.
     */
    private final double preValue;

    public GreedyCodePartitioner(BankSchema bankSchema, AtomicSpecification atomicSpec,
            double preValue) {
        checkNotNull(bankSchema, "bank schema cannot be null");
        checkNotNull(atomicSpec, "atomic specification cannot be null");
        checkArgument(preValue > 0.0, "the pre-value must be positive");
        this.bankSchema = bankSchema;
        this.commonBankAllocator = new CommonBankAllocator(atomicSpec);
        this.preValue = preValue;
    }

    @Override
    public BankSchema getBankSchema() {
        return bankSchema;
    }

    @Override
    public BankTable partition(Iterable<FunctionDecl> functions, CodeSizeEstimation sizesEstimation,
            ReferencesGraph refsGraph) throws PartitionImpossibleException {
        checkNotNull(functions, "functions cannot be null");
        checkNotNull(sizesEstimation, "sizes estimation cannot be null");
        checkNotNull(refsGraph, "references graph cannot be null");

        final GreedyPartitionContext context = new GreedyPartitionContext(functions,
                sizesEstimation.getFunctionsSizes());
        computeCostSavings(context, refsGraph);
        commonBankAllocator.allocate(context, functions);
        assignFunctions(context);

        return context.getBankTable();
    }

    private void computeCostSavings(GreedyPartitionContext context, ReferencesGraph refsGraph) {
        for (String funUniqueName : context.unassignedFunctions.keySet()) {
            final Optional<ImmutableSet<String>> optConnectedFunctionsSet =
                    computeConnectedFunctionsSet(funUniqueName, refsGraph);
            if (!optConnectedFunctionsSet.isPresent()) {
                continue;
            }
            final ImmutableSet<String> connectedFunctionsSet = optConnectedFunctionsSet.get();

            addCostsSavings(context, connectedFunctionsSet);
            addCommonBankGain(context, funUniqueName, connectedFunctionsSet);
        }
    }

    private Optional<ImmutableSet<String>> computeConnectedFunctionsSet(String funUniqueName,
                ReferencesGraph refsGraph) {
        final ImmutableSet.Builder<String> uniqueNamesBuilder = ImmutableSet.builder();

        for (Reference reference : refsGraph.getOrdinaryIds().get(funUniqueName).getPredecessors()) {
            if (reference.getReferencingNode().getKind() != EntityNode.Kind.FUNCTION
                    || reference.getType() != Reference.Type.CALL) {
                return Optional.absent();
            }
            uniqueNamesBuilder.add(reference.getReferencingNode().getUniqueName());
        }

        uniqueNamesBuilder.add(funUniqueName);
        return Optional.of(uniqueNamesBuilder.build());
    }

    private void addCostsSavings(GreedyPartitionContext context, ImmutableSet<String> connectedFunctionsSet) {
        final double costSaving = preValue / (double) (connectedFunctionsSet.size() - 1);

        // Add cost savings to the graph
        for (String funUniqueName1 : connectedFunctionsSet) {
            for (String funUniqueName2 : connectedFunctionsSet) {
                final Optional<Double> saving1 = Optional.fromNullable(
                        context.costSavingsGraph.get(funUniqueName1).get(funUniqueName2));
                final Optional<Double> saving2 = Optional.fromNullable(
                        context.costSavingsGraph.get(funUniqueName2).get(funUniqueName1));
                final double newSaving1 = saving1.isPresent()
                        ? saving1.get() + costSaving
                        : costSaving;
                final double newSaving2 = saving2.isPresent()
                        ? saving2.get() + costSaving
                        : costSaving;

                context.costSavingsGraph.get(funUniqueName1).put(funUniqueName2, newSaving1);
                context.costSavingsGraph.get(funUniqueName2).put(funUniqueName1, newSaving2);
            }
        }
    }

    private void addCommonBankGain(GreedyPartitionContext context, String funUniqueName,
                ImmutableSet<String> connectedFunctionsSet) {
        final Map<String, Double> commonBankGains = context.banksGains.get(
                bankSchema.getCommonBankName());
        final NavigableMap<Double, NavigableSet<BankedFunction>> sortedCommonBankGains =
                context.sortedBanksGains.get(bankSchema.getCommonBankName());
        final double commonBankGain = connectedFunctionsSet.size() - 1;

        commonBankGains.put(funUniqueName, commonBankGain);

        if (!sortedCommonBankGains.containsKey(commonBankGain)) {
            sortedCommonBankGains.put(commonBankGain, new TreeSet<BankedFunction>());
        }
        sortedCommonBankGains.get(commonBankGain).add(context.newBankedFunction(funUniqueName));
    }

    private void assignFunctions(GreedyPartitionContext context) throws PartitionImpossibleException {
        if (context.sortedUnassignedFunctions.isEmpty()) {
            return;
        }

        // At the beginning assign functions to the common bank
        String currentBank = bankSchema.getCommonBankName();

        // Assign remaining function maximizing the cost savings
        while (!context.sortedUnassignedFunctions.isEmpty()) {
            final NavigableMap<Double, NavigableSet<BankedFunction>> currentBankGains =
                    context.sortedBanksGains.get(currentBank);
            final BankedFunction currentFunction = !currentBankGains.isEmpty()
                    ? currentBankGains.lastEntry().getValue().first()
                    : context.sortedUnassignedFunctions.last();
            final int functionSize = context.getFunctionSize(
                    context.unassignedFunctions.get(currentFunction.funUniqueName));

            if (context.getBankTable().getFreeSpace(currentBank) < functionSize) {
                final Optional<String> newBank = context.getCeilingBank(functionSize);
                if (!newBank.isPresent()) {
                    throw new PartitionImpossibleException("not enough space for function '"
                            + currentFunction.funUniqueName + "'");
                }
                currentBank = newBank.get();
            }

            context.assign(context.unassignedFunctions.get(currentFunction.funUniqueName),
                    currentBank);
        }
    }

    /**
     * Partition context extended with functionality needed by the local code
     * partitioner.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class GreedyPartitionContext extends PartitionContext {
        /**
         * Functions that have not yet been assigned to banks.
         */
        private final Map<String, FunctionDecl> unassignedFunctions;

        /**
         * Set with unassigned functions sorted ascending in their size.
         */
        private final TreeSet<BankedFunction> sortedUnassignedFunctions;

        /**
         * Map that represents the cost savings graph which is an undirected
         * graph. Keys are unique names of functions.
         */
        private final ImmutableMap<String, Map<String, Double>> costSavingsGraph;

        /**
         * Map whose keys are names of banks. The value for each key specifies
         * the gain that can be achieved by allocating each function to the
         * bank.
         */
        private final ImmutableMap<String, Map<String, Double>> banksGains;

        /**
         * Map whose keys are names of banks. The value for each key is a map
         * from gains that can be achieved by allocating any function in the
         * mapped set to the bank.
         */
        private final ImmutableMap<String, NavigableMap<Double, NavigableSet<BankedFunction>>> sortedBanksGains;

        private GreedyPartitionContext(Iterable<FunctionDecl> functions,
                Map<String, Range<Integer>> functionsSizes) {
            super(bankSchema, functionsSizes);

            final PrivateBuilder builder = new PrivateBuilder(functions, functionsSizes);
            this.unassignedFunctions = builder.buildUnassignedFunctions();
            this.sortedUnassignedFunctions = builder.buildSortedUnassignedFunctions();
            this.costSavingsGraph = builder.buildCostSavingsGraph();
            this.banksGains = builder.buildBanksGains();
            this.sortedBanksGains = builder.buildSortedBanksGains();
        }

        @Override
        void assign(FunctionDecl function, String targetBankName) {
            super.assign(function, targetBankName);

            final String funUniqueName = DeclaratorUtils.getUniqueName(
                    function.getDeclarator()).get();

            if (unassignedFunctions.remove(funUniqueName) == null) {
                throw new RuntimeException("cannot remove function '" + funUniqueName
                        + "' from the map of unassigned functions");
            }

            if (!sortedUnassignedFunctions.remove(new BankedFunction(funUniqueName,
                    getFunctionSize(function)))) {
                throw new RuntimeException("cannot remove function '" + funUniqueName
                        + "' from the sorted set of unassigned functions");
            }

            removeFunctionFromGains(funUniqueName);

            if (!targetBankName.equals(bankSchema.getCommonBankName())) {
                increaseGains(funUniqueName, targetBankName);
            }
        }

        private void removeFunctionFromGains(String funUniqueName) {
            for (Map.Entry<String, Map<String, Double>> gainsEntry : banksGains.entrySet()) {
                final String bankName = gainsEntry.getKey();
                final Map<String, Double> gains = gainsEntry.getValue();

                if (gains.containsKey(funUniqueName)) {
                    final double gain = gains.get(funUniqueName);
                    gains.remove(funUniqueName);

                    final Map<Double, NavigableSet<BankedFunction>> sortedGains = sortedBanksGains.get(bankName);
                    final NavigableSet<BankedFunction> functionsForGain = sortedGains.get(gain);

                    if (!functionsForGain.remove(newBankedFunction(funUniqueName))) {
                        throw new RuntimeException("function '" + funUniqueName
                                + "' not present in sorted gains");
                    }

                    if (functionsForGain.isEmpty()) {
                        if (sortedGains.remove(gain) == null) {
                            throw new RuntimeException("cannot remove gain " + gain
                                    + " from the sorted map");
                        }
                    }
                }
            }
        }

        private void increaseGains(String funUniqueName, String targetBankName) {
            final Map<String, Double> savings = costSavingsGraph.get(funUniqueName);
            final Map<String, Double> bankGains = banksGains.get(targetBankName);
            final NavigableMap<Double, NavigableSet<BankedFunction>> sortedBankGains =
                    sortedBanksGains.get(targetBankName);

            for (Map.Entry<String, Double> savingEntry : savings.entrySet()) {
                final String savingFunUniqueName = savingEntry.getKey();

                if (unassignedFunctions.containsKey(savingFunUniqueName)) {
                    final double oldGain;

                    if (bankGains.containsKey(savingFunUniqueName)) {
                        oldGain = bankGains.get(savingFunUniqueName);
                        final NavigableSet<BankedFunction> functionsForOldGain =
                                sortedBankGains.get(oldGain);
                        if (!functionsForOldGain.remove(newBankedFunction(savingFunUniqueName))) {
                            throw new RuntimeException("cannot function '" + savingFunUniqueName
                                    + "' from its old gain key");
                        }
                        if (functionsForOldGain.isEmpty()) {
                            if (sortedBankGains.remove(oldGain) == null) {
                                throw new RuntimeException("cannot remove mapping for empty gain '"
                                        + oldGain + "'");
                            }
                        }
                    } else {
                        oldGain = 0.;
                    }

                    final double newGain = oldGain + savingEntry.getValue();
                    bankGains.put(savingFunUniqueName, newGain);

                    if (!sortedBankGains.containsKey(newGain)) {
                        sortedBankGains.put(newGain, new TreeSet<BankedFunction>());
                    }

                    sortedBankGains.get(newGain).add(newBankedFunction(savingFunUniqueName));
                }
            }
        }

        private BankedFunction newBankedFunction(String funUniqueName) {
            return new BankedFunction(funUniqueName, getFunctionSize(funUniqueName));
        }

        /**
         * Private builder for a local partition context.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private final class PrivateBuilder {
            private final Iterable<FunctionDecl> functions;
            private final Map<String, Range<Integer>> functionsSizes;

            private PrivateBuilder(Iterable<FunctionDecl> functions, Map<String, Range<Integer>> functionsSizes) {
                this.functions = functions;
                this.functionsSizes = functionsSizes;
            }

            private Map<String, FunctionDecl> buildUnassignedFunctions() {
                final Map<String, FunctionDecl> unassignedFunctions = new HashMap<>();
                for (FunctionDecl function : functions) {
                    final String uniqueName = DeclaratorUtils.getUniqueName(
                            function.getDeclarator()).get();
                    if (unassignedFunctions.containsKey(uniqueName)) {
                        throw new RuntimeException("more than one function with unique name '"
                                + uniqueName + "'");
                    }
                    unassignedFunctions.put(uniqueName, function);
                }
                return unassignedFunctions;
            }

            private TreeSet<BankedFunction> buildSortedUnassignedFunctions() {
                final TreeSet<BankedFunction> sortedUnassignedFunctions = new TreeSet<>();
                for (FunctionDecl function : functions) {
                    final String funUniqueName = DeclaratorUtils.getUniqueName(
                            function.getDeclarator()).get();
                    sortedUnassignedFunctions.add(new BankedFunction(funUniqueName,
                            functionsSizes.get(funUniqueName).upperEndpoint()));
                }
                return sortedUnassignedFunctions;
            }

            private ImmutableMap<String, Map<String, Double>> buildCostSavingsGraph() {
                final ImmutableMap.Builder<String, Map<String, Double>> costSavingsGraphBuilder =
                        ImmutableMap.builder();
                for (String functionName : functionsSizes.keySet()) {
                    costSavingsGraphBuilder.put(functionName, new HashMap<String, Double>());
                }
                return costSavingsGraphBuilder.build();
            }

            private ImmutableMap<String, Map<String, Double>> buildBanksGains() {
                final ImmutableMap.Builder<String, Map<String, Double>> banksGainsBuilder =
                        ImmutableMap.builder();
                for (String bankName : bankSchema.getBanksNames()) {
                    banksGainsBuilder.put(bankName, new HashMap<String, Double>());
                }
                return banksGainsBuilder.build();
            }

            private ImmutableMap<String, NavigableMap<Double, NavigableSet<BankedFunction>>> buildSortedBanksGains() {
                final ImmutableMap.Builder<String, NavigableMap<Double, NavigableSet<BankedFunction>>> sortedBanksGainsBuilder =
                        ImmutableMap.builder();
                for (String bankName : bankSchema.getBanksNames()) {
                    sortedBanksGainsBuilder.put(bankName, new TreeMap<Double, NavigableSet<BankedFunction>>());
                }
                return sortedBanksGainsBuilder.build();
            }
        }
    }

    /**
     * Helper class that combines unique name of a function with its size. The
     * natural ordering for its objects is the ascending ordering of sizes of
     * functions. If two functions have the same size, greater is the one with
     * greater unique name.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class BankedFunction implements Comparable<BankedFunction> {
        private final String funUniqueName;
        private final int size;

        private BankedFunction(String funUniqueName, int size) {
            checkNotNull(funUniqueName, "function unique name cannot be null");
            checkArgument(!funUniqueName.isEmpty(), "unique name of a function cannot be empty");
            checkArgument(size >= 0, "size of a function cannot be negative");
            this.funUniqueName = funUniqueName;
            this.size = size;
        }

        @Override
        public int compareTo(BankedFunction other) {
            checkNotNull(other, "the other object is null");
            final int sizeComparisonResult = Integer.compare(size, other.size);
            return sizeComparisonResult != 0
                    ? sizeComparisonResult
                    : funUniqueName.compareTo(other.funUniqueName);
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || getClass() != other.getClass()) {
                return false;
            }

            final BankedFunction otherBankedFunction = (BankedFunction) other;
            return size == otherBankedFunction.size
                    && funUniqueName.equals(otherBankedFunction.funUniqueName);
        }

        @Override
        public int hashCode() {
            return 7 * funUniqueName.hashCode() + size;
        }
    }
}
