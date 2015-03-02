package pl.edu.mimuw.nesc.optimization;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayDeque;
import java.util.Queue;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.ExceptionVisitor;
import pl.edu.mimuw.nesc.ast.gen.ExtensionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.refsgraph.EntityNode;
import pl.edu.mimuw.nesc.refsgraph.Reference;
import pl.edu.mimuw.nesc.refsgraph.ReferencesGraph;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Class responsible for raising flag
 *
 * {@link pl.edu.mimuw.nesc.ast.gen.FunctionDecl#isAtomic}
 *
 * for functions that are always executed atomically.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class AtomicOptimizer {
    /**
     * Candidates for functions that are atomic. Keys are functions names and
     * values are objects with information about functions.
     */
    private final ImmutableMap<String, FunctionData> candidates;

    /**
     * Graph of references between top-level entities.
     */
    private final ReferencesGraph refsGraph;

    /**
     * Initialize this optimizer to operate on functions in given declarations
     * using given graph of references.
     *
     * @param declarations Declarations with functions definitions.
     * @param refsGraph Graph of references between given declarations.
     */
    public AtomicOptimizer(ImmutableList<Declaration> declarations, ReferencesGraph refsGraph) {
        checkNotNull(declarations, "declarations cannot be null");
        checkNotNull(refsGraph, "references graph cannot be null");

        final PrivateBuilder builder = new RealBuilder(declarations, refsGraph);
        this.candidates = builder.buildCandidates();
        this.refsGraph = refsGraph;
    }

    /**
     * Determines which functions are atomic and raises <code>isAtomic</code>
     * flag in their AST nodes. This method shall be called exactly once.
     */
    public void optimize() {
        final Queue<FunctionData> candidatesQueue = new ArrayDeque<>();

        // Enqueue start functions
        for (FunctionData funData : candidates.values()) {
            if (funData.nonAtomicCallsCount == 0) {
                candidatesQueue.add(funData);
            }
        }

        // Determine atomic functions
        while (!candidatesQueue.isEmpty()) {
            final FunctionData funData = candidatesQueue.remove();
            funData.functionDecl.setIsAtomic(true);

            final EntityNode node = refsGraph.getOrdinaryIds().get(funData.uniqueName);

            // Update non-atomic calls counts in referenced functions
            for (Reference reference : node.getSuccessors()) {
                final EntityNode referencedNode = reference.getReferencedNode();

                if (referencedNode.getKind() == EntityNode.Kind.FUNCTION
                        && !reference.isInsideAtomic()
                        && candidates.containsKey(referencedNode.getUniqueName())) {
                    checkState(reference.getType() == Reference.Type.CALL,
                            "candidate referenced not by a call");
                    final FunctionData successorFunData = candidates.get(referencedNode.getUniqueName());
                    successorFunData.nonAtomicCallsCount -= 1;

                    if (successorFunData.nonAtomicCallsCount == 0) {
                        candidatesQueue.add(successorFunData);
                    } else if (successorFunData.nonAtomicCallsCount < 0) {
                        throw new IllegalStateException("candidate '" + referencedNode.getUniqueName()
                                + "' with negative non-atomic calls " + successorFunData.nonAtomicCallsCount);
                    }
                }
            }
        }
    }

    /**
     * Interface for building elements of an atomic optimizer.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private interface PrivateBuilder {
        ImmutableMap<String, FunctionData> buildCandidates();
    }

    /**
     * Class that builds particular elements of an atomic optimizer.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class RealBuilder extends ExceptionVisitor<Void, Void> implements PrivateBuilder {
        private final ImmutableMap.Builder<String, FunctionData> candidatesBuilder = ImmutableMap.builder();
        private final ImmutableList<Declaration> declarations;
        private final ReferencesGraph refsGraph;
        private boolean built = false;

        private RealBuilder(ImmutableList<Declaration> declarations, ReferencesGraph refsGraph) {
            this.declarations = declarations;
            this.refsGraph = refsGraph;
        }

        @Override
        public ImmutableMap<String, FunctionData> buildCandidates() {
            visitDeclarations();
            return candidatesBuilder.build();
        }

        private void visitDeclarations() {
            if (built) {
                return;
            }

            built = true;

            for (Declaration declaration : declarations) {
                declaration.accept(this, null);
            }
        }

        @Override
        public Void visitFunctionDecl(FunctionDecl declaration, Void arg) {
            // Return if call assumptions aren't too strong
            if (!checkCallAssumptions(declaration)) {
                return null;
            }

            /* Check if all references to the function are calls and
               simultaneously count non-atomic calls. */
            final String uniqueName = DeclaratorUtils.getUniqueName(declaration.getDeclarator()).get();
            final Optional<Integer> nonAtomicCallsCount = countNonAtomicCalls(uniqueName);
            if (!nonAtomicCallsCount.isPresent()) {
                return null;
            }

            // Create the data object and put the candidate in the map
            candidatesBuilder.put(uniqueName, new FunctionData(uniqueName,
                    declaration, nonAtomicCallsCount.get()));
            return null;
        }

        @Override
        public Void visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            declaration.getDeclaration().accept(this, null);
            return null;
        }

        @Override
        public Void visitDataDecl(DataDecl declaration, Void arg) {
            return null;
        }

        /**
         * @return <code>true</code> if the conditions for call assumptions are
         *         fulfilled.
         */
        private boolean checkCallAssumptions(FunctionDecl functionDecl) {
            if (functionDecl.getDeclaration() != null) {
                switch (functionDecl.getDeclaration().getCallAssumptions()) {
                    case SPONTANEOUS:
                    case HWEVENT:
                        return false;
                    case NONE:
                    case ATOMIC_HWEVENT:
                        return true;
                    default:
                        throw new RuntimeException("unexpected call assumptions "
                                + functionDecl.getDeclaration().getCallAssumptions());
                }
            } else {
                return true;
            }
        }

        /**
         * @return Count of non-atomic calls to function with given name. The
         *         object is absent if there are some references to the function
         *         that are not calls.
         */
        private Optional<Integer> countNonAtomicCalls(String funUniqueName) {
            final EntityNode node = refsGraph.getOrdinaryIds().get(funUniqueName);
            int nonAtomicCallsCount = 0;

            for (Reference reference : node.getPredecessors()) {
                switch (reference.getType()) {
                    case CALL:
                        if (!reference.isInsideAtomic()) {
                            ++nonAtomicCallsCount;
                        }
                        break;
                    case NORMAL:
                        /* This function is not a candidate if there is
                           a reference that is not a call. */
                        return Optional.absent();
                    default:
                        throw new RuntimeException("unexpected reference type "
                                + reference.getType());
                }
            }

            return Optional.of(nonAtomicCallsCount);
        }
    }

    /**
     * Helper class for collecting information about functions.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class FunctionData {
        private final String uniqueName;
        private final FunctionDecl functionDecl;
        private int nonAtomicCallsCount;

        private FunctionData(String uniqueName, FunctionDecl functionDecl,
                    int nonAtomicCallsCount) {
            checkNotNull(uniqueName, "unique name cannot be null");
            checkNotNull(functionDecl, "function definition cannot be null");
            checkArgument(!uniqueName.isEmpty(), "unique name cannot be an empty string");
            checkArgument(nonAtomicCallsCount >= 0, "non-atomic calls count cannot be negative");
            this.uniqueName = uniqueName;
            this.functionDecl = functionDecl;
            this.nonAtomicCallsCount = nonAtomicCallsCount;
        }
    }
}
