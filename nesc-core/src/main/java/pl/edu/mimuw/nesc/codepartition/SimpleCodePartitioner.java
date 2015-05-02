package pl.edu.mimuw.nesc.codepartition;

import com.google.common.base.Optional;
import com.google.common.collect.Range;
import java.util.HashSet;
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
 *     <li>all spontaneous functions are assigned to the common bank</li>
 *     <li>functions are sequenced from the biggest one to the smallest one and
 *     assigned in that order</li>
 *     <li>each function is assigned to the bank with the smallest amount of
 *     remaining free space that can hold it</li>
 * </ol>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class SimpleCodePartitioner implements CodePartitioner {
    /**
     * Bank schema assumed by this partitioner.
     */
    private final BankSchema bankSchema;

    public SimpleCodePartitioner(BankSchema bankSchema) {
        checkNotNull(bankSchema, "bank schema cannot be null");
        this.bankSchema = bankSchema;
    }

    @Override
    public BankSchema getBankSchema() {
        return bankSchema;
    }

    @Override
    public BankTable partition(Iterable<FunctionDecl> functions,
            Map<String, Range<Integer>> functionsSizes) throws PartitionImpossibleException {
        checkNotNull(functions, "functions cannot be null");
        checkNotNull(functionsSizes, "sizes of functions cannot be null");

        final PartitionContext context = new PartitionContext(functionsSizes);
        final PriorityQueue<BankedFunction> sortedFuns = assignSpontaneousFunctions(context, functions);
        assignRemainingFunctions(context, sortedFuns);

        return context.getBankTable();
    }

    private PriorityQueue<BankedFunction> assignSpontaneousFunctions(PartitionContext context,
                Iterable<FunctionDecl> functions) throws PartitionImpossibleException {
        final PriorityQueue<BankedFunction> sortedFunctions = new PriorityQueue<>();

        for (FunctionDecl functionDecl : functions) {
            final int funSize = context.getFunctionSize(functionDecl);

            if (functionDecl.getDeclaration() != null
                    && functionDecl.getDeclaration().getCallAssumptions().compareTo(FunctionDeclaration.CallAssumptions.SPONTANEOUS) >= 0) {
                if (context.bankTable.getFreeSpace(bankSchema.getCommonBankName()) >= funSize) {
                    context.assign(functionDecl, bankSchema.getCommonBankName());
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
                throw new PartitionImpossibleException("not enough space for function '"
                        + DeclaratorUtils.getUniqueName(unassignedFun.functionDecl.getDeclarator()).get()
                        + "'");
            }

            final String targetBankName = context.banksFreeSpaceMap.get(targetBankSize.get())
                    .iterator().next();
            context.assign(unassignedFun.functionDecl, targetBankName);
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
        private final BankTable bankTable;
        private final TreeMap<Integer, Set<String>> banksFreeSpaceMap;
        private final Map<String, Range<Integer>> functionsSizes;

        private PartitionContext(Map<String, Range<Integer>> functionsSizes) {
            checkNotNull(functionsSizes, "sizes of functions cannot be null");

            final PrivateBuilder builder = new PrivateBuilder();
            this.bankTable = new BankTable(bankSchema);
            this.banksFreeSpaceMap = builder.buildBanksFreeSpaceMap();
            this.functionsSizes = functionsSizes;
        }

        private int getFunctionSize(FunctionDecl functionDecl) {
            final String uniqueName = DeclaratorUtils.getUniqueName(functionDecl.getDeclarator()).get();
            return functionsSizes.get(uniqueName).upperEndpoint();
        }

        private void assign(FunctionDecl function, String bankName) {
            final int funSize = getFunctionSize(function);
            final int oldTargetBankFreeSpace = bankTable.getFreeSpace(bankName);
            if (funSize > oldTargetBankFreeSpace) {
                throw new IllegalStateException("assigning a function to a bank without sufficient space");
            }

            bankTable.allocate(bankName, function, funSize);
            final int newTargetBankFreeSpace = bankTable.getFreeSpace(bankName);

            if (funSize > 0) {
                final Set<String> banksMapSet = banksFreeSpaceMap.get(oldTargetBankFreeSpace);
                if (!banksMapSet.remove(bankName)) {
                    throw new IllegalStateException("inconsistent information about free space in bank " + bankName);
                }
                if (banksMapSet.isEmpty()) {
                    banksFreeSpaceMap.remove(oldTargetBankFreeSpace);
                }

                if (!banksFreeSpaceMap.containsKey(newTargetBankFreeSpace)) {
                    banksFreeSpaceMap.put(newTargetBankFreeSpace, new HashSet<String>());
                }
                banksFreeSpaceMap.get(newTargetBankFreeSpace).add(bankName);
            }
        }

        private BankTable getBankTable() {
            return bankTable;
        }

        /**
         * Helper class that builds elements of a partition context.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private final class PrivateBuilder {
            private TreeMap<Integer, Set<String>> buildBanksFreeSpaceMap() {
                final TreeMap<Integer, Set<String>> result = new TreeMap<>();
                for (String bankName : bankSchema.getBanksNames()) {
                    final int capacity = bankSchema.getBankCapacity(bankName);
                    if (!result.containsKey(capacity)) {
                        result.put(capacity, new HashSet<String>());
                    }
                    result.get(capacity).add(bankName);
                }
                return result;
            }
        }
    }
}
