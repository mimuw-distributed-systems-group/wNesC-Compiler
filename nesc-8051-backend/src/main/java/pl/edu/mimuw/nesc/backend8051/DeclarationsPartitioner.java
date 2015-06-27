package pl.edu.mimuw.nesc.backend8051;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.ExceptionVisitor;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.ExtensionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRefDeclarator;
import pl.edu.mimuw.nesc.ast.gen.NestedDeclarator;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.astutil.TypeElementUtils;
import pl.edu.mimuw.nesc.codepartition.BankTable;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;

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
    private final BankTable bankTable;

    /**
     * Set with unique names of functions that are static inline.
     */
    private final ImmutableSet<String> inlineFunctions;

    /**
     * Object for generation of names for unnamed enumerated, structure and
     * union types.
     */
    private final NameMangler nameMangler;

    /**
     * The partition of the declarations that has been computed.
     */
    private Optional<Partition> partition;

    DeclarationsPartitioner(ImmutableList<Declaration> allDeclarations, BankTable bankTable,
            ImmutableSet<String> inlineFunctions, NameMangler nameMangler) {
        checkNotNull(allDeclarations, "all declarations list cannot be null");
        checkNotNull(bankTable, "bank table cannot be null");
        checkNotNull(inlineFunctions, "inline functions cannot be null");
        checkNotNull(nameMangler, "name mangler cannot be null");
        this.allDeclarations = allDeclarations;
        this.bankTable =  bankTable;
        this.inlineFunctions = inlineFunctions;
        this.nameMangler = nameMangler;
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

        final ImmutableList<Declaration> headerDecls = collectHeaderDeclarations();
        final ImmutableListMultimap<String, Declaration> codeFiles = collectBanksDefinitions();

        partition = Optional.of(new Partition(headerDecls, codeFiles));
        return partition.get();
    }

    private ImmutableList<Declaration> collectHeaderDeclarations() {
        final ImmutableList.Builder<Declaration> headerDeclsBuilder = ImmutableList.builder();
        final HeaderDeclarationsCollector collector = new HeaderDeclarationsCollector();

        // Add only declarations
        for (Declaration declaration : allDeclarations) {
            headerDeclsBuilder.add(declaration.accept(collector, null));
        }

        // Add definitions of all inline functions
        if (collector.inlineFunctionsDefinitions.size() != inlineFunctions.size()) {
            throw new RuntimeException("cannot collect definitions of all inline functions");
        }
        headerDeclsBuilder.addAll(collector.inlineFunctionsDefinitions);

        return headerDeclsBuilder.build();
    }

    private ImmutableListMultimap<String, Declaration> collectBanksDefinitions() {
        final ArrayListMultimap<String, Declaration> codeFiles = ArrayListMultimap.create();

        for (String bankName : bankTable.getBanksNames()) {
            final List<Declaration> bankDeclarations = codeFiles.get(bankName);
            if (bankName.equals(bankTable.getCommonBankName())) {
                collectCommonBankDefinitions(bankDeclarations, bankTable.getBankContents(bankName));
            } else {
                collectOrdinaryBankDefinitions(bankDeclarations, bankTable.getBankContents(bankName));
            }
        }

        return ImmutableListMultimap.copyOf(codeFiles);
    }

    private void collectCommonBankDefinitions(List<Declaration> bankDeclarations,
            List<FunctionDecl> functions) {
        collectVariablesDefinitions(bankDeclarations);
        collectFunctionsDefinitions(bankDeclarations, functions);
    }

    private void collectOrdinaryBankDefinitions(List<Declaration> bankDeclarations,
            List<FunctionDecl> bank) {
        collectFunctionsDefinitions(bankDeclarations, bank);
    }

    private void collectVariablesDefinitions(List<Declaration> bankDeclarations) {
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
                        AstUtils.undefineTags(dataDecl.getModifiers());
                        if (rids.contains(RID.STATIC)) {
                            TypeElementUtils.removeRID(dataDecl.getModifiers(), RID.STATIC);
                        }
                        bankDeclarations.add(declaration);
                    }
                }
            }
        }
    }

    private void collectFunctionsDefinitions(List<Declaration> bankDeclarations,
                List<FunctionDecl> bank) {
        bankDeclarations.addAll(bank);
    }

    /**
     * Visitor that collects declarations for the header file. It returns
     * the declaration that should appear in the file.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class HeaderDeclarationsCollector extends ExceptionVisitor<Declaration, Void> {
        private final List<FunctionDecl> inlineFunctionsDefinitions = new ArrayList<>();

        @Override
        public Declaration visitFunctionDecl(FunctionDecl functionDecl, Void arg) {
            final String funUniqueName = DeclaratorUtils.getUniqueName(
                    functionDecl.getDeclarator()).get();
            if (inlineFunctions.contains(funUniqueName)) {
                inlineFunctionsDefinitions.add(functionDecl);
            }
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
            final Optional<NestedDeclarator> deepestDeclarator =
                    DeclaratorUtils.getDeepestNestedDeclarator(variableDecl.getDeclarator().get());
            final EnumSet<RID> rids = TypeElementUtils.collectRID(declaration.getModifiers());

            if (rids.contains(RID.TYPEDEF)) {
                // Type definition
                return declaration;
            } else if (deepestDeclarator.isPresent()
                    && deepestDeclarator.get() instanceof FunctionDeclarator) {
                // Function forward declaration
                return declaration;
            } else if (deepestDeclarator.isPresent() &&
                    deepestDeclarator.get() instanceof InterfaceRefDeclarator) {
                throw new IllegalStateException("unexpected interface reference declarator");
            } else {
                // Variable declaration
                if (!rids.contains(RID.EXTERN)) {
                    AstUtils.nameTags(declaration.getModifiers(), nameMangler);
                }
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
     * Class that represents a partition of declarations of a single program to
     * files.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Partition {
        private final ImmutableList<Declaration> headerFile;
        private final ImmutableListMultimap<String, Declaration> codeFiles;

        private Partition(ImmutableList<Declaration> headerFile,
                    ImmutableListMultimap<String, Declaration> codeFiles) {
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
         * keys of the returned map than count of all banks. Order of elements
         * associated with each key corresponds to the order of functions in
         * banks from the list given at construction.
         *
         * @return List multimap with declarations for each of the C files. Keys
         *         are names of banks and values are declarations to be put in
         *         the file for the bank.
         */
        public ImmutableListMultimap<String, Declaration> getCodeFiles() {
            return codeFiles;
        }
    }
}
