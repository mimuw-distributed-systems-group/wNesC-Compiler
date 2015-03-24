package pl.edu.mimuw.nesc.codepartition;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Code partitioner that uses the following algorithm:</p>
 * <ol>
 *     <li>all spontaneous functions are assigned to the bank with index 0 (the
 *     common bank)</li>
 *     <li>functions are sequenced from the biggest one to the smallest one and
 *     assigned in that order</li>
 *     <li>each function is assigned to the bank with the smallest amount of
 *     remaining free space that can hold it</li>
 * </ol>
 *
 * <p>Banks model that is assumed by this partitioner is as follows. Every bank
 * has the same size. There is one common bank (it is considered the bank with
 * index 0).</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class SimpleCodePartitioner implements CodePartitioner {
    /**
     * Size of a single bank (in bytes).
     */
    private final int bankSize;

    /**
     * Count of banks.
     */
    private final int banksCount;

    public SimpleCodePartitioner(int bankSize, int banksCount) {
        checkArgument(bankSize > 0, "size of a bank must be positive");
        checkArgument(banksCount > 0, "count of banks must be positive");
        this.bankSize = bankSize;
        this.banksCount = banksCount;
    }

    @Override
    public ImmutableList<ImmutableSet<FunctionDecl>> partition(List<FunctionDecl> functions,
            Map<String, Range<Integer>> functionsSizes) throws PartitionImpossibleException {
        checkNotNull(functions, "functions cannot be null");
        checkNotNull(functionsSizes, "sizes of functions cannot be null");

        final PartitionContext context = new PartitionContext(functionsSizes);
        final PriorityQueue<BankedFunction> sortedFuns = assignSpontaneousFunctions(context, functions);
        assignRemainingFunctions(context, sortedFuns);

        return context.createResult();
    }

    private PriorityQueue<BankedFunction> assignSpontaneousFunctions(PartitionContext context,
                List<FunctionDecl> functions) throws PartitionImpossibleException {
        final PriorityQueue<BankedFunction> sortedFunctions = new PriorityQueue<>(functions.size());

        for (FunctionDecl functionDecl : functions) {
            final int funSize = context.getFunctionSize(functionDecl);

            if (functionDecl.getDeclaration() != null
                    && functionDecl.getDeclaration().getCallAssumptions().compareTo(FunctionDeclaration.CallAssumptions.SPONTANEOUS) >= 0) {
                if (context.banksFreeSpace[INDEX_COMMON_BANK] >= funSize) {
                    context.assign(functionDecl, INDEX_COMMON_BANK);
                } else {
                    throw new PartitionImpossibleException("not enough space in the common bank for a spontaneous function");
                }
            } else {
                sortedFunctions.add(new BankedFunction(functionDecl, funSize));
            }
        }

        return sortedFunctions;
    }

    private void assignRemainingFunctions(PartitionContext context,
                PriorityQueue<BankedFunction> sortedFuns) throws PartitionImpossibleException {
        while (!sortedFuns.isEmpty()) {
            final BankedFunction unassignedFun = sortedFuns.remove();
            final Optional<Integer> targetBankSize = Optional.fromNullable(
                    context.banksFreeSpaceMap.ceilingKey(unassignedFun.size));

            if (!targetBankSize.isPresent()) {
                throw new PartitionImpossibleException("not enough space for function "
                        + DeclaratorUtils.getUniqueName(unassignedFun.functionDecl.getDeclarator()).get());
            }

            final int targetBankIndex = context.banksFreeSpaceMap.get(targetBankSize.get())
                    .iterator().next();
            context.assign(unassignedFun.functionDecl, targetBankIndex);
        }
    }

    /**
     * Helper class that associates a function with its size. The natural
     * ordering is the descending ordering of function sizes.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class BankedFunction implements Comparable<BankedFunction> {
        private final FunctionDecl functionDecl;
        private final int size;

        private BankedFunction(FunctionDecl functionDecl, int size) {
            checkNotNull(functionDecl, "function cannot be null");
            checkArgument(size >= 0, "size cannot be negative");
            this.functionDecl = functionDecl;
            this.size = size;
        }

        @Override
        public int compareTo(BankedFunction otherBankedFun) {
            checkNotNull(otherBankedFun, "other function cannot be null");
            return Integer.compare(otherBankedFun.size, this.size);
        }
    }

    /**
     * Class that represents a context for a single partition operation.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class PartitionContext {
        private final List<Set<FunctionDecl>> assignment;
        private final TreeMap<Integer, Set<Integer>> banksFreeSpaceMap;
        private final Map<String, Range<Integer>> functionsSizes;
        private final int[] banksFreeSpace;

        private PartitionContext(Map<String, Range<Integer>> functionsSizes) {
            checkNotNull(functionsSizes, "sizes of functions cannot be null");

            final PrivateBuilder builder = new PrivateBuilder();
            this.assignment = builder.buildAssignment();
            this.banksFreeSpaceMap = builder.buildBanksFreeSpaceMap();
            this.functionsSizes = functionsSizes;
            this.banksFreeSpace = builder.buildBanksFreeSpace();
        }

        private int getFunctionSize(FunctionDecl functionDecl) {
            final String uniqueName = DeclaratorUtils.getUniqueName(functionDecl.getDeclarator()).get();
            return functionsSizes.get(uniqueName).upperEndpoint();
        }

        private void assign(FunctionDecl function, int bankIndex) {
            final int funSize = getFunctionSize(function);
            if (funSize > banksFreeSpace[bankIndex]) {
                throw new IllegalStateException("assigning a function to a bank without sufficient space");
            }

            assignment.get(bankIndex).add(function);

            if (funSize > 0) {
                final Set<Integer> banksMapSet = banksFreeSpaceMap.get(banksFreeSpace[bankIndex]);
                if (!banksMapSet.remove(bankIndex)) {
                    throw new IllegalStateException("inconsistent information about free space in bank " + bankIndex);
                }
                if (banksMapSet.isEmpty()) {
                    banksFreeSpaceMap.remove(banksFreeSpace[bankIndex]);
                }

                banksFreeSpace[bankIndex] -= funSize;

                if (!banksFreeSpaceMap.containsKey(banksFreeSpace[bankIndex])) {
                    banksFreeSpaceMap.put(banksFreeSpace[bankIndex], new HashSet<Integer>());
                }
                banksFreeSpaceMap.get(banksFreeSpace[bankIndex]).add(bankIndex);
            }
        }

        private ImmutableList<ImmutableSet<FunctionDecl>> createResult() {
            final ImmutableList.Builder<ImmutableSet<FunctionDecl>> banksBuilder =
                    ImmutableList.builder();
            for (Set<FunctionDecl> bank : assignment) {
                banksBuilder.add(ImmutableSet.copyOf(bank));
            }
            return banksBuilder.build();
        }

        /**
         * Helper class that builds elements of a partition context.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private final class PrivateBuilder {
            private List<Set<FunctionDecl>> buildAssignment() {
                final List<Set<FunctionDecl>> emptyAssignment = new ArrayList<>();
                for (int i = 0; i < SimpleCodePartitioner.this.banksCount; ++i) {
                    emptyAssignment.add(new HashSet<FunctionDecl>());
                }
                return emptyAssignment;
            }

            private TreeMap<Integer, Set<Integer>> buildBanksFreeSpaceMap() {
                final Set<Integer> banksIndices = new HashSet<>();
                for (int i = 0; i < banksCount; ++i) {
                    banksIndices.add(i);
                }
                final TreeMap<Integer, Set<Integer>> result = new TreeMap<>();
                result.put(bankSize, banksIndices);
                return result;
            }

            private int[] buildBanksFreeSpace() {
                final int[] freeSpace = new int[banksCount];
                for (int i = 0; i < banksCount; ++i) {
                    freeSpace[i] = bankSize;
                }
                return freeSpace;
            }
        }
    }
}
