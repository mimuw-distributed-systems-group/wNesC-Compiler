package pl.edu.mimuw.nesc.codepartition;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.PriorityQueue;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.codesize.CodeSizeEstimation;
import pl.edu.mimuw.nesc.common.AtomicSpecification;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.refsgraph.ReferencesGraph;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Code partitioner that uses the following algorithm:</p>
 * <ol>
 *     <li>all spontaneous functions not marked as banked are assigned to the
 *     common bank</li>
 *     <li>functions from set {<code>main</code>,
 *     <code>__nesc_atomic_start</code>, <code>__nesc_atomic_end</code>}
 *     not marked as banked are assigned to the common bank</li>
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

    /**
     * Common bank allocator used by this partitioner.
     */
    private final CommonBankAllocator commonBankAllocator;

    public SimpleCodePartitioner(BankSchema bankSchema, AtomicSpecification atomicSpec) {
        checkNotNull(bankSchema, "bank schema cannot be null");
        checkNotNull(atomicSpec, "atomic specification cannot be null");
        this.bankSchema = bankSchema;
        this.commonBankAllocator = new CommonBankAllocator(atomicSpec);
    }

    @Override
    public BankSchema getBankSchema() {
        return bankSchema;
    }

    @Override
    public BankTable partition(Iterable<FunctionDecl> functions, CodeSizeEstimation sizesEstimation,
            ReferencesGraph refsGraph) throws PartitionImpossibleException {
        checkNotNull(functions, "functions cannot be null");
        checkNotNull(sizesEstimation, "estimation of sizes of functions cannot be null");
        checkNotNull(refsGraph, "references graph cannot be null");

        final PartitionContext context = new PartitionContext(bankSchema, sizesEstimation.getFunctionsSizes());
        final PriorityQueue<BankedFunction> sortedFuns = assignSpontaneousFunctions(context, functions);
        assignRemainingFunctions(context, sortedFuns);

        return context.getBankTable();
    }

    private PriorityQueue<BankedFunction> assignSpontaneousFunctions(PartitionContext context,
                Iterable<FunctionDecl> functions) throws PartitionImpossibleException {
        final ImmutableList<FunctionDecl> remainingFuns = commonBankAllocator.allocate(context, functions);
        final PriorityQueue<BankedFunction> sortedFunctions = new PriorityQueue<>(Math.max(remainingFuns.size(), 1));

        for (FunctionDecl functionDecl : remainingFuns) {
            sortedFunctions.add(new BankedFunction(functionDecl, context.getFunctionSize(functionDecl)));
        }

        return sortedFunctions;
    }

    private void assignRemainingFunctions(PartitionContext context,
                PriorityQueue<BankedFunction> sortedFuns) throws PartitionImpossibleException {
        while (!sortedFuns.isEmpty()) {
            final BankedFunction unassignedFun = sortedFuns.remove();
            final Optional<String> targetBankName = context.getFloorBank(unassignedFun.size);

            if (!targetBankName.isPresent()) {
                throw new PartitionImpossibleException("not enough space for function '"
                        + DeclaratorUtils.getUniqueName(unassignedFun.functionDecl.getDeclarator()).get()
                        + "'");
            }

            context.assign(unassignedFun.functionDecl, targetBankName.get());
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

}
