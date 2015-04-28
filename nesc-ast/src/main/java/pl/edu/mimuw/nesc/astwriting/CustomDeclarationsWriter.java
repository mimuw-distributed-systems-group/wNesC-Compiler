package pl.edu.mimuw.nesc.astwriting;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.List;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Declarator;
import pl.edu.mimuw.nesc.ast.gen.ExceptionVisitor;
import pl.edu.mimuw.nesc.ast.gen.ExtensionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRefDeclarator;
import pl.edu.mimuw.nesc.ast.gen.NestedDeclarator;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Creator of header files that contain only declarations without definitions
 * of functions and variables.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class CustomDeclarationsWriter {
    /**
     * Path to the header file that will be created.
     */
    private String outputFile;

    /**
     * Value indicating whether forward declarations of functions will be outputted
     * instead of their definitions.
     */
    private boolean forwardDeclarations;

    /**
     * Value that indicates if all functions will be outputted as banked or not.
     */
    private Banking banking;

    /**
     * A text that will be written before the declarations.
     */
    private String prependedText;

    /**
     * Write settings used for writing.
     */
    private WriteSettings writeSettings;

    public CustomDeclarationsWriter(String outputFile, boolean functionsForwardDeclarations,
            Banking banking, WriteSettings writeSettings) {
        checkNotNull(outputFile, "output file cannot be null");
        checkNotNull(banking, "banking setting cannot be null");
        checkNotNull(writeSettings, "write settings cannot be null");
        checkArgument(!outputFile.isEmpty(), "output file cannot be an empty string");

        this.outputFile = outputFile;
        this.forwardDeclarations = functionsForwardDeclarations;
        this.banking = banking;
        this.prependedText = "";
        this.writeSettings = writeSettings;
    }

    public void setWriteSettings(WriteSettings settings) {
        checkNotNull(settings, "settings cannot be null");
        this.writeSettings = settings;
    }

    public void setOutputFile(String outputFile) {
        checkNotNull(outputFile, "output file cannot be null");
        checkArgument(!outputFile.isEmpty(), "output file cannot be an empty string");
        this.outputFile = outputFile;
    }

    public void setForwardDeclarations(boolean forwardDeclarations) {
        this.forwardDeclarations = forwardDeclarations;
    }

    public void setBanking(Banking banking) {
        checkNotNull(banking, "banked cannot be null");
        this.banking = banking;
    }

    public void setPrependedText(Optional<String> prependedText) {
        checkNotNull(prependedText, "text to prepend cannot be null");
        this.prependedText = prependedText.or("");
    }

    /**
     * Write given declarations to a file in a way implied by configuration of
     * this object. Iterator returned by the given iterable must allow multiple
     * iterations over the declarations.
     *
     * @param declarations Declarations that will be written.
     */
    public void write(List<? extends Declaration> declarations) throws IOException {
        checkNotNull(declarations, "declarations cannot be null");

        final Optional<Boolean> isBanked = prepareIsBankedValue();
        final List<? extends Declaration> forWriting;
        final ImmutableMap<Node, Boolean> originalIsBanked;

        // Prepare declarations
        if (isBanked.isPresent() || forwardDeclarations) {
            final ImmutableList.Builder<Declaration> forWritingBuilder = ImmutableList.builder();
            final PreparingVisitor preparingVisitor = new PreparingVisitor(isBanked, forwardDeclarations);
            for (Declaration declaration : declarations) {
                forWritingBuilder.add(declaration.accept(preparingVisitor, null));
            }
            forWriting = forWritingBuilder.build();
            originalIsBanked = preparingVisitor.getOriginalValues();
        } else {
            forWriting = declarations;
            originalIsBanked = ImmutableMap.of();
        }

        // Write declarations
        try (final ASTWriter writer = new ASTWriter(outputFile, writeSettings)) {
            writer.write(prependedText);
            writer.write(forWriting);
        } finally {
            if (!originalIsBanked.isEmpty()) {
                final RestoringVisitor restoringVisitor = new RestoringVisitor(originalIsBanked);
                for (Declaration declaration : declarations) {
                    declaration.accept(restoringVisitor, null);
                }
            }
        }
    }

    private Optional<Boolean> prepareIsBankedValue() {
        switch (banking) {
            case DONT_CHANGE:
                return Optional.absent();
            case DEFINED_BANKED:
                return Optional.of(true);
            case DEFINED_NOT_BANKED:
                return Optional.of(false);
            default:
                throw new RuntimeException("unexpected value of banking: " + banking);
        }
    }

    /**
     * Enum type that represents settings related to functions banking.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum Banking {
        DONT_CHANGE,
        DEFINED_BANKED,
        DEFINED_NOT_BANKED,
    }

    /**
     * Visitor that prepares declarations to write.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class PreparingVisitor extends ExceptionVisitor<Declaration, Void> {
        /**
         * Value of the flag to set.
         */
        private final Optional<Boolean> isBankedValue;

        /**
         * Value that indicate whether add forward declarations of functions
         * instead their definitions.
         */
        private final boolean forwardDeclarations;

        /**
         * Builder for a map that will contain original values of the isBanked
         * flag.
         */
        private final ImmutableMap.Builder<Node, Boolean> originalValuesBuilder;

        private PreparingVisitor(Optional<Boolean> isBankedValue, boolean forwardDeclarations) {
            this.isBankedValue = isBankedValue;
            this.forwardDeclarations = forwardDeclarations;
            this.originalValuesBuilder = ImmutableMap.builder();
        }

        private ImmutableMap<Node, Boolean> getOriginalValues() {
            return originalValuesBuilder.build();
        }

        @Override
        public Declaration visitFunctionDecl(FunctionDecl declaration, Void arg) {
            if (isBankedValue.isPresent()) {
                originalValuesBuilder.put(declaration, DeclaratorUtils.getIsBanked(declaration.getDeclarator()));
                DeclaratorUtils.setIsBanked(declaration.getDeclarator(), isBankedValue.get());
            }

            return forwardDeclarations
                    ? AstUtils.createForwardDeclaration(declaration)
                    : declaration;
        }

        @Override
        public Declaration visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            final Declaration newDeclaration = declaration.getDeclaration().accept(this, null);
            return newDeclaration != declaration.getDeclaration()
                    ? new ExtensionDecl(Location.getDummyLocation(), newDeclaration)
                    : declaration;
        }

        @Override
        public Declaration visitDataDecl(DataDecl declaration, Void arg) {
            if (isBankedValue.isPresent()) {
                for (Declaration innerDeclaration : declaration.getDeclarations()) {
                    innerDeclaration.accept(this, null);
                }
            }
            return declaration;
        }

        @Override
        public Declaration visitVariableDecl(VariableDecl declaration, Void arg) {
            final Optional<NestedDeclarator> deepestNestedDeclarator =
                    DeclaratorUtils.getDeepestNestedDeclarator(declaration.getDeclarator());

            if (deepestNestedDeclarator.isPresent()
                    && deepestNestedDeclarator.get() instanceof FunctionDeclarator) {

                final FunctionDeclaration declarationObj =
                        (FunctionDeclaration) declaration.getDeclaration();

                if (declarationObj == null || declarationObj.isDefined()) {
                    final Declarator declarator = declaration.getDeclarator().get();
                    originalValuesBuilder.put(declaration, DeclaratorUtils.getIsBanked(declarator));
                    DeclaratorUtils.setIsBanked(declarator, isBankedValue.get());
                }
            } else if (deepestNestedDeclarator.isPresent()
                    && deepestNestedDeclarator.get() instanceof InterfaceRefDeclarator) {
                throw new RuntimeException("unexpected interface reference declarator");
            }

            return null;
        }
    }

    /**
     * Visitor that removes changes made to the AST nodes.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class RestoringVisitor extends ExceptionVisitor<Void, Void> {
        /**
         * Map with original values of is banked flag.
         */
        private final ImmutableMap<Node, Boolean> originalIsBanked;

        private RestoringVisitor(ImmutableMap<Node, Boolean> originalIsBanked) {
            this.originalIsBanked = originalIsBanked;
        }

        @Override
        public Void visitFunctionDecl(FunctionDecl declaration, Void arg) {
            if (originalIsBanked.containsKey(declaration)) {
                DeclaratorUtils.setIsBanked(declaration.getDeclarator(),
                        originalIsBanked.get(declaration));
            }
            return null;
        }

        @Override
        public Void visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            declaration.getDeclaration().accept(this, null);
            return null;
        }

        @Override
        public Void visitDataDecl(DataDecl declaration, Void arg) {
            for (Declaration innerDeclaration : declaration.getDeclarations()) {
                innerDeclaration.accept(this, null);
            }
            return null;
        }

        @Override
        public Void visitVariableDecl(VariableDecl declaration, Void arg) {
            if (originalIsBanked.containsKey(declaration)) {
                DeclaratorUtils.setIsBanked(declaration.getDeclarator().get(),
                        originalIsBanked.get(declaration));
            }
            return null;
        }
    }
}
