package pl.edu.mimuw.nesc.backend8051;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Declarator;
import pl.edu.mimuw.nesc.ast.gen.ExceptionVisitor;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.ExtensionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.gen.IdentifierDeclarator;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRefDeclarator;
import pl.edu.mimuw.nesc.ast.gen.NestedDeclarator;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.astutil.TypeElementUtils;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.refsgraph.EntityNode;
import pl.edu.mimuw.nesc.refsgraph.Reference;
import pl.edu.mimuw.nesc.refsgraph.ReferencesGraph;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Class responsible for partitioning declarations into multiple files that
 * will be the output of the compiler.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class DeclarationsPartitioner {
    /**
     * List with all declarations of the program in the appropriate order.
     */
    private final ImmutableList<Declaration> allDeclarations;

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

    /**
     * Set with unique names of rigid functions - functions whose banking
     * characteristic will not be changed by the compiler. It means that
     * functions declared as banked remain banked and functions not declared
     * ad banked remain not banked.
     */
    private final ImmutableSet<String> rigidFunctions;

    /**
     * The partition of the declarations that has been computed.
     */
    private Optional<Partition> partition;

    DeclarationsPartitioner(ImmutableList<Declaration> allDeclarations,
            ImmutableList<ImmutableSet<FunctionDecl>> banks, ReferencesGraph refsGraph,
            ImmutableSet<String> rigidFunctions) {
        checkNotNull(allDeclarations, "all declarations list cannot be null");
        checkNotNull(banks, "assignment to banks cannot be null");
        checkNotNull(refsGraph, "references graph cannot be null");
        checkNotNull(rigidFunctions, "rigid functions cannot be null");

        final PrivateBuilder builder = new RealBuilder(allDeclarations, banks, refsGraph);
        this.allDeclarations = builder.buildAllDeclarations();
        this.banks = builder.buildBanks();
        this.funsAssignment = builder.buildFunsAssignment();
        this.refsGraph = builder.buildRefsGraph();
        this.rigidFunctions = rigidFunctions;
        this.partition = Optional.absent();
    }

    /**
     * Performs the partition for declarations given at construction.
     *
     * @return Partition for the NesC program.
     */
    Partition partition() {
        if (partition.isPresent()) {
            return partition.get();
        }

        final ImmutableSet<String> nonbankedFuns = determineNonbankedFuns();
        final ImmutableList<Declaration> headerDecls = collectHeaderDeclarations(nonbankedFuns);
        final ImmutableList<ImmutableList<Declaration>> codeFiles =
                collectBanksDefinitions(nonbankedFuns);

        partition = Optional.of(new Partition(headerDecls, codeFiles));
        return partition.get();
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

    private ImmutableList<Declaration> collectHeaderDeclarations(ImmutableSet<String> nonbankedFuns) {
        final ImmutableList.Builder<Declaration> headerDeclsBuilder = ImmutableList.builder();
        final HeaderDeclarationsCollector collector = new HeaderDeclarationsCollector(nonbankedFuns);

        for (Declaration declaration : allDeclarations) {
            headerDeclsBuilder.add(declaration.accept(collector, null));
        }

        return headerDeclsBuilder.build();
    }

    private void setIsBanked(Declarator declarator, FunctionDeclaration declaration,
                ImmutableSet<String> nonbankedFuns) {
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

        if (!rigidFunctions.contains(uniqueName)) {
            funDeclarator.setIsBanked(!nonbankedFuns.contains(uniqueName));
        }
    }

    private void prepareFunctionSpecifiers(LinkedList<TypeElement> specifiers,
                FunctionDeclaration declaration, boolean bankedFunction) {
        if (declaration != null && !declaration.isDefined()) {
            return;
        }

        TypeElementUtils.removeRID(specifiers, RID.STATIC, RID.EXTERN, RID.INLINE);

        if (bankedFunction) {
            specifiers.add(0, AstUtils.newRid(RID.STATIC));
        }
    }

    private ImmutableList<ImmutableList<Declaration>> collectBanksDefinitions(ImmutableSet<String> nonbankedFuns) {
        final ImmutableList.Builder<ImmutableList<Declaration>> codeFiles = ImmutableList.builder();
        final Iterator<ImmutableSet<FunctionDecl>> banksIt = banks.iterator();

        if (banksIt.hasNext()) {
            codeFiles.add(collectCommonBankDefinitions(banksIt.next(), nonbankedFuns));

            while (banksIt.hasNext()) {
                codeFiles.add(collectOrdinaryBankDefinitions(banksIt.next(), nonbankedFuns));
            }
        }

        return codeFiles.build();
    }

    private ImmutableList<Declaration> collectCommonBankDefinitions(ImmutableSet<FunctionDecl> functions,
                ImmutableSet<String> nonbankedDecls) {
        final ImmutableList.Builder<Declaration> declarationsBuilder = ImmutableList.builder();
        collectVariablesDefinitions(declarationsBuilder);
        collectFunctionsDefinitions(declarationsBuilder, functions, nonbankedDecls);
        return declarationsBuilder.build();
    }

    private ImmutableList<Declaration> collectOrdinaryBankDefinitions(ImmutableSet<FunctionDecl> bank,
                ImmutableSet<String> nonbankedFuns) {
        final ImmutableList.Builder<Declaration> declarationsBuilder = ImmutableList.builder();
        collectFunctionsDefinitions(declarationsBuilder, bank, nonbankedFuns);
        return declarationsBuilder.build();
    }

    private void collectVariablesDefinitions(ImmutableList.Builder<Declaration> declarationsBuilder) {
        for (Declaration declaration : allDeclarations) {
            if (declaration instanceof ExtensionDecl) {
                declaration = ((ExtensionDecl) declaration).getDeclaration();
            }
            if (declaration instanceof DataDecl) {
                final DataDecl dataDecl = (DataDecl) declaration;
                if (dataDecl.getDeclarations().isEmpty()) {
                    continue;
                }
                final EnumSet<RID> rids = TypeElementUtils.collectRID(dataDecl.getModifiers());
                if (rids.contains(RID.TYPEDEF)) {
                    continue;
                }
                final Optional<NestedDeclarator> deepestNestedDeclarator =
                        DeclaratorUtils.getDeepestNestedDeclarator(((VariableDecl) dataDecl.getDeclarations().getFirst()).getDeclarator());
                if (!deepestNestedDeclarator.isPresent() || !(deepestNestedDeclarator.get() instanceof FunctionDeclarator)
                        && !(deepestNestedDeclarator.get() instanceof InterfaceRefDeclarator)) {
                    if (!rids.contains(RID.EXTERN)) {
                        if (rids.contains(RID.STATIC)) {
                            TypeElementUtils.removeRID(dataDecl.getModifiers(), RID.STATIC);
                        }
                        declarationsBuilder.add(declaration);
                    }
                }
            }
        }
    }

    private void collectFunctionsDefinitions(ImmutableList.Builder<Declaration> declarationsBuilder,
                ImmutableSet<FunctionDecl> bank, ImmutableSet<String> nonbankedFuns) {
        for (FunctionDecl function : bank) {
            final String uniqueName = DeclaratorUtils.getUniqueName(function.getDeclarator()).get();
            setIsBanked(function.getDeclarator(), function.getDeclaration(), nonbankedFuns);
            prepareFunctionSpecifiers(function.getModifiers(), function.getDeclaration(),
                    !nonbankedFuns.contains(uniqueName));
            declarationsBuilder.add(function);
        }
    }

    /**
     * Visitor that collects declarations for the header file. It returns
     * the declaration that should appear in the file.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class HeaderDeclarationsCollector extends ExceptionVisitor<Declaration, Void> {
        private final ImmutableSet<String> nonbankedFuns;

        private HeaderDeclarationsCollector(ImmutableSet<String> nonbankedFuns) {
            checkNotNull(nonbankedFuns, "non-banked functions cannot be null");
            this.nonbankedFuns = nonbankedFuns;
        }

        @Override
        public Declaration visitFunctionDecl(FunctionDecl functionDecl, Void arg) {
            setIsBanked(functionDecl.getDeclarator(), functionDecl.getDeclaration(),
                    nonbankedFuns);
            final String uniqueName = DeclaratorUtils.getUniqueName(functionDecl.getDeclarator()).get();
            prepareFunctionSpecifiers(functionDecl.getModifiers(), functionDecl.getDeclaration(),
                    !nonbankedFuns.contains(uniqueName));
            return AstUtils.createForwardDeclaration(functionDecl);
        }

        @Override
        public Declaration visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            // Remove '__extension__'
            return declaration.getDeclaration().accept(this, null);
        }

        @Override
        public Declaration visitDataDecl(DataDecl declaration, Void arg) {
            checkState(declaration.getDeclarations().size() <= 1,
                    "more than one inner declaration encountered");
            if (declaration.getDeclarations().isEmpty()) {
                return declaration;
            }

            final VariableDecl variableDecl =
                    (VariableDecl) declaration.getDeclarations().getFirst();
            final String uniqueName = DeclaratorUtils.getUniqueName(variableDecl.getDeclarator().get()).get();
            final Optional<NestedDeclarator> deepestDeclarator =
                    DeclaratorUtils.getDeepestNestedDeclarator(variableDecl.getDeclarator().get());
            final EnumSet<RID> rids = TypeElementUtils.collectRID(declaration.getModifiers());

            if (rids.contains(RID.TYPEDEF)) {
                // Type definition
                return declaration;
            } else if (deepestDeclarator.isPresent()
                    && deepestDeclarator.get() instanceof FunctionDeclarator) {
                // Function forward declaration
                final FunctionDeclaration declarationObj = (FunctionDeclaration) variableDecl.getDeclaration();
                setIsBanked(variableDecl.getDeclarator().get(), declarationObj, nonbankedFuns);
                prepareFunctionSpecifiers(declaration.getModifiers(), declarationObj,
                        !nonbankedFuns.contains(uniqueName));
                return declaration;
            } else if (deepestDeclarator.isPresent() &&
                    deepestDeclarator.get() instanceof InterfaceRefDeclarator) {
                throw new IllegalStateException("unexpected interface reference declarator");
            } else {
                // Variable declaration
                final DataDecl copy = declaration.deepCopy(true);
                if (rids.contains(RID.STATIC)) {
                    TypeElementUtils.removeRID(copy.getModifiers(), RID.STATIC);
                }
                if (!rids.contains(RID.EXTERN)) {
                    copy.getModifiers().add(0, AstUtils.newRid(RID.EXTERN));
                }
                final VariableDecl copyInnerDecl = (VariableDecl) copy.getDeclarations().getFirst();
                copyInnerDecl.setInitializer(Optional.<Expression>absent());
                return copy;
            }
        }
    }

    /**
     * Class that represents a partition of declarations of a single program.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Partition {
        private final ImmutableList<Declaration> headerFile;
        private final ImmutableList<ImmutableList<Declaration>> codeFiles;

        private Partition(ImmutableList<Declaration> headerFile,
                    ImmutableList<ImmutableList<Declaration>> codeFiles) {
            checkNotNull(headerFile, "header file cannot be null");
            checkNotNull(codeFiles, "files with code cannot be null");
            this.headerFile = headerFile;
            this.codeFiles = codeFiles;
        }

        /**
         * Get the declarations that are to be placed in the header file.
         *
         * @return List with declaration for the header file.
         */
        public ImmutableList<Declaration> getHeaderFile() {
            return headerFile;
        }

        /**
         * Get declarations for each of the code banks. There may be less
         * elements on the list than count of all banks. Order of elements
         * of the least nested list corresponds to order of functions in
         * banks from the list given at construction.
         *
         * @return Declarations for each of the C files.
         */
        public ImmutableList<ImmutableList<Declaration>> getCodeFiles() {
            return codeFiles;
        }
    }

    /**
     * Interface for building particular elements of the partitioner.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private interface PrivateBuilder {
        ImmutableList<Declaration> buildAllDeclarations();
        ImmutableList<ImmutableSet<FunctionDecl>> buildBanks();
        ReferencesGraph buildRefsGraph();
        ImmutableMap<String, Integer> buildFunsAssignment();
    }

    /**
     * Implementation of the private builder interface for the partitioner.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class RealBuilder implements PrivateBuilder {
        /**
         * Data necessary for building a partitioner.
         */
        private final ImmutableList<Declaration> allDeclarations;
        private final ImmutableList<ImmutableSet<FunctionDecl>> banks;
        private final ReferencesGraph refsGraph;

        private RealBuilder(ImmutableList<Declaration> allDeclarations,
                ImmutableList<ImmutableSet<FunctionDecl>> banks, ReferencesGraph refsGraph) {
            this.allDeclarations = allDeclarations;
            this.banks = banks;
            this.refsGraph = refsGraph;
        }

        @Override
        public ImmutableList<Declaration> buildAllDeclarations() {
            return allDeclarations;
        }

        @Override
        public ImmutableList<ImmutableSet<FunctionDecl>> buildBanks() {
            return banks;
        }

        @Override
        public ReferencesGraph buildRefsGraph() {
            return refsGraph;
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
