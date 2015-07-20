package pl.edu.mimuw.nesc.codepartition;

import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.codepartition.context.PartitionContext;
import pl.edu.mimuw.nesc.common.AtomicSpecification;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Object for allocating functions that should be present in the common bank.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class CommonBankAllocator {
    /**
     * Atomic specification with names of functions necessary for the
     * allocation.
     */
    private final AtomicSpecification atomicSpecification;

    CommonBankAllocator(AtomicSpecification atomicSpec) {
        checkNotNull(atomicSpec, "atomic specification cannot be null");
        this.atomicSpecification = atomicSpec;
    }

    /**
     * Allocate functions to common bank that should be present in it.
     *
     * @param context Context with current partition.
     * @param functions Functions to allocate.
     * @return Functions that has not been allocated by this allocator.
     */
    ImmutableList<FunctionDecl> allocate(PartitionContext context,
            Iterable<FunctionDecl> functions) throws PartitionImpossibleException {
        final BankSchema bankSchema = context.getBankTable().getSchema();
        final ImmutableList.Builder<FunctionDecl> remainingFunsBuilder = ImmutableList.builder();

        for (FunctionDecl functionDecl : functions) {
            final int funSize = context.getFunctionSize(functionDecl);
            final String uniqueName = DeclaratorUtils.getUniqueName(functionDecl.getDeclarator()).get();
            final boolean isSpontaneous = functionDecl.getDeclaration() != null
                    && functionDecl.getDeclaration().getCallAssumptions().compareTo(
                    FunctionDeclaration.CallAssumptions.SPONTANEOUS) >= 0;
            final boolean fitsCommonBank = uniqueName.equals("main")
                    || uniqueName.equals(atomicSpecification.getStartFunctionName())
                    || uniqueName.equals(atomicSpecification.getEndFunctionName());
            final boolean isMarkedAsBanked = DeclaratorUtils.getIsBanked(functionDecl.getDeclarator());

            if ((isSpontaneous || fitsCommonBank) && !isMarkedAsBanked) {
                if (context.getBankTable().getFreeSpace(bankSchema.getCommonBankName()) >= funSize) {
                    context.assign(functionDecl, bankSchema.getCommonBankName());
                } else {
                    throw new PartitionImpossibleException("not enough space in the common bank for a spontaneous function");
                }
            } else {
                remainingFunsBuilder.add(functionDecl);
            }
        }

        return remainingFunsBuilder.build();
    }
}
