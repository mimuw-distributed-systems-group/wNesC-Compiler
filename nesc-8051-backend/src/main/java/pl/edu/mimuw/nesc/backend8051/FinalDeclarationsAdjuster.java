package pl.edu.mimuw.nesc.backend8051;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Declarator;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.gen.IdentityVisitor;
import pl.edu.mimuw.nesc.ast.gen.NestedDeclarator;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.astutil.TypeElementUtils;
import pl.edu.mimuw.nesc.codepartition.BankTable;
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
     * Table with contents of each bank.
     */
    private final BankTable bankTable;

    /**
     * Assignment of functions to banks. Keys are unique names of functions and
     * values are names of banks.
     */
    private final ImmutableMap<String, String> funsAssignment;

    /**
     * Set with names of inline functions that are marked with 'static' and
     * 'inline' keyword.
     */
    private final ImmutableSet<String> inlineFunctions;

    /**
     * Graph of references between entities.
     */
    private final ReferencesGraph refsGraph;

    FinalDeclarationsAdjuster(
            ImmutableList<Declaration> allDeclarations,
            BankTable bankTable,
            ImmutableSet<String> inlineFunctions,
            boolean isBankedRelaxed,
            ReferencesGraph refsGraph
    ) {
        checkNotNull(allDeclarations, "declaration cannot be null");
        checkNotNull(bankTable, "bank table cannot be null");
        checkNotNull(inlineFunctions, "inline functions cannot be null");
        checkNotNull(refsGraph, "references graph cannot be null");

        final PrivateBuilder builder = new RealBuilder(bankTable);
        this.allDeclarations = allDeclarations;
        this.bankTable = bankTable;
        this.isBankedRelaxed = isBankedRelaxed;
        this.funsAssignment = builder.buildFunsAssignment();
        this.inlineFunctions = inlineFunctions;
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
     * Compute the set of candidates of functions that are not banked.
     * A function will be a candidate if all its references are calls and all
     * these calls are made by functions from the same bank and the function is
     * not spontaneous.
     *
     * @return Set of functions that don't need banking.
     */
    private ImmutableSet<String> determineNonbankedFuns() {
        final ImmutableSet.Builder<String> nonbankedFunsBuilder = ImmutableSet.builder();

        for (String bankName : bankTable.getBanksNames()) {
            final Iterable<FunctionDecl> bankContents = bankTable.getBankContents(bankName);

            if (bankName.equals(bankTable.getCommonBankName())) {
                addNonbankedFunsFromCommonBank(nonbankedFunsBuilder, bankContents);
            } else {
                addNonbankedFunsFromNormalBank(nonbankedFunsBuilder, bankName, bankContents);
            }
        }

        return nonbankedFunsBuilder.build();
    }

    private void addNonbankedFunsFromCommonBank(ImmutableSet.Builder<String> nonbankedFunsBuilder,
                Iterable<FunctionDecl> commonBank) {
        /* All functions from the common bank are candidates for non-banked
           functions. */
        for (FunctionDecl functionDecl : commonBank) {
            nonbankedFunsBuilder.add(DeclaratorUtils.getUniqueName(functionDecl.getDeclarator()).get());
        }
    }

    private void addNonbankedFunsFromNormalBank(ImmutableSet.Builder<String> nonbankedFunsBuilder,
                String bankName, Iterable<FunctionDecl> bankContents) {
        for (FunctionDecl functionDecl : bankContents) {
            // Skip spontaneous functions
            if (functionDecl.getDeclaration() != null && functionDecl.getDeclaration()
                    .getCallAssumptions().compareTo(FunctionDeclaration.CallAssumptions.SPONTANEOUS) >= 0) {
                continue;
            }

            final String uniqueName = DeclaratorUtils.getUniqueName(
                    functionDecl.getDeclarator()).get();

            if (!mustBeBanked(uniqueName, bankName)) {
                nonbankedFunsBuilder.add(uniqueName);
            }
        }
    }

    private boolean mustBeBanked(String funUniqueName, String funBankName) {
        for (Reference reference : refsGraph.getOrdinaryIds().get(funUniqueName).getPredecessors()) {
            final EntityNode referencingNode = reference.getReferencingNode();

            if (!reference.isInsideNotEvaluatedExpr()) {
                if (reference.getType() != Reference.Type.CALL) {
                    return true;
                } else if (referencingNode.getKind() != EntityNode.Kind.FUNCTION) {
                    return true;
                } else if (!funsAssignment.get(referencingNode.getUniqueName()).equals(funBankName)) {
                    return true; // call from a function from a different bank
                }
            }
        }

        return false;
    }

    private void setIsBanked(String funUniqueName, Declarator declarator,
                FunctionDeclaration declaration, ImmutableSet<String> nonbankedFuns) {
        // Don't change the banking characteristic for undefined functions
        if (declaration != null && !declaration.isDefined()) {
            return;
        }

        final NestedDeclarator deepestDeclarator =
                DeclaratorUtils.getDeepestNestedDeclarator(declarator).get();
        checkArgument(deepestDeclarator instanceof FunctionDeclarator,
                "expected declarator of a function");
        final FunctionDeclarator funDeclarator = (FunctionDeclarator) deepestDeclarator;

        /* Change the banking characteristic only if it is allowed according to
           banked relaxation. Inline functions are never banked. */
        if (inlineFunctions.contains(funUniqueName)) {
            funDeclarator.setIsBanked(false);
        } else if (!VariousUtils.getBooleanValue(funDeclarator.getIsBanked())
                || isBankedRelaxed && declaration != null && declaration.getCallAssumptions()
                    .compareTo(FunctionDeclaration.CallAssumptions.SPONTANEOUS) < 0) {
            funDeclarator.setIsBanked(!nonbankedFuns.contains(funUniqueName));
        }
    }

    private void prepareFunctionSpecifiers(String funUniqueName, LinkedList<TypeElement> specifiers,
            FunctionDeclaration declaration) {
        if (declaration != null && !declaration.isDefined()) {
            return;
        }

        if (inlineFunctions.contains(funUniqueName)) {
            /* FIXME: If 'static' specifier is added, it is necessary to check
               if the declaration hasn't block scope. */
            final EnumSet<RID> rids = TypeElementUtils.collectRID(specifiers);
            if (rids.contains(RID.EXTERN)) {
                TypeElementUtils.removeRID(specifiers, RID.EXTERN);
            }
            if (!rids.contains(RID.INLINE)) {
                specifiers.addFirst(AstUtils.newRid(RID.INLINE));
            }
            if (!rids.contains(RID.STATIC)) {
                specifiers.addFirst(AstUtils.newRid(RID.STATIC));
            }
        } else {
            TypeElementUtils.removeRID(specifiers, RID.STATIC, RID.EXTERN, RID.INLINE);
        }
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
            final String funUniqueName = DeclaratorUtils.getUniqueName(
                    functionDecl.getDeclarator()).get();
            setIsBanked(funUniqueName, functionDecl.getDeclarator(),
                    functionDecl.getDeclaration(), nonbankedFuns);
            prepareFunctionSpecifiers(funUniqueName, functionDecl.getModifiers(),
                    functionDecl.getDeclaration());
            return null;
        }

        @Override
        public Void visitFunctionDeclarator(FunctionDeclarator declarator, Void arg) {
            FluentIterable<Declaration> paramsDecls = FluentIterable.from(declarator.getParameters());
            if (declarator.getGenericParameters().isPresent()) {
                paramsDecls = paramsDecls.append(declarator.getGenericParameters().get());
            }

            for (Declaration paramDecl : paramsDecls) {
                // The parameter can be also an ellipsis declaration
                if (paramDecl instanceof DataDecl) {
                    parametersDecls.add((DataDecl) paramDecl);
                }
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
                final String uniqueName = DeclaratorUtils.getUniqueName(
                        variableDecl.getDeclarator().get()).get();
                setIsBanked(uniqueName, variableDecl.getDeclarator().get(), declarationObj, nonbankedFuns);
                prepareFunctionSpecifiers(uniqueName, declaration.getModifiers(), declarationObj);
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
        ImmutableMap<String, String> buildFunsAssignment();
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
        private final BankTable bankTable;

        private RealBuilder(BankTable bankTable) {
            this.bankTable = bankTable;
        }

        @Override
        public ImmutableMap<String, String> buildFunsAssignment() {
            final ImmutableMap.Builder<String, String> funsAssignmentBuilder = ImmutableMap.builder();
            for (String bankName : bankTable.getBanksNames()) {
                for (FunctionDecl functionDecl : bankTable.getBankContents(bankName)) {
                    funsAssignmentBuilder.put(DeclaratorUtils.getUniqueName(
                            functionDecl.getDeclarator()).get(), bankName);
                }
            }
            return funsAssignmentBuilder.build();
        }
    }
}
