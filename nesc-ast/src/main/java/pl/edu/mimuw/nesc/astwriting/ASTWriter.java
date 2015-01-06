package pl.edu.mimuw.nesc.astwriting;

import com.google.common.base.Optional;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import pl.edu.mimuw.nesc.ast.gen.*;

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

    public void write(Node node) {
        if (node instanceof Declaration) {
            writeVisitor.writeTopLevelDeclaration((Declaration) node);
        } else {
            node.accept(writeVisitor, null);
        }
    }

    public void write(List<? extends Declaration> topLevelDeclarations) {
        writeVisitor.writeTopLevelDeclarations(topLevelDeclarations);
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
            output.append(NESC_INTERFACE);
            output.append(SPACE);
            output.append(interfaceAst.getName().getName());

            if (interfaceAst.getParameters().isPresent()) {
                output.append(LANGLE);
                writeCommaSeparated(interfaceAst.getParameters().get());
                output.append(RANGLE);
            }

            if (!interfaceAst.getAttributes().isEmpty()) {
                output.append(SPACE);
                writeSpaceSeparated(interfaceAst.getAttributes());
            }

            output.println();
            output.append(indentation);
            output.append(LBRACE);
            output.println();

            writeSemicolonTerminatedIndented(interfaceAst.getDeclarations());

            output.append(indentation);
            output.append(RBRACE);

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
            output.append(NESC_COMPONENTS);
            output.append(SPACE);
            writeCommaSeparated(componentsUses.getComponents());
            output.append(SEMICOLON);
            return null;
        }

        @Override
        public Void visitComponentRef(ComponentRef componentRef, Void arg) {
            if (componentRef.getIsAbstract()) {
                output.append(NESC_NEW);
                output.append(SPACE);
            }

            output.append(componentRef.getName().getName());

            if (componentRef.getIsAbstract()) {
                output.append(LPAREN);
                writeCommaSeparated(componentRef.getArguments());
                output.append(RPAREN);
            }

            if (componentRef.getAlias().isPresent()) {
                output.append(SPACE);
                output.append(NESC_AS);
                output.append(SPACE);
                output.append(componentRef.getAlias().get().getName());
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
            output.append(NESC_INTERFACE);
            output.append(SPACE);
            output.append(declaration.getName().getName());

            if (declaration.getArguments().isPresent()) {
                output.append(LANGLE);
                writeCommaSeparated(declaration.getArguments().get());
                output.append(RANGLE);
            }

            if (declaration.getAlias().isPresent()) {
                output.append(SPACE);
                output.append(NESC_AS);
                output.append(SPACE);
                output.append(declaration.getAlias().get().getName());
            }

            if (declaration.getGenericParameters().isPresent()) {
                output.append(LBRACK);
                writeCommaSeparated(declaration.getGenericParameters().get());
                output.append(RBRACK);
            }

            if (!declaration.getAttributes().isEmpty()) {
                output.append(SPACE);
                writeSpaceSeparated(declaration.getAttributes());
            }

            output.append(SEMICOLON);
            return null;
        }

        @Override
        public Void visitFunctionDecl(FunctionDecl functionDecl, Void arg) {
            writeSpaceTerminated(functionDecl.getModifiers());
            functionDecl.getDeclarator().accept(this, null);

            if (!functionDecl.getAttributes().isEmpty()) {
                output.append(SPACE);
                writeSpaceSeparated(functionDecl.getAttributes());
            }

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
                output.append(SPACE);
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
                output.append(SPACE);
                declaration.getAsmStmt().get().accept(this, null);
            }

            // Attributes
            if (!declaration.getAttributes().isEmpty()) {
                output.append(SPACE);
                writeSpaceSeparated(declaration.getAttributes());
            }

            // Initializer
            if (declaration.getInitializer().isPresent()) {
                output.append(SPACE);
                output.append(ASSIGN.toString());
                output.append(SPACE);
                declaration.getInitializer().get().accept(this, null);
            }

            return null;
        }

        @Override
        public Void visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            output.append(GCC_EXTENSION);
            output.append(SPACE);
            declaration.getDeclaration().accept(this, null);
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
                output.append(SPACE);
                output.append(COLON);
                output.append(SPACE);
                declaration.getBitfield().get().accept(this, null);
            }

            // Attributes
            if (!declaration.getAttributes().isEmpty()) {
                output.append(SPACE);
                writeSpaceSeparated(declaration.getAttributes());
            }

            return null;
        }

        @Override
        public Void visitEllipsisDecl(EllipsisDecl declaration, Void arg) {
            output.append(ELLIPSIS);
            return null;
        }

        @Override
        public Void visitEnumerator(Enumerator declaration, Void arg) {
            writeName(declaration.getName(), declaration.getUniqueName());

            if (declaration.getValue() != null) {
                output.append(SPACE);
                output.append(ASSIGN.toString());
                output.append(SPACE);
                declaration.getValue().accept(this, null);
            }

            return null;
        }

        @Override
        public Void visitOldIdentifierDecl(OldIdentifierDecl declaration, Void arg) {
            output.append(declaration.getName());
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
                    output.append(declarator.getName());
                    break;
                case USE_UNIQUE_NAMES:
                    final String name = declarator.getUniqueName().isPresent()
                            ? declarator.getUniqueName().get()
                            : declarator.getName();
                    output.append(name);
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

            output.append(LBRACK);
            if (declarator.getSize().isPresent()) {
                declarator.getSize().get().accept(this, null);
            }
            output.append(RBRACK);

            return null;
        }

        @Override
        public Void visitFunctionDeclarator(FunctionDeclarator declarator, Void arg) {
            if (declarator.getDeclarator().isPresent()) {
                writeInParenthesesIfPointer(declarator.getDeclarator().get());
            }

            if (declarator.getGenericParameters().isPresent()) {
                output.append(LBRACK);
                writeCommaSeparated(declarator.getGenericParameters().get());
                output.append(RBRACK);
            }

            output.append(LPAREN);
            writeCommaSeparated(declarator.getParameters());
            output.append(RPAREN);

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
            output.append(ASTERISK);
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
                output.append(SPACE);
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
            output.append("<error-decl>");
            return null;
        }

        @Override
        public Void visitCompoundStmt(CompoundStmt stmt, Void arg) {
            output.append(LBRACE);
            output.println();

            writeSemicolonTerminatedIndented(stmt.getDeclarations());
            writeTerminatedIndented(stmt.getStatements(), "");

            output.append(indentation);
            output.append(RBRACE);
            return null;
        }

        @Override
        public Void visitExpressionStmt(ExpressionStmt stmt, Void arg) {
            stmt.getExpression().accept(this, null);
            output.append(SEMICOLON);
            return null;
        }

        @Override
        public Void visitIfStmt(IfStmt stmt, Void arg) {
            // Condition
            output.append(STMT_IF);
            output.append(SPACE);
            output.append(LPAREN);
            stmt.getCondition().accept(this, null);
            output.append(RPAREN);

            // True statement
            output.println();
            output.append(indentation);
            stmt.getTrueStatement().accept(this, null);

            // False statement
            if (stmt.getFalseStatement().isPresent()) {
                final Statement falseStmt = stmt.getFalseStatement().get();

                output.println();
                output.append(indentation);
                output.append(STMT_ELSE);

                if (falseStmt instanceof IfStmt) {
                    output.append(SPACE);
                    falseStmt.accept(this, null);
                } else {
                    output.println();
                    output.append(indentation);
                    falseStmt.accept(this, null);
                }
            }

            return null;
        }

        @Override
        public Void visitSwitchStmt(SwitchStmt stmt, Void arg) {
            // Condition
            output.append(STMT_SWITCH);
            output.append(SPACE);
            output.append(LPAREN);
            stmt.getCondition().accept(this, null);
            output.append(RPAREN);

            // Statement
            output.println();
            output.append(indentation);
            stmt.getStatement().accept(this, null);

            return null;
        }

        @Override
        public Void visitForStmt(ForStmt stmt, Void arg) {
            // Condition
            output.append(STMT_FOR);
            output.append(SPACE);
            output.append(LPAREN);
            if (stmt.getInitExpression().isPresent()) {
                stmt.getInitExpression().get().accept(this, null);
            }
            output.append(SEMICOLON);
            if (stmt.getConditionExpression().isPresent()) {
                output.append(SPACE);
                stmt.getConditionExpression().get().accept(this, null);
            }
            output.append(SEMICOLON);
            if (stmt.getIncrementExpression().isPresent()) {
                output.append(SPACE);
                stmt.getIncrementExpression().get().accept(this, null);
            }
            output.append(RPAREN);

            // Statement
            output.println();
            output.append(indentation);
            stmt.getStatement().accept(this, null);

            return null;
        }

        @Override
        public Void visitWhileStmt(WhileStmt stmt, Void arg) {
            // Condition
            output.append(STMT_WHILE);
            output.append(SPACE);
            output.append(LPAREN);
            stmt.getCondition().accept(this, null);
            output.append(RPAREN);

            // Statement
            output.println();
            output.append(indentation);
            stmt.getStatement().accept(this, null);

            return null;
        }

        @Override
        public Void visitDoWhileStmt(DoWhileStmt stmt, Void arg) {
            // Statement
            output.append(STMT_DO);
            output.println();
            output.append(indentation);
            stmt.getStatement().accept(this, null);

            // Condition
            output.println();
            output.append(STMT_WHILE);
            output.append(SPACE);
            output.append(LPAREN);
            stmt.getCondition().accept(this, null);
            output.append(RPAREN);
            output.append(SEMICOLON);

            return null;
        }

        @Override
        public Void visitReturnStmt(ReturnStmt stmt, Void arg) {
            output.append(STMT_RETURN);
            if (stmt.getValue().isPresent()) {
                output.append(SPACE);
                stmt.getValue().get().accept(this, null);
            }
            output.append(SEMICOLON);
            return null;
        }

        @Override
        public Void visitEmptyStmt(EmptyStmt stmt, Void arg) {
            // empty statement is ignored
            return null;
        }

        @Override
        public Void visitBreakStmt(BreakStmt stmt, Void arg) {
            output.append(STMT_BREAK);
            output.append(SEMICOLON);
            return null;
        }

        @Override
        public Void visitContinueStmt(ContinueStmt stmt, Void arg) {
            output.append(STMT_CONTINUE);
            output.append(SEMICOLON);
            return null;
        }

        @Override
        public Void visitGotoStmt(GotoStmt stmt, Void arg) {
            output.append(STMT_GOTO);
            output.append(SPACE);
            stmt.getIdLabel().accept(this, null);
            output.append(SEMICOLON);
            return null;
        }

        @Override
        public Void visitLabeledStmt(LabeledStmt stmt, Void arg) {
            stmt.getLabel().accept(this, null);
            if (stmt.getStatement().isPresent()) {
                output.append(SPACE);
                stmt.getStatement().get().accept(this, null);
            }
            return null;
        }

        @Override
        public Void visitAtomicStmt(AtomicStmt stmt, Void arg) {
            output.append(NESC_ATOMIC);
            output.append(SPACE);
            stmt.getStatement().accept(this, null);
            return null;
        }

        @Override
        public Void visitComputedGotoStmt(ComputedGotoStmt stmt, Void arg) {
            output.append(STMT_GOTO);
            output.append(SPACE);
            stmt.getAddress().accept(this, null);
            output.append(SEMICOLON);
            return null;
        }

        @Override
        public Void visitAsmStmt(AsmStmt stmt, Void arg) {
            output.append(GCC_ASM);
            if (!stmt.getQualifiers().isEmpty()) {
                output.append(SPACE);
                writeSpaceSeparated(stmt.getQualifiers());
            }
            output.append(SPACE);
            output.append(LPAREN);

            stmt.getArg1().accept(this, null);
            output.append(SPACE);
            output.append(COLON);

            if (!stmt.getAsmOperands1().isEmpty()) {
                output.append(SPACE);
                writeCommaSeparated(stmt.getAsmOperands1());
                output.append(SPACE);
            }

            output.append(COLON);

            if (!stmt.getAsmOperands2().isEmpty()) {
                output.append(SPACE);
                writeCommaSeparated(stmt.getAsmOperands2());
            }

            if (!stmt.getAsmClobbers().isEmpty()) {
                output.append(SPACE);
                output.append(COLON);
                output.append(SPACE);
                writeCommaSeparated(stmt.getAsmClobbers());
            }

            output.append(RPAREN);
            output.append(SEMICOLON);
            return null;
        }

        @Override
        public Void visitErrorStmt(ErrorStmt stmt, Void arg) {
            output.append("<error-stmt>");
            return null;
        }

        @Override
        public Void visitCaseLabel(CaseLabel label, Void arg) {
            output.append(LBL_CASE);
            output.append(SPACE);
            label.getLow().accept(this, null);

            if (label.getHigh().isPresent()) {
                output.append(SPACE);
                output.append(ELLIPSIS);
                output.append(SPACE);
                label.getHigh().get().accept(this, null);
            }

            output.append(COLON);
            return null;
        }

        @Override
        public Void visitDefaultLabel(DefaultLabel label, Void arg) {
            output.append(LBL_DEFAULT);
            output.append(COLON);
            return null;
        }

        @Override
        public Void visitIdLabel(IdLabel label, Void arg) {
            output.append(label.getId());
            if (label.getIsColonTerminated()) {
                output.append(COLON);
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
            output.append(identifier.getName().getName());

            if (!identifier.getArguments().isEmpty()) {
                output.append(LBRACK);
                writeCommaSeparated(identifier.getArguments());
                output.append(RBRACK);
            }

            return null;
        }

        @Override
        public Void visitAsmOperand(AsmOperand asm, Void arg) {
            if (asm.getWord1().isPresent()) {
                output.append(LBRACK);
                output.append(asm.getWord1().get().getName());
                output.append(RBRACK);
                output.append(SPACE);
            }

            asm.getString().accept(this, null);
            output.append(SPACE);
            output.append(LPAREN);
            asm.getArg1().accept(this, null);
            output.append(RPAREN);

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
            printExprWithType(expr, OP_ALIGNOF, expr.getAsttype());
            return null;
        }

        @Override
        public Void visitSizeofType(SizeofType expr, Void arg) {
            printExprWithType(expr, OP_SIZEOF, expr.getAsttype());
            return null;
        }

        @Override
        public Void visitOffsetof(Offsetof expr, Void arg) {
            writeLeftParentheses(expr);

            output.append(MACRO_OFFSETOF);
            output.append(LPAREN);
            expr.getTypename().accept(this, null);
            output.append(COMMA);
            output.append(SPACE);
            writeDotSeparated(expr.getFieldlist());
            output.append(RPAREN);

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
            output.append(LBRACK);
            writeCommaSeparated(expr.getIndex());
            output.append(RBRACK);

            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitErrorExpr(ErrorExpr expr, Void arg) {
            writeLeftParentheses(expr);
            output.append("<syntax-error>");
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

            output.append(LABELADDRESS.toString());
            output.append(expr.getIdLabel().getId());

            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitConditional(Conditional expr, Void arg) {
            writeLeftParentheses(expr);

            // FIXME when true-expr is absent
            expr.getCondition().accept(this, null);
            output.append(" ? ");
            if (expr.getOnTrueExp().isPresent()) {
                expr.getOnTrueExp().get().accept(this, null);
            } else {
                output.append(" <absent> ");
            }
            output.append(" : ");
            expr.getOnFalseExp().accept(this, null);

            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitIdentifier(Identifier expr, Void arg) {
            writeLeftParentheses(expr);

            switch (settings.getNameMode()) {
                case USE_NORMAL_NAMES:
                    output.append(expr.getName());
                    break;
                case USE_UNIQUE_NAMES:
                    output.append(expr.getUniqueName().or(expr.getName()));
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

            output.append(LPAREN);
            expr.getStatement().accept(this, null);
            output.append(RPAREN);

            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitIntegerCst(IntegerCst expr, Void arg) {
            printConstant(expr);
            return null;
        }

        @Override
        public Void visitFloatingCst(FloatingCst expr, Void arg) {
            printConstant(expr);
            return null;
        }

        @Override
        public Void visitCharacterCst(CharacterCst expr, Void arg) {
            writeLeftParentheses(expr);

            output.append(APOSTROPHE);
            output.append(expr.getString());
            output.append(APOSTROPHE);

            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitStringCst(StringCst expr, Void arg) {
            writeLeftParentheses(expr);

            output.append(QUOTATION_MARK);
            output.append(expr.getString());
            output.append(QUOTATION_MARK);

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
            printFunctionCall(expr);
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
            printFieldLikeExpr(expr, expr.getFieldName());
            return null;
        }

        @Override
        public Void visitInterfaceDeref(InterfaceDeref expr, Void arg) {
            printFieldLikeExpr(expr, expr.getMethodName());
            return null;
        }

        @Override
        public Void visitComponentDeref(ComponentDeref expr, Void arg) {
            printFieldLikeExpr(expr, expr.getFieldName());
            return null;
        }

        @Override
        public Void visitPreincrement(Preincrement expr, Void arg) {
            printPreincrement(expr, INCREMENT);
            return null;
        }

        @Override
        public Void visitPredecrement(Predecrement expr, Void arg) {
            printPreincrement(expr, DECREMENT);
            return null;
        }

        @Override
        public Void visitPostincrement(Postincrement expr, Void arg) {
            printPostincrement(expr, INCREMENT);
            return null;
        }

        @Override
        public Void visitPostdecrement(Postdecrement expr, Void arg) {
            printPostincrement(expr, DECREMENT);
            return null;
        }

        @Override
        public Void visitCast(Cast expr, Void arg) {
            printCastLikeExpr(expr, expr.getAsttype(), expr.getArgument());
            return null;
        }

        @Override
        public Void visitCastList(CastList expr, Void arg) {
            printCastLikeExpr(expr, expr.getAsttype(), expr.getInitExpr());
            return null;
        }

        @Override
        public Void visitInitList(InitList expr, Void arg) {
            writeLeftParentheses(expr);

            output.append(LBRACE);
            writeCommaSeparated(expr.getArguments());
            output.append(RBRACE);

            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitInitSpecific(InitSpecific expr, Void arg) {
            writeLeftParentheses(expr);

            writeSpaceSeparated(expr.getDesignator());
            output.append(SPACE);
            output.append(ASSIGN.toString());
            output.append(SPACE);
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
            output.append(LBRACK);
            writeCommaSeparated(expr.getArguments());
            output.append(RBRACK);

            writeRightParentheses(expr);
            return null;
        }

        @Override
        public Void visitExtensionExpr(ExtensionExpr expr, Void arg) {
            writeLeftParentheses(expr);

            output.append(GCC_EXTENSION);
            output.append(SPACE);
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
                    output.append(typename.getName());
                    output.append(DOT.toString());
                    output.append(typename.getTypeName());
                    break;
                case USE_UNIQUE_NAMES:
                    output.append(typename.getUniqueName());
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
            output.append(rid.getId().getName());
            return null;
        }

        @Override
        public Void visitQualifier(Qualifier qualifier, Void arg) {
            output.append(qualifier.getId().getName());
            return null;
        }

        @Override
        public Void visitNescAttribute(NescAttribute nescAttribute, Void arg) {
            output.append(AT);
            output.append(nescAttribute.getName().getName());
            output.append(LPAREN);
            nescAttribute.getValue().accept(this, null);
            output.append(RPAREN);
            return null;
        }

        @Override
        public Void visitGccAttribute(GccAttribute gccAttribute, Void arg) {
            output.append(GCC_ATTRIBUTE);
            output.append(LPAREN);
            output.append(LPAREN);

            output.append(gccAttribute.getName().getName());
            if (gccAttribute.getArguments().isPresent()) {
                output.append(LPAREN);
                // FIXME don't use unique names while printing arguments for GCC attributes
                writeCommaSeparated(gccAttribute.getArguments().get());
                output.append(RPAREN);
            }

            output.append(RPAREN);
            output.append(RPAREN);
            return null;
        }

        @Override
        public Void visitDesignateField(DesignateField designator, Void arg) {
            output.append(DOT.toString());
            output.append(designator.getName());
            return null;
        }

        @Override
        public Void visitDesignateIndex(DesignateIndex designator, Void arg) {
            output.append(LBRACK);

            designator.getFirst().accept(this, null);
            if (designator.getLast().isPresent()) {
                output.append(SPACE);
                output.append(ELLIPSIS);
                output.append(SPACE);
                designator.getLast().get().accept(this, null);
            }

            output.append(RBRACK);
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

        private void writeConstantFunctionCall(ConstantFunctionCall cstFunCall) {
            switch (settings.getUniqueMode()) {
                case OUTPUT_CALLS:
                    printFunctionCall(cstFunCall);
                    break;
                case OUTPUT_VALUES:
                    output.append(cstFunCall.getValue().toString());
                    break;
                default:
                    throw new RuntimeException("unexpected constant functions mode '"
                            + settings.getUniqueMode() + "'");
            }
        }

        private void writeUsesProvides(RpInterface rp, String rpKeyword) {
            output.append(rpKeyword);

            if (rp.getDeclarations().size() == 1) {
                output.append(SPACE);
                rp.getDeclarations().getFirst().accept(this, null);
                addTrailingSemicolonIfNeeded(rp.getDeclarations().getFirst());
            } else {
                output.println();
                output.append(indentation);
                output.append(LBRACE);
                output.println();

                increaseIndentation();
                writeTopLevelDeclarations(rp.getDeclarations());
                decreaseIndentation();

                output.append(indentation);
                output.append(RBRACE);
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
                output.append(NESC_GENERIC);
                output.append(SPACE);
            }

            output.append(componentKeyword);
            output.append(SPACE);
            output.append(component.getName().getName());

            if (component.getIsAbstract()) {
                output.append(LPAREN);
                if (component.getParameters().isPresent()) {
                    writeCommaSeparated(component.getParameters().get());
                }
                output.append(RPAREN);
            }

            if (!component.getAttributes().isEmpty()) {
                output.append(SPACE);
                writeSpaceSeparated(component.getAttributes());
            }
        }

        private void writeComponentSpecification(LinkedList<Declaration> specDeclarations) {
            output.append(LBRACE);
            output.println();

            writeSemicolonTerminatedIndented(specDeclarations);
            output.append(indentation);

            output.append(RBRACE);
        }

        private void writeComponentImplementation(LinkedList<Declaration> implDeclarations) {
            output.append(NESC_IMPLEMENTATION);
            output.println();
            output.append(indentation);
            output.append(LBRACE);
            output.println();

            increaseIndentation();
            writeTopLevelDeclarations(implDeclarations);
            decreaseIndentation();

            output.append(indentation);
            output.append(RBRACE);
        }

        private void writeTopLevelDeclarations(List<? extends Declaration> declarations) {
            boolean first = true;

            for (Declaration declaration : declarations) {
                if (!first && declaration instanceof FunctionDecl) {
                    output.println();
                }
                first = false;
                writeTopLevelDeclaration(declaration);
                output.println();
            }
        }

        private void writeTopLevelDeclaration(Declaration declaration) {
            output.append(indentation);
            declaration.accept(this, null);
            addTrailingSemicolonIfNeeded(declaration);
        }

        private void addTrailingSemicolonIfNeeded(Declaration paddedDeclaration) {
            if (paddedDeclaration instanceof DataDecl) {
                output.append(SEMICOLON);
            }
        }

        private void writeConnection(EndPoint leftEndpoint, EndPoint rightEndpoint,
                String connectionToken) {
            leftEndpoint.accept(this, null);
            output.append(SPACE);
            output.append(connectionToken);
            output.append(SPACE);
            rightEndpoint.accept(this, null);
        }

        private void writeTag(TagRef tagRef, String tagKeyword, boolean at) {
            // Write the tag keyword
            output.append(tagKeyword);

            // Write '@' if it is necessary
            if (at) {
                output.append(SPACE);
                output.append(AT);
            }

            // Write the name if the tag is not anonymous
            final Optional<Word> tag = Optional.fromNullable(tagRef.getName());
            if (tag.isPresent()) {
                output.append(SPACE);
                writeName(tag.get().getName(), tagRef.getUniqueName().get());
            }

            // Write attributes
            if (!tagRef.getAttributes().isEmpty()) {
                output.append(SPACE);
                writeSpaceSeparated(tagRef.getAttributes());
            }

            // Write fields
            switch (tagRef.getSemantics()) {
                case PREDEFINITION:
                case DEFINITION:
                    output.append(SPACE);
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
                    output.append(name);
                    break;
                case USE_UNIQUE_NAMES:
                    output.append(uniqueName);
                    break;
                default:
                    throw new IllegalStateException("unexpected name mode '"
                            + settings.getNameMode() + "'");
            }
        }

        private void writeTypeof(Node typeofParam) {
            output.append(OP_TYPEOF);
            output.append(LPAREN);
            typeofParam.accept(this, null);
            output.append(RPAREN);
        }

        private void writeBinary(Binary binary, Tokens.BinaryOp op) {
            writeLeftParentheses(binary);
            binary.getLeftArgument().accept(this, null);

            output.append(SPACE);
            output.append(op.toString());
            output.append(SPACE);

            binary.getRightArgument().accept(this, null);
            writeRightParentheses(binary);
        }

        private void writeUnary(Unary unary, Tokens.UnaryOp op) {
            writeLeftParentheses(unary);

            output.append(op.toString());
            unary.getArgument().accept(this, null);

            writeRightParentheses(unary);
        }

        private void writeLetterUnary(Unary unary, String op) {
            writeLeftParentheses(unary);

            output.append(op);
            output.append(SPACE);
            unary.getArgument().accept(this, null);

            writeRightParentheses(unary);
        }

        private void printExprWithType(Expression expr, String op, AstType astType) {
            writeLeftParentheses(expr);

            output.append(op);
            output.append(LPAREN);
            astType.accept(this, null);
            output.append(RPAREN);

            writeRightParentheses(expr);
        }

        private void printConstant(LexicalCst cst) {
            writeLeftParentheses(cst);
            output.append(cst.getString());
            writeRightParentheses(cst);
        }

        private void printPreincrement(Unary unary, Tokens.UnaryOp op) {
            writeLeftParentheses(unary);
            output.append(op.toString());
            unary.getArgument().accept(this, null);
            writeRightParentheses(unary);
        }

        private void printPostincrement(Unary unary, Tokens.UnaryOp op) {
            writeLeftParentheses(unary);
            unary.getArgument().accept(this, null);
            output.append(op.toString());
            writeRightParentheses(unary);
        }

        private void printCastLikeExpr(Expression expr, AstType type, Expression subExpr) {
            writeLeftParentheses(expr);

            output.append(LPAREN);
            type.accept(this, null);
            output.append(RPAREN);
            output.append(SPACE);
            subExpr.accept(this, null);

            writeRightParentheses(expr);
        }

        private void printFieldLikeExpr(Unary unary, String fieldName) {
            writeLeftParentheses(unary);

            unary.getArgument().accept(this, null);
            output.append(DOT.toString());
            output.append(fieldName);

            writeRightParentheses(unary);
        }

        private void printFieldLikeExpr(Unary unary, Word fieldName) {
            writeLeftParentheses(unary);

            unary.getArgument().accept(this, null);
            output.append(DOT.toString());
            output.append(fieldName.getName());

            writeRightParentheses(unary);
        }

        private void printFunctionCall(FunctionCall expr) {
            writeLeftParentheses(expr);
            final AstType vaArgCall = expr.getVaArgCall();

            if (vaArgCall == null) {
                // Call keyword
                final Optional<String> callKeyword = Optional.fromNullable(CALL_KEYWORDS.get(expr.getCallKind()));
                if (callKeyword.isPresent()) {
                    output.append(callKeyword.get());
                    output.append(" ");
                }

                // Function identifier and parameters
                expr.getFunction().accept(this, null);
                output.append(LPAREN);
                writeCommaSeparated(expr.getArguments());
                output.append(RPAREN);
            } else {
                output.append(GCC_BUILTIN_VA_ARG);
                output.append(LPAREN);
                writeCommaSeparated(expr.getArguments());
                output.append(COMMA);
                output.append(SPACE);
                vaArgCall.accept(this, null);
                output.append(RPAREN);
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
                    output.append(separator);
                } else {
                    first = false;
                }

                node.accept(this, null);
            }
        }

        private void writeTerminated(List<? extends Node> nodes, String terminator) {
            for (Node node : nodes) {
                node.accept(this, null);
                output.append(terminator);
            }
        }

        private void writeTerminatedIndented(List<? extends Node> nodes, String terminator) {
            increaseIndentation();

            for (Node node : nodes) {
                output.append(indentation);
                node.accept(this, null);
                output.append(terminator);
                output.println();
            }

            decreaseIndentation();
        }

        private void writeTerminatedInBraces(List<? extends Node> nodes, String terminator) {
            output.append(LBRACE);

            if (!nodes.isEmpty()) {
                output.println();
                writeTerminatedIndented(nodes, terminator);
                output.append(indentation);
            }

            output.append(RBRACE);
        }

        private void writeSeparatedInBraces(List<? extends Node> nodes, String separator) {
            output.append(LBRACE);

            if (!nodes.isEmpty()) {
                output.println();
                increaseIndentation();

                boolean first = true;
                for (Node node : nodes) {
                    if (!first) {
                        output.append(separator);
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

            output.append(RBRACE);
        }

        private void writeLeftParentheses(Expression expression) {
            writeRepeated(LPAREN, Optional.fromNullable(expression.getParenthesesCount()).or(0));
        }

        private void writeRightParentheses(Expression expression) {
            writeRepeated(RPAREN, Optional.fromNullable(expression.getParenthesesCount()).or(0));
        }

        private void writeRepeated(String toRepeat, int count) {
            for (int i = 0; i < count; ++i) {
                output.append(toRepeat);
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
                output.append(LPAREN);
            }
            declarator.accept(this, null);
            if (inParentheses) {
                output.append(RPAREN);
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
