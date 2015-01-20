package pl.edu.mimuw.nesc.astwriting;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import pl.edu.mimuw.nesc.ast.StructSemantics;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.ast.gen.Declaration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static pl.edu.mimuw.nesc.astwriting.Tokens.BinaryOp.*;
import static pl.edu.mimuw.nesc.astwriting.Tokens.*;
import static pl.edu.mimuw.nesc.astwriting.Tokens.UnaryOp.*;

/**
 * <p>Class responsible for producing the code of a program (given in the form
 * of AST) as text.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ASTWriter implements Closeable {
    /**
     * Check if the given statement is always written in multiple lines
     * by the writer. The predicate is fulfilled if and only if the given
     * statement is always written in multiple lines by a writer.
     */
    private static final Predicate<Statement> IS_MULTILINE_STMT = new Predicate<Statement>() {
        @Override
        public boolean apply(Statement stmt) {
            checkNotNull(stmt, "the statement cannot be null");
            return stmt instanceof CompoundStmt || stmt instanceof IfStmt
                    || stmt instanceof ForStmt || stmt instanceof WhileStmt
                    || stmt instanceof DoWhileStmt || stmt instanceof SwitchStmt;
        }
    };

    /**
     * Check if the given declaration is always written in multiple lines
     * by the writer. The predicate is fulfilled if and only if the given
     * declaration is always written in multiple lines by this visitor.
     */
    private static final Predicate<Declaration> IS_MULTILINE_DECLARATION = new Predicate<Declaration>() {
        @Override
        public boolean apply(Declaration declaration) {
            checkNotNull(declaration, "declaration cannot be null");

            if (declaration instanceof FunctionDecl || declaration instanceof NescDecl) {
                return true;
            } else if (declaration instanceof DataDecl) {
                // Check if the declaration contains the definition of a tag
                final DataDecl dataDecl = (DataDecl) declaration;
                for (TypeElement typeElement : dataDecl.getModifiers()) {
                    if (!(typeElement instanceof TagRef)) {
                        continue;
                    }

                    final TagRef tagReference = (TagRef) typeElement;
                    if (tagReference.getSemantics() != StructSemantics.OTHER) {
                        return true;
                    }
                }
            }

            return false;
        }
    };

    /**
     * A terminator function for any node that always results in no terminator.
     */
    private static final Function<Node, String> EMPTY_TERMINATOR_FUN = new Function<Node, String>() {
        @Override
        public String apply(Node node) {
            checkNotNull(node, "node cannot be null");
            return "";
        }
    };

    /**
     * Terminator function for declarations. It returns a semicolon if the
     * declaration shall be ended with it but is not while visiting the node
     * by the visitor. Otherwise, the empty string denotes no terminator.
     */
    private final Function<Declaration, String> DECLARATION_TERMINATOR_FUN = new Function<Declaration, String>() {
        @Override
        public String apply(Declaration declaration) {
            checkNotNull(declaration, "declaration cannot be null");
            return writeVisitor.needsTrailingSemicolon(declaration)
                    ? SEMICOLON
                    : "";
        }
    };

    /**
     * The writer used for output.
     */
    private final PrintWriter output;

    /**
     * Settings that affect the writing operation.
     */
    private final WriteSettings settings;

    /**
     * String that constitutes a single indentation.
     */
    private final String indentationStep;

    /**
     * String builder whose contents are the current indentation.
     */
    private final StringBuilder indentation = new StringBuilder();

    /**
     * Instance of the visitor that will be used for writing.
     */
    private final WriteVisitor writeVisitor = new WriteVisitor();

    private ASTWriter(PrivateBuilder privateBuilder) throws IOException {
        this.output = privateBuilder.buildWriter();
        this.settings = privateBuilder.getSettings();
        this.indentationStep = privateBuilder.buildIndentationStep();
    }

    /**
     * Initializes this writer to write to the given stream using given
     * settings.
     *
     * @param targetStream Stream to write the code to.
     * @param settings Settings that depict the way of writing the code.
     */
    public ASTWriter(OutputStream targetStream, WriteSettings settings) throws IOException {
        this(new OutputStreamPrivateBuilder(targetStream, settings));
    }

    /**
     * Initializes this writer to write to the file with given name. The file is
     * truncated by the constructor.
     *
     * @param fileName Name of the file to write the code to.
     * @param settings Settings that depict the way of writing code.
     * @throws IOException The operation of opening the file and truncating it
     *                     fails.
     */
    public ASTWriter(String fileName, WriteSettings settings) throws IOException {
        this(new FilePrivateBuilder(fileName, settings));
    }

    /**
     * Get the given node in its code representation.
     *
     * @param node Node to be represented as code.
     * @return Textual representation of the given node as it would appear
     *         in a program.
     * @throws NullPointerException Given argument is null.
     */
    public static String writeToString(Node node) {
        checkNotNull(node, "node cannot be null");

        final ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        final ASTWriter codeWriter;

        try {
            codeWriter = new ASTWriter(byteOutStream, WriteSettings.DEFAULT_SETTINGS);
        } catch (IOException e) {
            throw new RuntimeException("Cannot write a node as a string: " + e.getMessage());
        }

        codeWriter.write(node);
        codeWriter.close();
        return byteOutStream.toString();
    }

    @Override
    public void close() {
        output.close();
    }

    /**
     * Write the given node as it would appear in the source code of a C or
     * NesC program. If the node produces any output, it is guaranteed that the
     * first and last characters that are written are not whitespace.
     *
     * @param node Node to write as it would appear in the source code.
     */
    public void write(Node node) {
        node.accept(writeVisitor, null);
    }

    /**
     * Write all declarations from the given list producing empty lines between
     * declarations that are written in multiple lines (single empty line is
     * placed between such declarations). Moreover, if the list is not empty,
     * the last character written is a new line character. The first is
     * a non-whitespace character.
     *
     * @param declarations Declarations to write to the output.
     */
    public void write(List<? extends Declaration> declarations) {
        writeVisitor.writeTopLevelDeclarations(declarations);
    }

    private void increaseIndentation() {
        indentation.append(indentationStep);
    }

    private void decreaseIndentation() {
        checkState(indentation.length() >= indentationStep.length(),
                "current indentation is too small to decrease it");
        indentation.delete(indentation.length() - indentationStep.length(),
                indentation.length());
    }

    /**
     * <p>Class responsible for writing particular nodes. All methods shall keep
     * the invariant:</p>
     * <ul>
     *     <li>The first character written by the method is not whitespace.</li>
     *     <li>The last character written by the method is not whitespace.</li>
     *     <li>The method contains the same count of calls to
     *     <code>increaseIndentation</code> and to
     *     <code>decreaseIndentation</code>.</li>
     * </ul>
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class WriteVisitor implements Visitor<Void, Void> {
        @Override
        public Void visitInterface(Interface interfaceAst, Void arg) {
            output.write(NESC_INTERFACE);
            output.write(SPACE);
            output.write(interfaceAst.getName().getName());

            if (interfaceAst.getParameters().isPresent()) {
                output.write(LANGLE);
                writeCommaSeparated(interfaceAst.getParameters().get());
                output.write(RANGLE);
            }

            if (!interfaceAst.getAttributes().isEmpty()) {
                output.write(SPACE);
                writeSpaceSeparated(interfaceAst.getAttributes());
            }

            output.println();
            output.append(indentation);
            output.write(LBRACE);
            output.println();

            writeSemicolonTerminatedIndented(interfaceAst.getDeclarations());

            output.append(indentation);
            output.write(RBRACE);

            return null;
        }

        @Override
        public Void visitConfiguration(Configuration component, Void arg) {
            writeComponent(component, NESC_CONFIGURATION);
            return null;
        }

        @Override
        public Void visitModule(Module component, Void arg) {
            writeComponent(component, NESC_MODULE);
            return null;
        }

        @Override
        public Void visitBinaryComponent(BinaryComponent component, Void arg) {
            writeComponent(component, NESC_BINARY_COMPONENT);
            return null;
        }

        @Override
        public Void visitModuleImpl(ModuleImpl impl, Void arg) {
            writeComponentImplementation(impl.getDeclarations());
            return null;
        }

        @Override
        public Void visitConfigurationImpl(ConfigurationImpl impl, Void arg) {
            writeComponentImplementation(impl.getDeclarations());
            return null;
        }

        @Override
        public Void visitBinaryComponentImpl(BinaryComponentImpl impl, Void arg) {
            // implementation of a binary component is empty
            return null;
        }

        @Override
        public Void visitComponentsUses(ComponentsUses componentsUses, Void arg) {
            output.write(NESC_COMPONENTS);
            output.write(SPACE);
            writeCommaSeparated(componentsUses.getComponents());
            output.write(SEMICOLON);
            return null;
        }

        @Override
        public Void visitComponentRef(ComponentRef componentRef, Void arg) {
            if (componentRef.getIsAbstract()) {
                output.write(NESC_NEW);
                output.write(SPACE);
            }

            output.write(componentRef.getName().getName());

            if (componentRef.getIsAbstract()) {
                output.write(LPAREN);
                writeCommaSeparated(componentRef.getArguments());
                output.write(RPAREN);
            }

            if (componentRef.getAlias().isPresent()) {
                output.write(SPACE);
                output.write(NESC_AS);
                output.write(SPACE);
                output.write(componentRef.getAlias().get().getName());
            }

            return null;
        }

        @Override
        public Void visitRequiresInterface(RequiresInterface rp, Void arg) {
            writeUsesProvides(rp, NESC_USES);
            return null;
        }

        @Override
        public Void visitProvidesInterface(ProvidesInterface rp, Void arg) {
            writeUsesProvides(rp, NESC_PROVIDES);
            return null;
        }

        @Override
        public Void visitInterfaceRef(InterfaceRef declaration, Void arg) {
            output.write(NESC_INTERFACE);
            output.write(SPACE);
            output.write(declaration.getName().getName());

            if (declaration.getArguments().isPresent()) {
                output.write(LANGLE);
                writeCommaSeparated(declaration.getArguments().get());
                output.write(RANGLE);
            }

            if (declaration.getAlias().isPresent()) {
                output.write(SPACE);
                output.write(NESC_AS);
                output.write(SPACE);
                output.write(declaration.getAlias().get().getName());
            }

            if (declaration.getGenericParameters().isPresent()) {
                output.write(LBRACK);
                writeCommaSeparated(declaration.getGenericParameters().get());
                output.write(RBRACK);
            }

            if (!declaration.getAttributes().isEmpty()) {
                output.write(SPACE);
                writeSpaceSeparated(declaration.getAttributes());
            }

            output.write(SEMICOLON);
            return null;
        }

        @Override
        public Void visitFunctionDecl(FunctionDecl functionDecl, Void arg) {
            /* Write function header. Attributes must be written before the
               declarator - GCC currently doesn't support the case when they
               appear after it. */
            writeSpaceTerminated(functionDecl.getAttributes());
            writeSpaceTerminated(functionDecl.getModifiers());
            functionDecl.getDeclarator().accept(this, null);

            output.println();

            if (!functionDecl.getOldParms().isEmpty()) {
                writeSemicolonTerminatedIndented(functionDecl.getOldParms());
            }

            output.append(indentation);
            functionDecl.getBody().accept(this, null);

            return null;
        }

        @Override
        public Void visitDataDecl(DataDecl declaration, Void arg) {
            // Specifiers, e.g. 'int', 'command'
            writeSpaceSeparated(declaration.getModifiers());

            // Declarators and attributes of individual entities
            if (!declaration.getDeclarations().isEmpty()) {
                output.write(SPACE);
                writeCommaSeparated(declaration.getDeclarations());
            }

            return null;
        }

        @Override
        public Void visitVariableDecl(VariableDecl declaration, Void arg) {
            // Declarator
            if (declaration.getDeclarator().isPresent()) {
                declaration.getDeclarator().get().accept(this, null);
            }

            // ASM statement
            if (declaration.getAsmStmt().isPresent()) {
                output.write(SPACE);

                final AsmStmt asmStmt = declaration.getAsmStmt().get();
                checkArgument(asmStmt.getAsmOperands1().isEmpty(), "unexpected ASM operands in variable declaration");
                checkArgument(asmStmt.getAsmOperands2().isEmpty(), "unexpected ASM operands in variable declaration");
                checkArgument(asmStmt.getAsmClobbers().isEmpty(), "unexpected ASM clobbers in variable declaration");
                checkArgument(asmStmt.getQualifiers().isEmpty(), "unexpected ASM qualifiers in variable declaration");

                output.write(GCC_ASM);
                output.write(LPAREN);
                asmStmt.getArg1().accept(this, null);
                output.write(RPAREN);
            }

            // Attributes
            if (!declaration.getAttributes().isEmpty()) {
                output.write(SPACE);
                writeSpaceSeparated(declaration.getAttributes());
            }

            // Initializer
            if (declaration.getInitializer().isPresent()) {
                output.write(SPACE);
                output.write(ASSIGN.toString());
                output.write(SPACE);
                declaration.getInitializer().get().accept(this, null);
            }

            return null;
        }

        @Override
        public Void visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            output.write(GCC_EXTENSION);
            output.write(SPACE);
            declaration.getDeclaration().accept(this, null);
            writeTrailingSemicolonIfNeeded(declaration.getDeclaration());
            return null;
        }

        @Override
        public Void visitFieldDecl(FieldDecl declaration, Void arg) {
            // Declarator
            if (declaration.getDeclarator().isPresent()) {
                declaration.getDeclarator().get().accept(this, null);
            }

            // Bit-field
            if (declaration.getBitfield().isPresent()) {
                output.write(SPACE);
                output.write(COLON);
                output.write(SPACE);
                declaration.getBitfield().get().accept(this, null);
            }

            // Attributes
            if (!declaration.getAttributes().isEmpty()) {
                output.write(SPACE);
                writeSpaceSeparated(declaration.getAttributes());
            }

            return null;
        }

        @Override
        public Void visitEllipsisDecl(EllipsisDecl declaration, Void arg) {
            output.write(ELLIPSIS);
            return null;
        }

        @Override
        public Void visitEnumerator(Enumerator declaration, Void arg) {
            writeName(declaration.getName(), declaration.getUniqueName());

            if (declaration.getValue().isPresent()) {
                output.write(SPACE);
                output.write(ASSIGN.toString());
                output.write(SPACE);
                declaration.getValue().get().accept(this, null);
            }

            return null;
        }

        @Override
        public Void visitOldIdentifierDecl(OldIdentifierDecl declaration, Void arg) {
            output.write(declaration.getName());
            return null;
        }

        @Override
        public Void visitAsmDecl(AsmDecl declaration, Void arg) {
            declaration.getAsmStmt().accept(this, null);
            return null;
        }

        @Override
        public Void visitIdentifierDeclarator(IdentifierDeclarator declarator, Void arg) {
            switch (settings.getNameMode()) {
                case USE_NORMAL_NAMES:
                    output.write(declarator.getName());
                    break;
                case USE_UNIQUE_NAMES:
                    final String name = declarator.getUniqueName().isPresent()
                            ? declarator.getUniqueName().get()
                            : declarator.getName();
                    output.write(name);
                    break;
                default:
                    throw new IllegalStateException("unexpected name mode '"
                            + settings.getNameMode() + "'");
            }
            return null;
        }

        @Override
        public Void visitArrayDeclarator(ArrayDeclarator declarator, Void arg) {
            if (declarator.getDeclarator().isPresent()) {
                writeInParenthesesIfPointer(declarator.getDeclarator().get());
            }

            output.write(LBRACK);
            if (declarator.getSize().isPresent()) {
                declarator.getSize().get().accept(this, null);
            }
            output.write(RBRACK);

            return null;
        }

        @Override
        public Void visitFunctionDeclarator(FunctionDeclarator declarator, Void arg) {
            if (declarator.getDeclarator().isPresent()) {
                writeInParenthesesIfPointer(declarator.getDeclarator().get());
            }

            if (declarator.getGenericParameters().isPresent()) {
                output.write(LBRACK);
                writeCommaSeparated(declarator.getGenericParameters().get());
                output.write(RBRACK);
            }

            output.write(LPAREN);
            writeCommaSeparated(declarator.getParameters());
            output.write(RPAREN);

            return null;
        }

        @Override
        public Void visitInterfaceRefDeclarator(InterfaceRefDeclarator declarator, Void arg) {
            final IdentifierDeclarator cmdOrEventNameDecl = (IdentifierDeclarator) declarator.getDeclarator().get();
            final String name = String.format("%s.%s", declarator.getName().getName(),
                    cmdOrEventNameDecl.getName());
            writeName(name, cmdOrEventNameDecl.getUniqueName().orNull());
            return null;
        }

        @Override
        public Void visitPointerDeclarator(PointerDeclarator declarator, Void arg) {
            output.write(ASTERISK);
            if (declarator.getDeclarator().isPresent()) {
                declarator.getDeclarator().get().accept(this, null);
            }
            return null;
        }

        @Override
        public Void visitQualifiedDeclarator(QualifiedDeclarator declarator, Void arg) {
            writeSpaceSeparated(declarator.getModifiers());
            if (declarator.getDeclarator().isPresent()) {
                declarator.getDeclarator().get().accept(this, null);
            }
            return null;
        }

        @Override
        public Void visitTypeParmDecl(TypeParmDecl declaration, Void arg) {
            writeName(declaration.getName(), declaration.getUniqueName());

            if (!declaration.getAttributes().isEmpty()) {
                output.write(SPACE);
                writeSpaceSeparated(declaration.getAttributes());
            }

            return null;
        }

        @Override
        public Void visitEmptyDecl(EmptyDecl declaration, Void arg) {
            // empty declaration is ignored
            return null;
        }

        @Override
        public Void visitErrorDecl(ErrorDecl declaration, Void arg) {
            output.write("<error-decl>");
            return null;
        }

        @Override
        public Void visitCompoundStmt(CompoundStmt stmt, Void arg) {
            output.write(LBRACE);
            output.println();

            // Declarations
            writeSemicolonTerminatedIndented(stmt.getDeclarations());

            // New line if there is at least one declaration and one statement
            if (!stmt.getDeclarations().isEmpty() && !stmt.getStatements().isEmpty()) {
                output.println();
            }

            // Statements
            writeFormattedIndentedStatements(stmt.getStatements());

            output.append(indentation);
            output.write(RBRACE);
            return null;
        }

        @Override
        public Void visitExpressionStmt(ExpressionStmt stmt, Void arg) {
            stmt.getExpression().accept(this, null);
            output.write(SEMICOLON);
            return null;
        }

        @Override
        public Void visitIfStmt(IfStmt stmt, Void arg) {
            // Condition
            output.write(STMT_IF);
            output.write(SPACE);
            output.write(LPAREN);
            stmt.getCondition().accept(this, null);
            output.write(RPAREN);

            // True statement
            writeSubstatement(stmt.getTrueStatement());

            // False statement
            if (stmt.getFalseStatement().isPresent()) {
                final Statement falseStmt = stmt.getFalseStatement().get();

                output.println();
                output.append(indentation);
                output.write(STMT_ELSE);

                if (falseStmt instanceof IfStmt) {
                    output.write(SPACE);
                    falseStmt.accept(this, null);
                } else {
                    writeSubstatement(falseStmt);
                }
            }

            return null;
        }

        @Override
        public Void visitSwitchStmt(SwitchStmt stmt, Void arg) {
            // Condition
            output.write(STMT_SWITCH);
            output.write(SPACE);
            output.write(LPAREN);
            stmt.getCondition().accept(this, null);
            output.write(RPAREN);

            // Statement
            writeSubstatement(stmt.getStatement());

            return null;
        }

        @Override
        public Void visitForStmt(ForStmt stmt, Void arg) {
            // Condition
            output.write(STMT_FOR);
            output.write(SPACE);
            output.write(LPAREN);
            if (stmt.getInitExpression().isPresent()) {
                stmt.getInitExpression().get().accept(this, null);
            }
            output.write(SEMICOLON);
            if (stmt.getConditionExpression().isPresent()) {
                output.write(SPACE);
                stmt.getConditionExpression().get().accept(this, null);
            }
            output.write(SEMICOLON);
            if (stmt.getIncrementExpression().isPresent()) {
                output.write(SPACE);
                stmt.getIncrementExpression().get().accept(this, null);
            }
            output.write(RPAREN);

            // Statement
            writeSubstatement(stmt.getStatement());

            return null;
        }

        @Override
        public Void visitWhileStmt(WhileStmt stmt, Void arg) {
            // Condition
            output.write(STMT_WHILE);
            output.write(SPACE);
            output.write(LPAREN);
            stmt.getCondition().accept(this, null);
            output.write(RPAREN);

            // Statement
            writeSubstatement(stmt.getStatement());

            return null;
        }

        @Override
        public Void visitDoWhileStmt(DoWhileStmt stmt, Void arg) {
            // Statement
            output.write(STMT_DO);
            writeSubstatement(stmt.getStatement());

            // Condition
            output.println();
            output.append(indentation);
            output.write(STMT_WHILE);
            output.write(SPACE);
            output.write(LPAREN);
            stmt.getCondition().accept(this, null);
            output.write(RPAREN);
            output.write(SEMICOLON);

            return null;
        }

        @Override
        public Void visitReturnStmt(ReturnStmt stmt, Void arg) {
            output.write(STMT_RETURN);
            if (stmt.getValue().isPresent()) {
                output.write(SPACE);
                stmt.getValue().get().accept(this, null);
            }
            output.write(SEMICOLON);
            return null;
        }

        @Override
        public Void visitEmptyStmt(EmptyStmt stmt, Void arg) {
            output.write(SEMICOLON);
            return null;
        }

        @Override
        public Void visitBreakStmt(BreakStmt stmt, Void arg) {
            output.write(STMT_BREAK);
            output.write(SEMICOLON);
            return null;
        }

        @Override
        public Void visitContinueStmt(ContinueStmt stmt, Void arg) {
            output.write(STMT_CONTINUE);
            output.write(SEMICOLON);
            return null;
        }

        @Override
        public Void visitGotoStmt(GotoStmt stmt, Void arg) {
            output.write(STMT_GOTO);
            output.write(SPACE);
            stmt.getIdLabel().accept(this, null);
            output.write(SEMICOLON);
            return null;
        }

        @Override
        public Void visitLabeledStmt(LabeledStmt stmt, Void arg) {
            stmt.getLabel().accept(this, null);
            if (stmt.getStatement().isPresent()) {
                output.write(SPACE);
                stmt.getStatement().get().accept(this, null);
            }
            return null;
        }

        @Override
        public Void visitAtomicStmt(AtomicStmt stmt, Void arg) {
            output.write(NESC_ATOMIC);
            output.write(SPACE);
            stmt.getStatement().accept(this, null);
            return null;
        }

        @Override
        public Void visitComputedGotoStmt(ComputedGotoStmt stmt, Void arg) {
            output.write(STMT_GOTO);
            output.write(SPACE);
            stmt.getAddress().accept(this, null);
            output.write(SEMICOLON);
            return null;
        }

        @Override
        public Void visitAsmStmt(AsmStmt stmt, Void arg) {
            output.write(GCC_ASM);
            if (!stmt.getQualifiers().isEmpty()) {
                output.write(SPACE);
                writeSpaceSeparated(stmt.getQualifiers());
            }
            output.write(SPACE);
            output.write(LPAREN);

            stmt.getArg1().accept(this, null);
            output.write(SPACE);
            output.write(COLON);

            if (!stmt.getAsmOperands1().isEmpty()) {
                output.write(SPACE);
                writeCommaSeparated(stmt.getAsmOperands1());
                output.write(SPACE);
            }

            output.write(COLON);

            if (!stmt.getAsmOperands2().isEmpty()) {
                output.write(SPACE);
                writeCommaSeparated(stmt.getAsmOperands2());
            }

            if (!stmt.getAsmClobbers().isEmpty()) {
                output.write(SPACE);
                output.write(COLON);
                output.write(SPACE);
                writeCommaSeparated(stmt.getAsmClobbers());
            }

            output.write(RPAREN);
            output.write(SEMICOLON);
            return null;
        }

        @Override
        public Void visitErrorStmt(ErrorStmt stmt, Void arg) {
            output.write("<error-stmt>");
            return null;
        }

        @Override
        public Void visitCaseLabel(CaseLabel label, Void arg) {
            output.write(LBL_CASE);
            output.write(SPACE);
            label.getLow().accept(this, null);

            if (label.getHigh().isPresent()) {
                output.write(SPACE);
                output.write(ELLIPSIS);
                output.write(SPACE);
                label.getHigh().get().accept(this, null);
            }

            output.write(COLON);
            return null;
        }

        @Override
        public Void visitDefaultLabel(DefaultLabel label, Void arg) {
            output.write(LBL_DEFAULT);
            output.write(COLON);
            return null;
        }

        @Override
        public Void visitIdLabel(IdLabel label, Void arg) {
            output.write(label.getId());
            if (label.getIsColonTerminated()) {
                output.write(COLON);
            }
            return null;
        }

        @Override
        public Void visitEqConnection(EqConnection connection, Void arg) {
            writeConnection(connection.getEndPoint1(), connection.getEndPoint2(),
                    NESC_EQUATE_WIRES);
            return null;
        }

        @Override
        public Void visitRpConnection(RpConnection connection, Void arg) {
            writeConnection(connection.getEndPoint1(), connection.getEndPoint2(),
                    NESC_LINK_WIRES);
            return null;
        }

        @Override
        public Void visitEndPoint(EndPoint endpoint, Void arg) {
            writeDotSeparated(endpoint.getIds());
            return null;
        }

        @Override
        public Void visitParameterisedIdentifier(ParameterisedIdentifier identifier, Void arg) {
            output.write(identifier.getName().getName());

            if (!identifier.getArguments().isEmpty()) {
                output.write(LBRACK);
                writeCommaSeparated(identifier.getArguments());
                output.write(RBRACK);
            }

            return null;
        }

        @Override
        public Void visitAsmOperand(AsmOperand asm, Void arg) {
            if (asm.getWord1().isPresent()) {
                output.write(LBRACK);
                output.write(asm.getWord1().get().getName());
                output.write(RBRACK);
                output.write(SPACE);
            }

            asm.getString().accept(this, null);
            output.write(SPACE);
            output.write(LPAREN);
            asm.getArg1().accept(this, null);
            output.write(RPAREN);

            return null;
        }

        @Override
        public Void visitPlus(Plus expr, Void arg) {
            writeBinary(expr, PLUS);
            return null;
        }

        @Override
        public Void visitMinus(Minus expr, Void arg) {
            writeBinary(expr, MINUS);
            return null;
        }

        @Override
        public Void visitTimes(Times expr, Void arg) {
            writeBinary(expr, TIMES);
            return null;
        }

        @Override
        public Void visitDivide(Divide expr, Void arg) {
            writeBinary(expr, DIVIDE);
            return null;
        }

        @Override
        public Void visitModulo(Modulo expr, Void arg) {
            writeBinary(expr, MODULO);
            return null;
        }

        @Override
        public Void visitLshift(Lshift expr, Void arg) {
            writeBinary(expr, LSHIFT);
            return null;
        }

        @Override
        public Void visitRshift(Rshift expr, Void arg) {
            writeBinary(expr, RSHIFT);
            return null;
        }

        @Override
        public Void visitLeq(Leq expr, Void arg) {
            writeBinary(expr, LEQ);
            return null;
        }

        @Override
        public Void visitGeq(Geq expr, Void arg) {
            writeBinary(expr, GEQ);
            return null;
        }

        @Override
        public Void visitLt(Lt expr, Void arg) {
            writeBinary(expr, LT);
            return null;
        }

        @Override
        public Void visitGt(Gt expr, Void arg) {
            writeBinary(expr, GT);
            return null;
        }

        @Override
        public Void visitEq(Eq expr, Void arg) {
            writeBinary(expr, EQ);
            return null;
        }

        @Override
        public Void visitNe(Ne expr, Void arg) {
            writeBinary(expr, NE);
            return null;
        }

        @Override
        public Void visitBitand(Bitand expr, Void arg) {
            writeBinary(expr, BITAND);
            return null;
        }

        @Override
        public Void visitBitor(Bitor expr, Void arg) {
            writeBinary(expr, BITOR);
            return null;
        }

        @Override
        public Void visitBitxor(Bitxor expr, Void arg) {
            writeBinary(expr, BITXOR);
            return null;
        }

        @Override
        public Void visitAndand(Andand expr, Void arg) {
            writeBinary(expr, ANDAND);
            return null;
        }

        @Override
        public Void visitOror(Oror expr, Void arg) {
            writeBinary(expr, OROR);
            return null;
        }

        @Override
        public Void visitAssign(Assign expr, Void arg) {
            writeBinary(expr, ASSIGN);
            return null;
        }

        @Override
        public Void visitPlusAssign(PlusAssign expr, Void arg) {
            writeBinary(expr, ASSIGN_PLUS);
            return null;
        }

        @Override
        public Void visitMinusAssign(MinusAssign expr, Void arg) {
            writeBinary(expr, ASSIGN_MINUS);
            return null;
        }

        @Override
        public Void visitTimesAssign(TimesAssign expr, Void arg) {
            writeBinary(expr, ASSIGN_TIMES);
            return null;
        }

        @Override
        public Void visitDivideAssign(DivideAssign expr, Void arg) {
            writeBinary(expr, ASSIGN_DIVIDE);
            return null;
        }

        @Override
        public Void visitModuloAssign(ModuloAssign expr, Void arg) {
            writeBinary(expr, ASSIGN_MODULO);
            return null;
        }

        @Override
        public Void visitLshiftAssign(LshiftAssign expr, Void arg) {
            writeBinary(expr, ASSIGN_LSHIFT);
            return null;
        }

        @Override
        public Void visitRshiftAssign(RshiftAssign expr, Void arg) {
            writeBinary(expr, ASSIGN_RSHIFT);
            return null;
        }

        @Override
        public Void visitBitandAssign(BitandAssign expr, Void arg) {
            writeBinary(expr, ASSIGN_BITAND);
            return null;
        }

        @Override
        public Void visitBitorAssign(BitorAssign expr, Void arg) {
            writeBinary(expr, ASSIGN_BITOR);
            return null;
        }

        @Override
        public Void visitBitxorAssign(BitxorAssign expr, Void arg) {
            writeBinary(expr, ASSIGN_BITXOR);
            return null;
        }

        @Override
        public Void visitUnaryMinus(UnaryMinus expr, Void arg) {
            writeUnary(expr, UNARY_MINUS);
            return null;
        }

        @Override
        public Void visitDereference(Dereference expr, Void arg) {
            writeUnary(expr, DEREFERENCE);
            return null;
        }

        @Override
        public Void visitAddressOf(AddressOf expr, Void arg) {
            writeUnary(expr, ADDRESSOF);
            return null;
        }

        @Override
        public Void visitUnaryPlus(UnaryPlus expr, Void arg) {
            writeUnary(expr, UNARY_PLUS);
            return null;
        }

        @Override
        public Void visitBitnot(Bitnot expr, Void arg) {
            writeUnary(expr, BITNOT);
            return null;
        }

        @Override
        public Void visitNot(Not expr, Void arg) {
            writeUnary(expr, NOT);
            return null;
        }

        @Override
        public Void visitAlignofType(AlignofType expr, Void arg) {
            writeExprWithType(expr, OP_ALIGNOF, expr.getAsttype());
            return null;
        }

        @Override
        public Void visitSizeofType(SizeofType expr, Void arg) {
            writeExprWithType(expr, OP_SIZEOF, expr.getAsttype());
            return null;
        }

        @Override
        public Void visitOffsetof(Offsetof expr, Void arg) {
            writeLeftParentheses(expr);

            output.write(MACRO_OFFSETOF);
            output.write(LPAREN);
            expr.getTypename().accept(this, null);
            output.write(COMMA);
            output.write(SPACE);
            writeDotSeparated(expr.getFieldlist());
            output.write(RPAREN);

            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitSizeofExpr(SizeofExpr expr, Void arg) {
            writeLetterUnary(expr, OP_SIZEOF);
            return null;
        }

        @Override
        public Void visitAlignofExpr(AlignofExpr expr, Void arg) {
            writeLetterUnary(expr, OP_ALIGNOF);
            return null;
        }

        @Override
        public Void visitRealpart(Realpart expr, Void arg) {
            writeLetterUnary(expr, OP_REALPART);
            return null;
        }

        @Override
        public Void visitImagpart(Imagpart expr, Void arg) {
            writeLetterUnary(expr, OP_IMAGPART);
            return null;
        }

        @Override
        public Void visitArrayRef(ArrayRef expr, Void arg) {
            writeLeftParentheses(expr);

            expr.getArray().accept(this, null);
            output.write(LBRACK);
            writeCommaSeparated(expr.getIndex());
            output.write(RBRACK);

            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitErrorExpr(ErrorExpr expr, Void arg) {
            writeLeftParentheses(expr);
            output.write("<syntax-error>");
            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitComma(Comma expr, Void arg) {
            writeLeftParentheses(expr);
            writeCommaSeparated(expr.getExpressions());
            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitLabelAddress(LabelAddress expr, Void arg) {
            writeLeftParentheses(expr);

            output.write(LABELADDRESS.toString());
            output.write(expr.getIdLabel().getId());

            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitConditional(Conditional expr, Void arg) {
            writeLeftParentheses(expr);

            // FIXME when true-expr is absent
            expr.getCondition().accept(this, null);
            output.write(" ? ");
            if (expr.getOnTrueExp().isPresent()) {
                expr.getOnTrueExp().get().accept(this, null);
            } else {
                output.write(" <absent> ");
            }
            output.write(" : ");
            expr.getOnFalseExp().accept(this, null);

            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitIdentifier(Identifier expr, Void arg) {
            writeLeftParentheses(expr);

            switch (settings.getNameMode()) {
                case USE_NORMAL_NAMES:
                    output.write(expr.getName());
                    break;
                case USE_UNIQUE_NAMES:
                    output.write(expr.getUniqueName().or(expr.getName()));
                    break;
                default:
                    throw new RuntimeException("unexpected name mode '"
                            + settings.getNameMode() + "'");
            }

            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitCompoundExpr(CompoundExpr expr, Void arg) {
            writeLeftParentheses(expr);

            output.write(LPAREN);
            expr.getStatement().accept(this, null);
            output.write(RPAREN);

            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitIntegerCst(IntegerCst expr, Void arg) {
            writeConstant(expr);
            return null;
        }

        @Override
        public Void visitFloatingCst(FloatingCst expr, Void arg) {
            writeConstant(expr);
            return null;
        }

        @Override
        public Void visitCharacterCst(CharacterCst expr, Void arg) {
            writeLeftParentheses(expr);

            output.write(APOSTROPHE);
            output.write(expr.getString());
            output.write(APOSTROPHE);

            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitStringCst(StringCst expr, Void arg) {
            writeLeftParentheses(expr);

            output.write(QUOTATION_MARK);
            output.write(expr.getString());
            output.write(QUOTATION_MARK);

            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitStringAst(StringAst expr, Void arg) {
            writeLeftParentheses(expr);
            writeSpaceSeparated(expr.getStrings());
            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitFunctionCall(FunctionCall expr, Void arg) {
            writeFunctionCall(expr);
            return null;
        }

        @Override
        public Void visitUniqueCall(UniqueCall expr, Void arg) {
            writeConstantFunctionCall(expr);
            return null;
        }

        @Override
        public Void visitUniqueNCall(UniqueNCall expr, Void arg) {
            writeConstantFunctionCall(expr);
            return null;
        }

        @Override
        public Void visitUniqueCountCall(UniqueCountCall expr, Void arg) {
            writeConstantFunctionCall(expr);
            return null;
        }

        @Override
        public Void visitFieldRef(FieldRef expr, Void arg) {
            if (expr.getArgument() instanceof Dereference) {
                writeLeftParentheses(expr);

                final Dereference dereference = (Dereference) expr.getArgument();
                dereference.getArgument().accept(this, null);
                output.write(ARROW.toString());
                output.write(expr.getFieldName());

                writeRightParentheses(expr);
            } else {
                writeFieldLikeExpr(expr, expr.getFieldName());
            }
            return null;
        }

        @Override
        public Void visitInterfaceDeref(InterfaceDeref expr, Void arg) {
            writeFieldLikeExpr(expr, expr.getMethodName());
            return null;
        }

        @Override
        public Void visitComponentDeref(ComponentDeref expr, Void arg) {
            writeFieldLikeExpr(expr, expr.getFieldName());
            return null;
        }

        @Override
        public Void visitPreincrement(Preincrement expr, Void arg) {
            writePreincrement(expr, INCREMENT);
            return null;
        }

        @Override
        public Void visitPredecrement(Predecrement expr, Void arg) {
            writePreincrement(expr, DECREMENT);
            return null;
        }

        @Override
        public Void visitPostincrement(Postincrement expr, Void arg) {
            writePostincrement(expr, INCREMENT);
            return null;
        }

        @Override
        public Void visitPostdecrement(Postdecrement expr, Void arg) {
            writePostincrement(expr, DECREMENT);
            return null;
        }

        @Override
        public Void visitCast(Cast expr, Void arg) {
            writeCastLikeExpr(expr, expr.getAsttype(), expr.getArgument());
            return null;
        }

        @Override
        public Void visitCastList(CastList expr, Void arg) {
            writeCastLikeExpr(expr, expr.getAsttype(), expr.getInitExpr());
            return null;
        }

        @Override
        public Void visitInitList(InitList expr, Void arg) {
            writeLeftParentheses(expr);

            output.write(LBRACE);
            writeCommaSeparated(expr.getArguments());
            output.write(RBRACE);

            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitInitSpecific(InitSpecific expr, Void arg) {
            writeLeftParentheses(expr);

            writeSpaceSeparated(expr.getDesignator());
            output.write(SPACE);
            output.write(ASSIGN.toString());
            output.write(SPACE);
            expr.getInitExpr().accept(this, null);

            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitTypeArgument(TypeArgument expr, Void arg) {
            writeLeftParentheses(expr);
            expr.getAsttype().accept(this, null);
            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitGenericCall(GenericCall expr, Void arg) {
            writeLeftParentheses(expr);

            expr.getName().accept(this, null);
            output.write(LBRACK);
            writeCommaSeparated(expr.getArguments());
            output.write(RBRACK);

            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitExtensionExpr(ExtensionExpr expr, Void arg) {
            writeLeftParentheses(expr);

            output.write(GCC_EXTENSION);
            output.write(SPACE);
            expr.getArgument().accept(this, null);

            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitAstType(AstType astType, Void arg) {
            writeSpaceSeparated(astType.getQualifiers());
            if (astType.getDeclarator().isPresent()) {
                astType.getDeclarator().get().accept(this, null);
            }
            return null;
        }

        @Override
        public Void visitTypeofType(TypeofType typeofType, Void arg) {
            writeTypeof(typeofType.getAsttype());
            return null;
        }

        @Override
        public Void visitTypeofExpr(TypeofExpr typeofExpr, Void arg) {
            writeTypeof(typeofExpr.getExpression());
            return null;
        }

        @Override
        public Void visitTypename(Typename typename, Void arg) {
            writeName(typename.getName(), typename.getUniqueName());
            return null;
        }

        @Override
        public Void visitComponentTyperef(ComponentTyperef typename, Void arg) {
            switch (settings.getNameMode()) {
                case USE_NORMAL_NAMES:
                    output.write(typename.getName());
                    output.write(DOT.toString());
                    output.write(typename.getTypeName());
                    break;
                case USE_UNIQUE_NAMES:
                    output.write(typename.getUniqueName());
                    break;
                default:
                    throw new IllegalStateException("unexpected name mode '"
                            + settings.getNameMode() + "'");
            }
            return null;
        }

        @Override
        public Void visitAttributeRef(AttributeRef tagRef, Void arg) {
            writeTag(tagRef, TAG_STRUCT, true);
            return null;
        }

        @Override
        public Void visitStructRef(StructRef tagRef, Void arg) {
            writeTag(tagRef, TAG_STRUCT, false);
            return null;
        }

        @Override
        public Void visitUnionRef(UnionRef tagRef, Void arg) {
            writeTag(tagRef, TAG_UNION, false);
            return null;
        }

        @Override
        public Void visitNxStructRef(NxStructRef tagRef, Void arg) {
            writeTag(tagRef, TAG_NX_STRUCT, false);
            return null;
        }

        @Override
        public Void visitNxUnionRef(NxUnionRef tagRef, Void arg) {
            writeTag(tagRef, TAG_NX_UNION, false);
            return null;
        }

        @Override
        public Void visitEnumRef(EnumRef tagRef, Void arg) {
            writeTag(tagRef, TAG_ENUM, false);
            return null;
        }

        @Override
        public Void visitRid(Rid rid, Void arg) {
            output.write(rid.getId().getName());
            return null;
        }

        @Override
        public Void visitQualifier(Qualifier qualifier, Void arg) {
            output.write(qualifier.getId().getName());
            return null;
        }

        @Override
        public Void visitNescAttribute(NescAttribute nescAttribute, Void arg) {
            output.write(AT);
            output.write(nescAttribute.getName().getName());
            output.write(LPAREN);
            nescAttribute.getValue().accept(this, null);
            output.write(RPAREN);
            return null;
        }

        @Override
        public Void visitGccAttribute(GccAttribute gccAttribute, Void arg) {
            output.write(GCC_ATTRIBUTE);
            output.write(LPAREN);
            output.write(LPAREN);

            output.write(gccAttribute.getName().getName());
            if (gccAttribute.getArguments().isPresent()) {
                output.write(LPAREN);
                // FIXME don't use unique names while printing arguments for GCC attributes
                writeCommaSeparated(gccAttribute.getArguments().get());
                output.write(RPAREN);
            }

            output.write(RPAREN);
            output.write(RPAREN);
            return null;
        }

        @Override
        public Void visitDesignateField(DesignateField designator, Void arg) {
            output.write(DOT.toString());
            output.write(designator.getName());
            return null;
        }

        @Override
        public Void visitDesignateIndex(DesignateIndex designator, Void arg) {
            output.write(LBRACK);

            designator.getFirst().accept(this, null);
            if (designator.getLast().isPresent()) {
                output.write(SPACE);
                output.write(ELLIPSIS);
                output.write(SPACE);
                designator.getLast().get().accept(this, null);
            }

            output.write(RBRACK);
            return null;
        }

        @Override
        public Void visitWord(Word word, Void arg) {
            output.println(word.getName());
            return null;
        }

        @Override
        public Void visitNode(Node node, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'Node' visited");
        }

        @Override
        public Void visitDeclarator(Declarator declarator, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'Declarator' visited");
        }

        @Override
        public Void visitImplicitDecl(ImplicitDecl declaration, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'ImplicitDecl' visited");
        }

        @Override
        public Void visitExpression(Expression expr, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'Expression' visited");
        }

        @Override
        public Void visitDesignator(Designator designator, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'Designator' visited");
        }

        @Override
        public Void visitStatement(Statement stmt, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'Statement' visited");
        }

        @Override
        public Void visitComparison(Comparison expr, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'Comparison' visited");
        }

        @Override
        public Void visitRpInterface(RpInterface rp, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'RpInterface' visited");
        }

        @Override
        public Void visitDeclaration(Declaration declaration, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'Declaration' visited");
        }

        @Override
        public Void visitComponent(Component component, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'Component' visited");
        }

        @Override
        public Void visitConditionalStmt(ConditionalStmt component, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'ConditionalStmt' visited");
        }

        @Override
        public Void visitConjugate(Conjugate expr, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'Conjuage' visited");
        }

        @Override
        public Void visitNescDecl(NescDecl declaration, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'NescDecl' visited");
        }

        @Override
        public Void visitLabel(Label label, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'Label' visited");
        }

        @Override
        public Void visitImplementation(Implementation impl, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'Implementation' visited");
        }

        @Override
        public Void visitConnection(Connection impl, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'Connection' visited");
        }

        @Override
        public Void visitTargetAttribute(TargetAttribute attribute, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'TargetAttribute' visited");
        }

        @Override
        public Void visitIncrement(Increment expr, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'Increment' visited");
        }

        @Override
        public Void visitUnary(Unary expr, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'Unary' visited");
        }

        @Override
        public Void visitBinary(Binary expr, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'Binary' visited");
        }

        @Override
        public Void visitTagRef(TagRef expr, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'TagRef' visited");
        }

        @Override
        public Void visitConstantFunctionCall(ConstantFunctionCall expr, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'ConstantFunctionCall' visited");
        }

        @Override
        public Void visitTypeElement(TypeElement typeElement, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'TypeElement' visited");
        }

        @Override
        public Void visitLexicalCst(LexicalCst expr, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'LexicalCst' visited");
        }

        @Override
        public Void visitAttribute(Attribute attribute, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'Attribute' visited");
        }

        @Override
        public Void visitNestedDeclarator(NestedDeclarator declarator, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'NestedDeclarator' visited");
        }

        @Override
        public Void visitAssignment(Assignment expr, Void arg) {
            throw new RuntimeException("unexpected AST node of class 'Assignment' visited");
        }

        private void writeFormattedIndentedDeclarations(List<? extends Declaration> declarations) {
            increaseIndentation();
            writeFormattedDeclarations(declarations);
            decreaseIndentation();
        }

        private void writeFormattedIndentedStatements(List<? extends Statement> stmts) {
            increaseIndentation();
            writeFormattedStatements(stmts);
            decreaseIndentation();
        }

        private void writeFormattedDeclarations(List<? extends Declaration> declarations) {
            writeFormatted(declarations, IS_MULTILINE_DECLARATION, DECLARATION_TERMINATOR_FUN);
        }

        private void writeFormattedStatements(List<? extends Statement> stmts) {
            writeFormatted(stmts, IS_MULTILINE_STMT, EMPTY_TERMINATOR_FUN);
        }

        private <T extends Node> void writeFormatted(List<? extends T> astNodes, Predicate<T> isMultiline,
                Function<? super T, String> terminator) {
            boolean first = true;
            boolean previousMultiline = false;

            for (T astNode : astNodes) {
                if (!first && (previousMultiline || isMultiline.apply(astNode))) {
                    output.println();
                }

                output.append(indentation);
                astNode.accept(this, null);
                output.write(terminator.apply(astNode));
                output.println();

                first = false;
                previousMultiline = isMultiline.apply(astNode);
            }
        }

        /**
         * Check if a semicolon shall be written after the given declaration as
         * a top-level declaration but is not written by the visitor after
         * visiting it.
         *
         * @param declaration Declaration to check for missing semicolon.
         * @return <code>true</code> if and only if the given declaration as
         *         a top-level declaration shall be terminated with a semicolon
         *         but the visitor doesn't append a semicolon during visit of
         *         that node.
         */
        private boolean needsTrailingSemicolon(Declaration declaration) {
            return declaration instanceof DataDecl;
        }

        /**
         * Method that indents substatements other than compound statements
         * to emphasize if they are part of larger statements.
         *
         * @param substmt Statement that is part of another statement.
         */
        private void writeSubstatement(Statement substmt) {
            output.println();

            if (substmt instanceof CompoundStmt) {
                output.append(indentation);
                substmt.accept(this, null);
            } else {
                increaseIndentation();
                output.append(indentation);
                substmt.accept(this, null);
                decreaseIndentation();
            }
        }

        private void writeConstantFunctionCall(ConstantFunctionCall cstFunCall) {
            switch (settings.getUniqueMode()) {
                case OUTPUT_CALLS:
                    writeFunctionCall(cstFunCall);
                    break;
                case OUTPUT_VALUES:
                    output.write(cstFunCall.getValue().toString());
                    break;
                default:
                    throw new RuntimeException("unexpected constant functions mode '"
                            + settings.getUniqueMode() + "'");
            }
        }

        private void writeUsesProvides(RpInterface rp, String rpKeyword) {
            output.write(rpKeyword);

            if (rp.getDeclarations().size() == 1) {
                output.write(SPACE);
                rp.getDeclarations().getFirst().accept(this, null);
                writeTrailingSemicolonIfNeeded(rp.getDeclarations().getFirst());
            } else {
                output.println();
                output.append(indentation);
                output.write(LBRACE);
                output.println();

                writeFormattedIndentedDeclarations(rp.getDeclarations());

                output.append(indentation);
                output.write(RBRACE);
            }
        }

        private void writeComponent(Component component, String componentKeyword) {
            writeComponentHeader(component, componentKeyword);
            output.println();
            output.append(indentation);
            writeComponentSpecification(component.getDeclarations());
            component.getImplementation().accept(this, null);
        }

        private void writeComponentHeader(Component component, String componentKeyword) {
            if (component.getIsAbstract()) {
                output.write(NESC_GENERIC);
                output.write(SPACE);
            }

            output.write(componentKeyword);
            output.write(SPACE);
            output.write(component.getName().getName());

            if (component.getIsAbstract()) {
                output.write(LPAREN);
                if (component.getParameters().isPresent()) {
                    writeCommaSeparated(component.getParameters().get());
                }
                output.write(RPAREN);
            }

            if (!component.getAttributes().isEmpty()) {
                output.write(SPACE);
                writeSpaceSeparated(component.getAttributes());
            }
        }

        private void writeComponentSpecification(LinkedList<Declaration> specDeclarations) {
            output.write(LBRACE);
            output.println();

            writeSemicolonTerminatedIndented(specDeclarations);
            output.append(indentation);

            output.write(RBRACE);
        }

        private void writeComponentImplementation(LinkedList<Declaration> implDeclarations) {
            output.write(NESC_IMPLEMENTATION);
            output.println();
            output.append(indentation);
            output.write(LBRACE);
            output.println();

            writeFormattedIndentedDeclarations(implDeclarations);

            output.append(indentation);
            output.write(RBRACE);
        }

        private void writeTopLevelDeclarations(List<? extends Declaration> declarations) {
            writeFormattedDeclarations(declarations);
        }

        private void writeTrailingSemicolonIfNeeded(Declaration declaration) {
            if (needsTrailingSemicolon(declaration)) {
                output.write(SEMICOLON);
            }
        }

        private void writeConnection(EndPoint leftEndpoint, EndPoint rightEndpoint,
                String connectionToken) {
            leftEndpoint.accept(this, null);
            output.write(SPACE);
            output.write(connectionToken);
            output.write(SPACE);
            rightEndpoint.accept(this, null);
        }

        private void writeTag(TagRef tagRef, String tagKeyword, boolean at) {
            // Write the tag keyword
            output.write(tagKeyword);

            // Write '@' if it is necessary
            if (at) {
                output.write(SPACE);
                output.write(AT);
            }

            // Write the name if the tag is not anonymous
            final Optional<Word> tag = Optional.fromNullable(tagRef.getName());
            if (tag.isPresent()) {
                if (!at) {
                    output.write(SPACE);
                }
                writeName(tag.get().getName(), tagRef.getUniqueName().get());
            }

            // Write attributes
            if (!tagRef.getAttributes().isEmpty()) {
                output.write(SPACE);
                writeSpaceSeparated(tagRef.getAttributes());
            }

            // Write fields
            switch (tagRef.getSemantics()) {
                case PREDEFINITION:
                case DEFINITION:
                    output.write(SPACE);
                    if (tagRef instanceof EnumRef) {
                        writeCommaSeparatedInBraces(tagRef.getFields());
                    } else {
                        writeSemicolonTerminatedInBraces(tagRef.getFields());
                    }
                    break;
                case OTHER:
                    // do nothing as this is not a definition
                    break;
            }
        }

        private void writeName(String name, String uniqueName) {
            switch (settings.getNameMode()) {
                case USE_NORMAL_NAMES:
                    output.write(name);
                    break;
                case USE_UNIQUE_NAMES:
                    output.write(uniqueName);
                    break;
                default:
                    throw new IllegalStateException("unexpected name mode '"
                            + settings.getNameMode() + "'");
            }
        }

        private void writeTypeof(Node typeofParam) {
            output.write(OP_TYPEOF);
            output.write(LPAREN);
            typeofParam.accept(this, null);
            output.write(RPAREN);
        }

        private void writeBinary(Binary binary, Tokens.BinaryOp op) {
            writeLeftParentheses(binary);
            binary.getLeftArgument().accept(this, null);

            output.write(SPACE);
            output.write(op.toString());
            output.write(SPACE);

            binary.getRightArgument().accept(this, null);
            writeRightParentheses(binary);
        }

        private void writeUnary(Unary unary, Tokens.UnaryOp op) {
            writeLeftParentheses(unary);

            output.write(op.toString());
            unary.getArgument().accept(this, null);

            writeRightParentheses(unary);
        }

        private void writeLetterUnary(Unary unary, String op) {
            writeLeftParentheses(unary);

            output.write(op);
            output.write(SPACE);
            unary.getArgument().accept(this, null);

            writeRightParentheses(unary);
        }

        private void writeExprWithType(Expression expr, String op, AstType astType) {
            writeLeftParentheses(expr);

            output.write(op);
            output.write(LPAREN);
            astType.accept(this, null);
            output.write(RPAREN);

            writeRightParentheses(expr);
        }

        private void writeConstant(LexicalCst cst) {
            writeLeftParentheses(cst);
            output.write(cst.getString());
            writeRightParentheses(cst);
        }

        private void writePreincrement(Unary unary, Tokens.UnaryOp op) {
            writeLeftParentheses(unary);
            output.write(op.toString());
            unary.getArgument().accept(this, null);
            writeRightParentheses(unary);
        }

        private void writePostincrement(Unary unary, Tokens.UnaryOp op) {
            writeLeftParentheses(unary);
            unary.getArgument().accept(this, null);
            output.write(op.toString());
            writeRightParentheses(unary);
        }

        private void writeCastLikeExpr(Expression expr, AstType type, Expression subExpr) {
            writeLeftParentheses(expr);

            output.write(LPAREN);
            type.accept(this, null);
            output.write(RPAREN);
            output.write(SPACE);
            subExpr.accept(this, null);

            writeRightParentheses(expr);
        }

        private void writeFieldLikeExpr(Unary unary, String fieldName) {
            writeLeftParentheses(unary);

            unary.getArgument().accept(this, null);
            output.write(DOT.toString());
            output.write(fieldName);

            writeRightParentheses(unary);
        }

        private void writeFieldLikeExpr(Unary unary, Word fieldName) {
            writeLeftParentheses(unary);

            unary.getArgument().accept(this, null);
            output.write(DOT.toString());
            output.write(fieldName.getName());

            writeRightParentheses(unary);
        }

        private void writeFunctionCall(FunctionCall expr) {
            writeLeftParentheses(expr);
            final AstType vaArgCall = expr.getVaArgCall();

            if (vaArgCall == null) {
                // Call keyword
                final Optional<String> callKeyword = Optional.fromNullable(CALL_KEYWORDS.get(expr.getCallKind()));
                if (callKeyword.isPresent()) {
                    output.write(callKeyword.get());
                    output.write(" ");
                }

                // Function identifier and parameters
                expr.getFunction().accept(this, null);
                output.write(LPAREN);
                writeCommaSeparated(expr.getArguments());
                output.write(RPAREN);
            } else {
                output.write(GCC_BUILTIN_VA_ARG);
                output.write(LPAREN);
                writeCommaSeparated(expr.getArguments());
                output.write(COMMA);
                output.write(SPACE);
                vaArgCall.accept(this, null);
                output.write(RPAREN);
            }

            writeRightParentheses(expr);
        }

        private void writeDotSeparated(List<? extends Node> nodes) {
            writeSeparated(nodes, DOT.toString());
        }

        private void writeCommaSeparated(List<? extends Node> nodes) {
            writeSeparated(nodes, COMMA_WITH_SPACE);
        }

        private void writeSpaceSeparated(List<? extends Node> nodes) {
            writeSeparated(nodes, SPACE);
        }

        private void writeSpaceTerminated(List<? extends Node> nodes) {
            writeTerminated(nodes, SPACE);
        }

        private void writeSemicolonTerminatedIndented(List<? extends Node> nodes) {
            writeTerminatedIndented(nodes, SEMICOLON);
        }

        private void writeCommaSeparatedInBraces(List<? extends Node> nodes) {
            writeSeparatedInBraces(nodes, COMMA);
        }

        private void writeSemicolonTerminatedInBraces(List<? extends Node> nodes) {
            writeTerminatedInBraces(nodes, SEMICOLON);
        }

        private void writeSeparated(List<? extends Node> nodes, String separator) {
            boolean first = true;

            for (Node node : nodes) {
                if (!first) {
                    output.write(separator);
                } else {
                    first = false;
                }

                node.accept(this, null);
            }
        }

        private void writeTerminated(List<? extends Node> nodes, String terminator) {
            for (Node node : nodes) {
                node.accept(this, null);
                output.write(terminator);
            }
        }

        private void writeTerminatedIndented(List<? extends Node> nodes, String terminator) {
            increaseIndentation();

            for (Node node : nodes) {
                output.append(indentation);
                node.accept(this, null);
                output.write(terminator);
                output.println();
            }

            decreaseIndentation();
        }

        private void writeTerminatedInBraces(List<? extends Node> nodes, String terminator) {
            output.write(LBRACE);

            if (!nodes.isEmpty()) {
                output.println();
                writeTerminatedIndented(nodes, terminator);
                output.append(indentation);
            }

            output.write(RBRACE);
        }

        private void writeSeparatedInBraces(List<? extends Node> nodes, String separator) {
            output.write(LBRACE);

            if (!nodes.isEmpty()) {
                output.println();
                increaseIndentation();

                boolean first = true;
                for (Node node : nodes) {
                    if (!first) {
                        output.write(separator);
                        output.println();
                    } else {
                        first = false;
                    }
                    output.append(indentation);
                    node.accept(this, null);
                }

                output.println();
                decreaseIndentation();
                output.append(indentation);
            }

            output.write(RBRACE);
        }

        private void writeLeftParentheses(Expression expression) {
            writeRepeated(LPAREN, Optional.fromNullable(expression.getParenthesesCount()).or(0));
        }

        private void writeRightParentheses(Expression expression) {
            writeRepeated(RPAREN, Optional.fromNullable(expression.getParenthesesCount()).or(0));
        }

        private void writeRepeated(String toRepeat, int count) {
            for (int i = 0; i < count; ++i) {
                output.write(toRepeat);
            }
        }

        private boolean isPointerDeclarator(Declarator declarator) {
            Optional<Declarator> optDeclarator = Optional.of(declarator);
            while (optDeclarator.isPresent() && optDeclarator.get() instanceof QualifiedDeclarator) {
                optDeclarator = ((QualifiedDeclarator) optDeclarator.get()).getDeclarator();
            }
            return optDeclarator.isPresent() && optDeclarator.get() instanceof PointerDeclarator;
        }

        private void writeInParenthesesIfPointer(Declarator declarator) {
            final boolean inParentheses = isPointerDeclarator(declarator);

            if (inParentheses) {
                output.write(LPAREN);
            }
            declarator.accept(this, null);
            if (inParentheses) {
                output.write(RPAREN);
            }
        }
    }

    /**
     * Interface with operations that involve building a code writer.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private interface PrivateBuilder {
        WriteSettings getSettings();
        PrintWriter buildWriter() throws IOException;
        String buildIndentationStep();
    }

    /**
     * Class with skeleton implementation of the private builder interface.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static abstract class AbstractPrivateBuilder implements PrivateBuilder {
        private final WriteSettings settings;

        private AbstractPrivateBuilder(WriteSettings settings) {
            checkNotNull(settings, "write settings cannot be null");
            this.settings = settings;
        }

        @Override
        public WriteSettings getSettings() {
            return settings;
        }

        @Override
        public String buildIndentationStep() {
            final String indentChar;

            switch (settings.getIndentationType()) {
                case TABS:
                    indentChar = TAB;
                    break;
                case SPACES:
                    indentChar = SPACE;
                    break;
                default:
                    throw new RuntimeException("unexpected indentation type '"
                            + settings.getIndentationType() + "'");
            }

            return new String(new char[settings.getIndentationSize()])
                    .replaceAll("\0", indentChar);
        }

        protected PrintWriter createPrintWriter(OutputStream stream) {
            return new PrintWriter(new OutputStreamWriter(stream, settings.getCharset()));
        }
    }

    /**
     * Builder that builds the object to write to an output stream.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class OutputStreamPrivateBuilder extends AbstractPrivateBuilder {
        private final OutputStream targetStream;

        private OutputStreamPrivateBuilder(OutputStream targetStream, WriteSettings settings) {
            super(settings);
            checkNotNull(targetStream, "the target stream cannot be null");
            this.targetStream = targetStream;
        }

        @Override
        public PrintWriter buildWriter() {
            return createPrintWriter(targetStream);
        }
    }

    /**
     * Builder that builds the object to write to a file.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class FilePrivateBuilder extends AbstractPrivateBuilder {
        private final String fileName;

        private FilePrivateBuilder(String fileName, WriteSettings settings) {
            super(settings);
            checkNotNull(fileName, "name of the file cannot be null");
            this.fileName = fileName;
        }

        @Override
        public PrintWriter buildWriter() throws IOException {
            final FileOutputStream fileOutStream = new FileOutputStream(fileName);
            fileOutStream.getChannel().truncate(0);
            return createPrintWriter(fileOutStream);
        }
    }
}
