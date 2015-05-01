package pl.edu.mimuw.nesc.backend8051;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.EnumSet;
import java.util.Iterator;
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
     * The partition of the declarations that has been computed.
     */
    private Optional<Partition> partition;

    DeclarationsPartitioner(ImmutableList<Declaration> allDeclarations,
            ImmutableList<ImmutableSet<FunctionDecl>> banks) {
        checkNotNull(allDeclarations, "all declarations list cannot be null");
        checkNotNull(banks, "assignment to banks cannot be null");
        this.allDeclarations = allDeclarations;
        this.banks = banks;
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
        final ImmutableList<ImmutableList<Declaration>> codeFiles =
                collectBanksDefinitions();

        partition = Optional.of(new Partition(headerDecls, codeFiles));
        return partition.get();
    }

    private ImmutableList<Declaration> collectHeaderDeclarations() {
        final ImmutableList.Builder<Declaration> headerDeclsBuilder = ImmutableList.builder();
        final HeaderDeclarationsCollector collector = new HeaderDeclarationsCollector();

        for (Declaration declaration : allDeclarations) {
            headerDeclsBuilder.add(declaration.accept(collector, null));
        }

        return headerDeclsBuilder.build();
    }

    private ImmutableList<ImmutableList<Declaration>> collectBanksDefinitions() {
        final ImmutableList.Builder<ImmutableList<Declaration>> codeFiles = ImmutableList.builder();
        final Iterator<ImmutableSet<FunctionDecl>> banksIt = banks.iterator();

        if (banksIt.hasNext()) {
            codeFiles.add(collectCommonBankDefinitions(banksIt.next()));

            while (banksIt.hasNext()) {
                codeFiles.add(collectOrdinaryBankDefinitions(banksIt.next()));
            }
        }

        return codeFiles.build();
    }

    private ImmutableList<Declaration> collectCommonBankDefinitions(ImmutableSet<FunctionDecl> functions) {
        final ImmutableList.Builder<Declaration> declarationsBuilder = ImmutableList.builder();
        collectVariablesDefinitions(declarationsBuilder);
        collectFunctionsDefinitions(declarationsBuilder, functions);
        return declarationsBuilder.build();
    }

    private ImmutableList<Declaration> collectOrdinaryBankDefinitions(ImmutableSet<FunctionDecl> bank) {
        final ImmutableList.Builder<Declaration> declarationsBuilder = ImmutableList.builder();
        collectFunctionsDefinitions(declarationsBuilder, bank);
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
                ImmutableSet<FunctionDecl> bank) {
        for (FunctionDecl function : bank) {
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
        @Override
        public Declaration visitFunctionDecl(FunctionDecl functionDecl, Void arg) {
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
}
