package pl.edu.mimuw.nesc.backend8051;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Declarator;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.gen.IdentifierDeclarator;
import pl.edu.mimuw.nesc.ast.gen.IdentityVisitor;
import pl.edu.mimuw.nesc.ast.gen.NestedDeclarator;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.astutil.TypeElementUtils;
import pl.edu.mimuw.nesc.common.util.VariousUtils;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.refsgraph.EntityNode;
import pl.edu.mimuw.nesc.refsgraph.Reference;
import pl.edu.mimuw.nesc.refsgraph.ReferencesGraph;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Adjuster responsible for setting storage-class specifiers and __banked
 * keyword for functions.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class FinalDeclarationsAdjuster {
    /**
     * List with all declarations of a NesC program that will be adjusted.
     */
    private final ImmutableList<Declaration> allDeclarations;

    /**
     * Value that indicates if the __banked keyword is relaxed by the compiler
     * option.
     */
    private final boolean isBankedRelaxed;

    /**
     * Contents of each bank.
     */
    private final ImmutableList<ImmutableSet<FunctionDecl>> banks;

    /**
     * Assignment of functions to banks. Keys are unique names of functions and
     * values are indices of banks. The indices correspond to indices of list
     * <code>banks</code>.
     */
    private final ImmutableMap<String, Integer> funsAssignment;

    /**
     * Graph of references between entities.
     */
    private final ReferencesGraph refsGraph;

    FinalDeclarationsAdjuster(
            ImmutableList<Declaration> allDeclarations,
            ImmutableList<ImmutableSet<FunctionDecl>> banks,
            boolean isBankedRelaxed,
            ReferencesGraph refsGraph
    ) {
        checkNotNull(allDeclarations, "declaration cannot be null");
        checkNotNull(banks, "banks cannot be null");
        checkNotNull(refsGraph, "references graph cannot be null");

        final PrivateBuilder builder = new RealBuilder(banks);
        this.allDeclarations = allDeclarations;
        this.banks = banks;
        this.isBankedRelaxed = isBankedRelaxed;
        this.funsAssignment = builder.buildFunsAssignment();
        this.refsGraph = refsGraph;
    }

    /**
     * Performs the adjustment of declarations given at construction.
     */
    public void adjust() {
        final AdjustingVisitor adjustingVisitor = new AdjustingVisitor(determineNonbankedFuns());
        for (Declaration declaration : allDeclarations) {
            declaration.traverse(adjustingVisitor, null);
        }
    }

    /**
     * Compute the set of functions that are not banked. A function will not be
     * banked if all its references are calls and all these calls are made by
     * functions from the same bank.
     *
     * @return Set of functions that don't need banking.
     */
    private ImmutableSet<String> determineNonbankedFuns() {
        final ImmutableSet.Builder<String> nonbankedFunsBuilder = ImmutableSet.builder();

        for (int i = 0; i < banks.size(); ++i) {
            for (FunctionDecl functionDecl : banks.get(i)) {
                final String uniqueName = DeclaratorUtils.getUniqueName(functionDecl.getDeclarator()).get();
                boolean banked = false;

                // Check entities that refer to the function
                for (Reference reference : refsGraph.getOrdinaryIds().get(uniqueName).getPredecessors()) {
                    final EntityNode referencingNode = reference.getReferencingNode();

                    if (!reference.isInsideNotEvaluatedExpr()) {
                        if (reference.getType() != Reference.Type.CALL) {
                            banked = true;
                            break;
                        } else if (referencingNode.getKind() != EntityNode.Kind.FUNCTION) {
                            banked = true;
                            break;
                        } else if (funsAssignment.get(referencingNode.getUniqueName()) != i) {
                            banked = true; // call from a function from a different bank
                            break;
                        }
                    }
                }

                if (!banked) {
                    nonbankedFunsBuilder.add(uniqueName);
                }
            }
        }

        return nonbankedFunsBuilder.build();
    }

    private void setIsBanked(Declarator declarator, FunctionDeclaration declaration,
            ImmutableSet<String> nonbankedFuns) {
        // Don't change the banking characteristic for undefined functions
        if (declaration != null && !declaration.isDefined()) {
            return;
        }

        final NestedDeclarator deepestDeclarator =
                DeclaratorUtils.getDeepestNestedDeclarator(declarator).get();
        checkArgument(deepestDeclarator instanceof FunctionDeclarator,
                "expected declarator of a function");
        final FunctionDeclarator funDeclarator = (FunctionDeclarator) deepestDeclarator;
        final IdentifierDeclarator identDeclarator =
                (IdentifierDeclarator) deepestDeclarator.getDeclarator().get();
        final String uniqueName = identDeclarator.getUniqueName().get();

        /* Change the banking characteristic only if it is allowed according to
           banked relaxation. */
        if (!VariousUtils.getBooleanValue(funDeclarator.getIsBanked())
                || isBankedRelaxed && declaration != null && declaration.getCallAssumptions()
                    .compareTo(FunctionDeclaration.CallAssumptions.SPONTANEOUS) < 0) {
            funDeclarator.setIsBanked(!nonbankedFuns.contains(uniqueName));
        }
    }

    private void prepareFunctionSpecifiers(LinkedList<TypeElement> specifiers,
            FunctionDeclaration declaration) {
        if (declaration != null && !declaration.isDefined()) {
            return;
        }

        /* If 'static' specifier was to be added, it is necessary to check if
           the declaration hasn't block scope. */

        TypeElementUtils.removeRID(specifiers, RID.STATIC, RID.EXTERN, RID.INLINE);
    }

    /**
     * Visitor that performs the adjustment of visited nodes.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class AdjustingVisitor extends IdentityVisitor<Void> {
        private final ImmutableSet<String> nonbankedFuns;
        private final Set<DataDecl> parametersDecls;

        private AdjustingVisitor(ImmutableSet<String> nonbankedFuns) {
            checkNotNull(nonbankedFuns, "not banked functions cannot be null");
            this.nonbankedFuns = nonbankedFuns;
            this.parametersDecls = new HashSet<>();
        }

        @Override
        public Void visitFunctionDecl(FunctionDecl functionDecl, Void arg) {
            setIsBanked(functionDecl.getDeclarator(), functionDecl.getDeclaration(), nonbankedFuns);
            prepareFunctionSpecifiers(functionDecl.getModifiers(), functionDecl.getDeclaration());
            return null;
        }

        @Override
        public Void visitFunctionDeclarator(FunctionDeclarator declarator, Void arg) {
            FluentIterable<Declaration> paramsDecls = FluentIterable.from(declarator.getParameters());
            if (declarator.getGenericParameters().isPresent()) {
                paramsDecls = paramsDecls.append(declarator.getGenericParameters().get());
            }

            for (Declaration paramDecl : paramsDecls) {
                parametersDecls.add((DataDecl) paramDecl);
            }

            return null;
        }

        @Override
        public Void visitDataDecl(DataDecl declaration, Void arg) {
            // Don't change declarations of parameters and tags
            if (parametersDecls.remove(declaration) || declaration.getDeclarations().isEmpty()) {
                return null;
            }

            // Don't affect declarations of fields in tags
            final boolean containsVariableDecl = Iterables.any(declaration.getDeclarations(),
                    Predicates.instanceOf(VariableDecl.class));
            checkState(!containsVariableDecl || declaration.getDeclarations().size() == 1,
                    "a variable declaration that is not separated is present");
            if (!containsVariableDecl) {
                return null;
            }

            // Modify declaration of a function
            final VariableDecl variableDecl = (VariableDecl) declaration.getDeclarations().getFirst();
            final Optional<NestedDeclarator> deepestDeclarator =
                    DeclaratorUtils.getDeepestNestedDeclarator(variableDecl.getDeclarator());
            if (deepestDeclarator.isPresent() && deepestDeclarator.get() instanceof FunctionDeclarator) {
                final FunctionDeclaration declarationObj = (FunctionDeclaration) variableDecl.getDeclaration();
                setIsBanked(variableDecl.getDeclarator().get(), declarationObj, nonbankedFuns);
                prepareFunctionSpecifiers(declaration.getModifiers(), declarationObj);
            }

            return null;
        }
    }

    /**
     * Interface for building some elements of the adjuster.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private interface PrivateBuilder {
        ImmutableMap<String, Integer> buildFunsAssignment();
    }

    /**
     * Implementation of the private builder interface for the adjuster.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class RealBuilder implements PrivateBuilder {
        /**
         * Data necessary for building some elements of the adjuster.
         */
        private final ImmutableList<ImmutableSet<FunctionDecl>> banks;

        private RealBuilder(ImmutableList<ImmutableSet<FunctionDecl>> banks) {
            this.banks = banks;
        }

        @Override
        public ImmutableMap<String, Integer> buildFunsAssignment() {
            final ImmutableMap.Builder<String, Integer> funsAssignmentBuilder = ImmutableMap.builder();
            for (int i = 0; i < banks.size(); ++i) {
                for (FunctionDecl functionDecl : banks.get(i)) {
                    funsAssignmentBuilder.put(DeclaratorUtils.getUniqueName(
                            functionDecl.getDeclarator()).get(), i);
                }
            }
            return funsAssignmentBuilder.build();
        }
    }
}
