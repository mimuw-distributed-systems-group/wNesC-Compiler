package pl.edu.mimuw.nesc.astbuilding;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import pl.edu.mimuw.nesc.ast.AstUtils;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.declaration.object.*;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.environment.NescEntityEnvironment;
import pl.edu.mimuw.nesc.problem.NescIssue;
import pl.edu.mimuw.nesc.token.Token;

import java.util.LinkedList;

import static java.lang.String.format;
import static pl.edu.mimuw.nesc.ast.AstUtils.getEndLocation;
import static pl.edu.mimuw.nesc.ast.AstUtils.getStartLocation;
import static pl.edu.mimuw.nesc.astbuilding.DeclaratorUtils.getDeclaratorName;

/**
 * <p>
 * Contains a set of methods useful for creating syntax tree nodes during
 * parsing.
 * </p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class Declarations extends AstBuildingBase {

    private static final ErrorDecl ERROR_DECLARATION;

    static {
        ERROR_DECLARATION = new ErrorDecl(Location.getDummyLocation());
        ERROR_DECLARATION.setEndLocation(Location.getDummyLocation());
    }

    public Declarations(NescEntityEnvironment nescEnvironment,
                        ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder,
                        ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder) {
        super(nescEnvironment, issuesMultimapBuilder, tokensMultimapBuilder);
    }

    public ErrorDecl makeErrorDecl() {
        return ERROR_DECLARATION;
    }

    public VariableDecl startDecl(Environment environment, Declarator declarator, Optional<AsmStmt> asmStmt,
                                  LinkedList<TypeElement> elements, LinkedList<Attribute> attributes,
                                  boolean initialised) {
        /*
         * NOTE: This can be variable declaration, typedef declaration,
         * function declaration etc.
         * Consider the following example :)
         * int (*max)(int, int) = ({ int __fn__ (int x, int y) { return x > y ? x : y; } __fn__; });
         */
        final VariableDecl variableDecl = new VariableDecl(declarator.getLocation(), Optional.of(declarator),
                attributes, asmStmt);

        if (!initialised) {
            final Location endLocation = AstUtils.getEndLocation(
                    asmStmt.isPresent() ? asmStmt.get().getEndLocation() : declarator.getEndLocation(),
                    elements,
                    attributes);
            variableDecl.setEndLocation(endLocation);
        }

        final StartDeclarationVisitor declarationVisitor = new StartDeclarationVisitor(environment,variableDecl,
                declarator, asmStmt, elements, attributes);
        declarator.accept(declarationVisitor, null);
        final Optional<? extends ObjectDeclaration> objectDeclaration = declarationVisitor.getObjectDeclaration();

        if (objectDeclaration.isPresent()) {
            final ObjectDeclaration declaration = objectDeclaration.get();
            if (!environment.getObjects().add(declaration.getName(), declaration)) {
                errorHelper.error(declarator.getLocation(), Optional.of(declarator.getEndLocation()),
                        format("redeclaration of '%s'", declaration.getName()));
            }
            variableDecl.setDeclaration(declaration);
        }

        // TODO: add to tokens

        return variableDecl;
    }

    public VariableDecl finishDecl(VariableDecl declaration, Optional<Expression> initializer) {
        if (initializer.isPresent()) {
            final Location endLocation = initializer.get().getEndLocation();
            declaration.setEndLocation(endLocation);
        }
        declaration.setInitializer(initializer);
        return declaration;
    }

    public DataDecl makeDataDecl(Location startLocation, Location endLocation,
                                 LinkedList<TypeElement> modifiers, LinkedList<Declaration> decls) {
        final DataDecl result = new DataDecl(startLocation, modifiers, decls);
        result.setEndLocation(endLocation);
        return result;
    }

    public ExtensionDecl makeExtensionDecl(Location startLocation, Location endLocation, Declaration decl) {
        // TODO: pedantic
        final ExtensionDecl result = new ExtensionDecl(startLocation, decl);
        result.setEndLocation(endLocation);
        return result;
    }

    /**
     * <p>Finishes array of function declarator.</p>
     * <h3>Example</h3>
     * <p><code>Foo.bar(int baz)</code>
     * where <code>Foo.bar</code> is <code>nested</code>, <code>(int baz)</code>
     * is <code>declarator</code>.</p>
     *
     * @param nested     declarator that precedes array or function parentheses
     *                   (e.g. plain identifier or interface reference)
     * @param declarator declarator containing array indices declaration or
     *                   function parameters
     * @return declarator combining these two declarators
     */
    public Declarator finishArrayOrFnDeclarator(Declarator nested, NestedDeclarator declarator) {
        declarator.setLocation(nested.getLocation());
        declarator.setDeclarator(nested);
        return declarator;
    }

    /**
     * <p>Starts definition of function.</p>
     * <p>NOTICE: The <code>declarator</code> may not be function declarator but
     * an identifier declarator. This may happen when variable is declared,
     * e.g. <code>message_t packet;</code> and <code>message_t</code> token is
     * not recognized as typedef name.</p>
     *
     * @param environment   current environment
     * @param startLocation start location
     * @param modifiers     modifiers
     * @param declarator    function declarator
     * @param attributes    attributes
     * @param isNested      <code>true</code> for nested functions
     * @return function declaration or <code>Optional.absent()</code> when
     * error occurs
     */
    public Optional<FunctionDecl> startFunction(Environment environment, Location startLocation,
                                                LinkedList<TypeElement> modifiers, Declarator declarator,
                                                LinkedList<Attribute> attributes, boolean isNested) {
        final FunctionDecl functionDecl = new FunctionDecl(startLocation, declarator, modifiers, attributes,
                null, isNested);

        final StartFunctionVisitor startVisitor = new StartFunctionVisitor(modifiers);
        try {
            declarator.accept(startVisitor, null);
        } catch (RuntimeException e) {
            /* Return absent. Syntax error should be reported. */
            return Optional.absent();
        }
        final ObjectDeclaration symbol = startVisitor.getObjectDeclaration();

        if (!environment.getObjects().add(symbol.getName(), symbol)) {
            errorHelper.error(declarator.getLocation(), Optional.of(declarator.getEndLocation()),
                    format("redeclaration of '%s'", symbol.getName()));
        }
        functionDecl.setDeclaration(symbol);

        // TODO: add to tokens

        return Optional.of(functionDecl);
    }

    public FunctionDecl setOldParams(FunctionDecl functionDecl, LinkedList<Declaration> oldParams) {
        functionDecl.setOldParms(oldParams);
        return functionDecl;
    }

    public FunctionDecl finishFunction(FunctionDecl functionDecl, Statement body) {
        functionDecl.setBody(body);
        functionDecl.setEndLocation(body.getEndLocation());
        return functionDecl;
    }

    /**
     * <p>Create definition of function parameter
     * <code>elements declarator</code> with attributes.</p>
     * <p>There must be at least a <code>declarator</code> or some form of type
     * specification.</p>
     *
     * @param declarator parameter declarator
     * @param elements   type elements
     * @param attributes attributes list (maybe empty)
     * @return the declaration for the parameter
     */
    public DataDecl declareParameter(Environment environment, Optional<Declarator> declarator,
                                     LinkedList<TypeElement> elements, LinkedList<Attribute> attributes) {
        /*
         * The order of entities:
         * elements [declarator] [attributes]
         */
        /* Create variable declarator. */
        final Location varStartLocation;
        final Location varEndLocation;
        if (declarator.isPresent()) {
            varStartLocation = declarator.get().getLocation();
            varEndLocation = getEndLocation(declarator.get().getEndLocation(), attributes);
        } else {
            varStartLocation = getStartLocation(elements).get();
            varEndLocation = getEndLocation(elements, attributes).get();
        }
        final VariableDecl variableDecl = new VariableDecl(varStartLocation, declarator, attributes,
                Optional.<AsmStmt>absent());
        variableDecl.setInitializer(Optional.<Expression>absent());
        variableDecl.setEndLocation(varEndLocation);

        /*
         * FIXME: parameter definition may not contain name.
         */
        if (declarator.isPresent()) {
            final String name = getDeclaratorName(declarator.get());
            if (name != null) {
                final VariableDeclaration symbol = new VariableDeclaration(name, declarator.get().getLocation());
                if (!environment.getObjects().add(name, symbol)) {
                    errorHelper.error(declarator.get().getLocation(), Optional.of(declarator.get().getEndLocation()),
                            format("redeclaration of '%s'", name));
                }
                variableDecl.setDeclaration(symbol);
            }
            // TODO: name could be null here?
        } else {
            // TODO: definition consist only from modifiers, qualifiers, etc.
        }

        /* Create parameter declarator. */
        final Location startLocation = getStartLocation(elements).get();
        final Location endLocation = declarator.isPresent()
                ? getEndLocation(declarator.get().getEndLocation(), attributes)
                : getEndLocation(elements, attributes).get();

        final DataDecl dataDecl = new DataDecl(startLocation, elements, Lists.<Declaration>newList(variableDecl));
        dataDecl.setEndLocation(endLocation);
        return dataDecl;
    }

    public OldIdentifierDecl declareOldParameter(Environment environment, Location startLocation, Location endLocation,
                                                 String id) {
        final OldIdentifierDecl decl = new OldIdentifierDecl(startLocation, id);
        decl.setEndLocation(endLocation);

        // TODO update symbol table

        return decl;
    }

    public TagRef makeStruct(Location startLocation, Location endLocation, StructKind kind, Optional<Word> tag,
                             LinkedList<Declaration> fields, LinkedList<Attribute> attributes) {
        return makeTagRef(startLocation, endLocation, kind, tag, fields, attributes);
    }

    public TagRef makeEnum(Location startLocation, Location endLocation, Optional<Word> tag,
                           LinkedList<Declaration> fields, LinkedList<Attribute> attributes) {
        return makeTagRef(startLocation, endLocation, StructKind.ENUM, tag, fields, attributes);
    }

    /**
     * Returns a reference to struct, union or enum.
     *
     * @param startLocation start location
     * @param endLocation   end location
     * @param structKind    kind
     * @param tag           name
     * @return struct/union/enum reference
     */
    public TagRef makeXrefTag(Location startLocation, Location endLocation, StructKind structKind, Word tag) {
        return makeTagRef(startLocation, endLocation, structKind, Optional.of(tag));
    }

    /**
     * Creates declaration of field
     * <code>elements declarator : bitfield</code> with attributes.
     * <code>declarator</code> and <code>bitfield</code> cannot be both
     * absent.
     *
     * @param startLocation start location
     * @param endLocation   end location
     * @param declarator    declarator
     * @param bitfield      bitfield
     * @param elements      elements
     * @param attributes    attributes
     * @return declaration of field
     */
    public FieldDecl makeField(Location startLocation, Location endLocation,
                               Optional<Declarator> declarator, Optional<Expression> bitfield,
                               LinkedList<TypeElement> elements, LinkedList<Attribute> attributes) {
        // FIXME: elements?
        endLocation = getEndLocation(endLocation, attributes);
        final FieldDecl decl = new FieldDecl(startLocation, declarator.orNull(), attributes, bitfield.orNull());
        decl.setEndLocation(endLocation);
        return decl;
    }

    public Enumerator makeEnumerator(Environment environment, Location startLocation, Location endLocation, String id,
                                     Optional<Expression> value) {
        final Enumerator enumerator = new Enumerator(startLocation, id, value.orNull());
        enumerator.setEndLocation(endLocation);

        final ConstantDeclaration symbol = new ConstantDeclaration(id, startLocation);
        if (!environment.getObjects().add(id, symbol)) {
            errorHelper.error(startLocation, Optional.of(endLocation), format("redeclaration of '%s'", id));
        }
        enumerator.setDeclaration(symbol);

        return enumerator;
    }

    public AstType makeType(LinkedList<TypeElement> elements, Optional<Declarator> declarator) {
        final Location startLocation;
        final Location endLocation;
        if (declarator.isPresent()) {
            startLocation = getStartLocation(declarator.get().getLocation(), elements);
            endLocation = declarator.get().getEndLocation();
        } else {
            startLocation = getStartLocation(elements).get();
            endLocation = getEndLocation(elements).get();
        }
        final AstType type = new AstType(startLocation, declarator.orNull(), elements);
        type.setEndLocation(endLocation);
        return type;
    }

    public Declarator makePointerDeclarator(Location startLocation, Location endLocation,
                                            Optional<Declarator> declarator,
                                            LinkedList<TypeElement> qualifiers) {
        final Location qualifiedDeclStartLocation = getStartLocation(
                declarator.isPresent()
                        ? declarator.get().getLocation()
                        : startLocation,
                qualifiers);
        final QualifiedDeclarator qualifiedDeclarator = new QualifiedDeclarator(qualifiedDeclStartLocation,
                declarator.orNull(), qualifiers);
        qualifiedDeclarator.setEndLocation(declarator.isPresent()
                ? declarator.get().getEndLocation()
                : endLocation);

        final PointerDeclarator pointerDeclarator = new PointerDeclarator(startLocation, qualifiedDeclarator);
        pointerDeclarator.setEndLocation(endLocation);
        return pointerDeclarator;
    }

    public Rid makeRid(Location startLocation, Location endLocation, RID rid) {
        final Rid result = new Rid(startLocation, rid);
        result.setEndLocation(endLocation);
        return result;
    }

    public Qualifier makeQualifier(Location startLocation, Location endLocation, RID rid) {
        final Qualifier result = new Qualifier(startLocation, rid);
        result.setEndLocation(endLocation);
        return result;
    }

    private TagRef makeTagRef(Location startLocation, Location endLocation, StructKind structKind,
                              Optional<Word> tag) {
        final LinkedList<Attribute> attributes = Lists.newList();
        final LinkedList<Declaration> declarations = Lists.newList();
        return makeTagRef(startLocation, endLocation, structKind, tag, declarations, attributes);
    }

    private TagRef makeTagRef(Location startLocation, Location endLocation, StructKind structKind,
                              Optional<Word> tag, LinkedList<Declaration> declarations,
                              LinkedList<Attribute> attributes) {
        final TagRef tagRef;
        switch (structKind) {
            case STRUCT:
                tagRef = new StructRef(startLocation, attributes, declarations, true, tag.orNull());
                break;
            case UNION:
                tagRef = new UnionRef(startLocation, attributes, declarations, true, tag.orNull());
                break;
            case NX_STRUCT:
                tagRef = new NxStructRef(startLocation, attributes, declarations, true, tag.orNull());
                break;
            case NX_UNION:
                tagRef = new NxUnionRef(startLocation, attributes, declarations, true, tag.orNull());
                break;
            case ENUM:
                tagRef = new EnumRef(startLocation, attributes, declarations, true, tag.orNull());
                break;
            case ATTRIBUTE:
                tagRef = new AttributeRef(startLocation, attributes, declarations, true, tag.orNull());
                break;
            default:
                throw new IllegalArgumentException("Unexpected argument " + structKind);
        }
        tagRef.setEndLocation(endLocation);
        return tagRef;
    }

    @SuppressWarnings({"UnnecessaryLocalVariable", "UnusedDeclaration"})    // FIXME
    private static class StartFunctionVisitor extends ExceptionVisitor<Void, Void> {

        private final LinkedList<TypeElement> modifiers;

        private ObjectDeclaration objectDeclaration;
        private Token token;
        private FunctionDecl astDeclaration;

        public StartFunctionVisitor(LinkedList<TypeElement> modifiers) {
            this.modifiers = modifiers;
        }

        public ObjectDeclaration getObjectDeclaration() {
            return objectDeclaration;
        }

        public Token getToken() {
            return token;
        }

        public Declaration getAstDeclaration() {
            return astDeclaration;
        }

        @Override
        public Void visitFunctionDeclarator(FunctionDeclarator funDeclarator, Void arg) {
            final Location startLocation = funDeclarator.getLocation();
            final Declarator innerDeclarator = funDeclarator.getDeclarator();

            /* C function/task */
            if (innerDeclarator instanceof IdentifierDeclarator) {
                identifierDeclarator(funDeclarator, (IdentifierDeclarator) innerDeclarator, startLocation);
            }
            /* command/event */
            else if (innerDeclarator instanceof InterfaceRefDeclarator) {
                interfaceRefDeclarator(funDeclarator, (InterfaceRefDeclarator) innerDeclarator, startLocation);
            } else {
                throw new IllegalStateException("Unexpected declarator class " + innerDeclarator.getClass());
            }
            return null;
        }

        private void identifierDeclarator(FunctionDeclarator funDeclarator, IdentifierDeclarator identifierDeclarator,
                                          Location startLocation) {
            final String name = identifierDeclarator.getName();
            final FunctionDeclaration functionDeclaration = new FunctionDeclaration(name, startLocation);
            functionDeclaration.setAstFunctionDeclarator(funDeclarator);
            functionDeclaration.setFunctionType(TypeElementUtils.getFunctionType(modifiers));
            objectDeclaration = functionDeclaration;
        }

        private void interfaceRefDeclarator(FunctionDeclarator funDeclarator, InterfaceRefDeclarator refDeclaration,
                                            Location startLocation) {
            final String ifaceName = refDeclaration.getName().getName();
            final Declarator innerDeclarator = refDeclaration.getDeclarator();

            if (innerDeclarator instanceof IdentifierDeclarator) {
                final IdentifierDeclarator idDeclarator = (IdentifierDeclarator) innerDeclarator;
                final String callableName = idDeclarator.getName();
                final FunctionDeclaration declaration = new FunctionDeclaration(callableName, startLocation, ifaceName);
                declaration.setAstFunctionDeclarator(funDeclarator);
                objectDeclaration = declaration;
            } else {
                throw new IllegalStateException("Unexpected declarator class " + innerDeclarator.getClass());
            }
        }

    }

    @SuppressWarnings("UnusedDeclaration")
    private static class StartDeclarationVisitor extends ExceptionVisitor<Void, Void> {

        private final VariableDecl variableDecl;
        private final LinkedList<TypeElement> elements;

        private Optional<? extends ObjectDeclaration> objectDeclaration;
        private Optional<? extends Token> token;

        StartDeclarationVisitor(Environment environment, VariableDecl variableDecl, Declarator declarator,
                                Optional<AsmStmt> asmStmt, LinkedList<TypeElement> elements,
                                LinkedList<Attribute> attributes) {
            this.variableDecl = variableDecl;
            this.elements = elements;
        }

        public Optional<? extends ObjectDeclaration> getObjectDeclaration() {
            return objectDeclaration;
        }

        public Optional<? extends Token> getToken() {
            return token;
        }

        @Override
        public Void visitFunctionDeclarator(FunctionDeclarator funDeclarator, Void arg) {
            /*
             * Function declaration (not definition!). There can be many
             * declarations but only one definition.
             * All declarations and definition must have the same return type
             * and types of parameters.
             */
            // TODO provide "chain" of consecutive declaration and definitions
            // of the same function to be able to check their equality.
            variableDecl.setForward(true);
            final String name = getDeclaratorName(funDeclarator);
            final FunctionDeclaration declaration = new FunctionDeclaration(name, funDeclarator.getLocation());
            declaration.setAstFunctionDeclarator(funDeclarator);
            objectDeclaration = Optional.of(declaration);
            return null;
        }

        @Override
        public Void visitPointerDeclarator(PointerDeclarator elem, Void arg) {
            return elem.getDeclarator().accept(this, null);
        }

        @Override
        public Void visitQualifiedDeclarator(QualifiedDeclarator elem, Void arg) {
            return elem.getDeclarator().accept(this, null);
        }

        @Override
        public Void visitArrayDeclarator(ArrayDeclarator elem, Void arg) {
            return elem.getDeclarator().accept(this, null);
        }

        @Override
        public Void visitIdentifierDeclarator(IdentifierDeclarator elem, Void arg) {
            final String name = elem.getName();
            final Location startLocation = elem.getLocation();
            final boolean isTypedef = TypeElementUtils.isTypedef(elements);
            if (isTypedef) {
                final TypenameDeclaration declaration = new TypenameDeclaration(name, startLocation);
                objectDeclaration = Optional.of(declaration);
            } else {
                final VariableDeclaration declaration = new VariableDeclaration(name, startLocation);
                objectDeclaration = Optional.of(declaration);
            }
            return null;
        }

        @Override
        public Void visitInterfaceRefDeclarator(InterfaceRefDeclarator elem, Void arg) {
            throw new IllegalStateException();
        }

    }

}
