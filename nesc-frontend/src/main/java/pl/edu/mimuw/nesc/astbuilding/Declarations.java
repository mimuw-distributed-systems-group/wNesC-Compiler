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
import pl.edu.mimuw.nesc.environment.ScopeType;
import pl.edu.mimuw.nesc.parser.TypeElementsAssociation;
import pl.edu.mimuw.nesc.problem.NescIssue;
import pl.edu.mimuw.nesc.token.Token;
import pl.edu.mimuw.nesc.ast.type.Type;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import static java.lang.String.format;
import static pl.edu.mimuw.nesc.analysis.TagsAnalysis.makeFieldDeclaration;
import static pl.edu.mimuw.nesc.analysis.TypesAnalysis.resolveType;
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
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class Declarations extends AstBuildingBase {

    private static final ErrorDecl ERROR_DECLARATION;

    static {
        ERROR_DECLARATION = new ErrorDecl(Location.getDummyLocation());
        ERROR_DECLARATION.setEndLocation(Location.getDummyLocation());
    }

    /**
     * A set with scopes that can contain standalone declarations of tags, i.e.
     * declarations that have no declarators and end with a semicolon, e.g.:
     *     struct S;
     *     union U;
     */
    private static final Set<ScopeType> STANDALONE_TAGS_SCOPES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            ScopeType.GLOBAL,
            ScopeType.INTERFACE,
            ScopeType.SPECIFICATION,
            ScopeType.MODULE_IMPLEMENTATION,
            ScopeType.CONFIGURATION_IMPLEMENTATION,
            ScopeType.COMPOUND
        ))
    );

    public Declarations(NescEntityEnvironment nescEnvironment,
                        ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder,
                        ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder) {
        super(nescEnvironment, issuesMultimapBuilder, tokensMultimapBuilder);
    }

    public ErrorDecl makeErrorDecl() {
        return ERROR_DECLARATION;
    }

    public VariableDecl startDecl(Environment environment, Declarator declarator, Optional<AsmStmt> asmStmt,
                                  TypeElementsAssociation association, LinkedList<Attribute> attributes,
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
                    association.getTypeElements(),
                    attributes);
            variableDecl.setEndLocation(endLocation);
        }

        // Resolve and save type
        variableDecl.setType(association.resolveType(Optional.of(declarator),
                environment, errorHelper, variableDecl.getLocation(),
                variableDecl.getLocation()));

        final StartDeclarationVisitor declarationVisitor = new StartDeclarationVisitor(environment, variableDecl,
                declarator, asmStmt, association.getTypeElements(), attributes);
        declarator.accept(declarationVisitor, null);
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

    public DataDecl makeDataDecl(Environment environment, Location startLocation, Location endLocation,
                                 TypeElementsAssociation association, LinkedList<Declaration> decls) {
        // Process potential tag declarations from the type specifiers
        final Optional<Type> type = association.getType(environment, decls.isEmpty(), errorHelper, startLocation, startLocation);

        final DataDecl result = new DataDecl(startLocation, association.getTypeElements(), decls);
        result.setEndLocation(endLocation);
        result.setType(type);

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

        final StartFunctionVisitor startVisitor = new StartFunctionVisitor(environment, functionDecl, modifiers);
        try {
            declarator.accept(startVisitor, null);
        } catch (RuntimeException e) {
            /* Return absent. Syntax error should be reported. */
            return Optional.absent();
        }
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
        variableDecl.setType(resolveType(environment, elements, declarator,
                errorHelper, varStartLocation, varEndLocation));

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
        return makeTagRef(startLocation, endLocation, kind, tag, fields, attributes, true);
    }

    public TagRef makeEnum(Location startLocation, Location endLocation, Optional<Word> tag,
                           LinkedList<Declaration> fields, LinkedList<Attribute> attributes) {
        return makeTagRef(startLocation, endLocation, StructKind.ENUM, tag, fields,
                          attributes, true);
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
     * @param association   type elements association
     * @param attributes    attributes
     * @return declaration of field
     */
    public FieldDecl makeField(Environment environment, Location startLocation, Location endLocation,
                               Optional<Declarator> declarator, Optional<Expression> bitfield,
                               TypeElementsAssociation association, LinkedList<Attribute> attributes) {
        // Resolve the base type for this field if it has not been already done
        final Optional<Type> maybeBaseType = association.getType(environment, false, errorHelper,
                startLocation, endLocation);

        // FIXME: elements?
        endLocation = getEndLocation(endLocation, attributes);
        final FieldDecl decl = new FieldDecl(startLocation, declarator.orNull(), attributes, bitfield.orNull());
        decl.setEndLocation(endLocation);
        makeFieldDeclaration(decl, maybeBaseType, errorHelper);

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

    public AstType makeType(Environment environment, LinkedList<TypeElement> elements,
                            Optional<Declarator> declarator) {
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
        return makeTagRef(startLocation, endLocation, structKind, tag, declarations, attributes, false);
    }

    private TagRef makeTagRef(Location startLocation, Location endLocation, StructKind structKind,
                              Optional<Word> tag, LinkedList<Declaration> declarations,
                              LinkedList<Attribute> attributes, boolean isDefined) {
        final TagRef tagRef;
        switch (structKind) {
            case STRUCT:
                tagRef = new StructRef(startLocation, attributes, declarations, isDefined, tag.orNull());
                break;
            case UNION:
                tagRef = new UnionRef(startLocation, attributes, declarations, isDefined, tag.orNull());
                break;
            case NX_STRUCT:
                tagRef = new NxStructRef(startLocation, attributes, declarations, isDefined, tag.orNull());
                break;
            case NX_UNION:
                tagRef = new NxUnionRef(startLocation, attributes, declarations, isDefined, tag.orNull());
                break;
            case ENUM:
                tagRef = new EnumRef(startLocation, attributes, declarations, isDefined, tag.orNull());
                break;
            case ATTRIBUTE:
                tagRef = new AttributeRef(startLocation, attributes, declarations, isDefined, tag.orNull());
                break;
            default:
                throw new IllegalArgumentException("Unexpected argument " + structKind);
        }
        tagRef.setEndLocation(endLocation);
        return tagRef;
    }

    private class StartFunctionVisitor extends ExceptionVisitor<Void, Void> {

        private final Environment environment;
        private final FunctionDecl functionDecl;
        private final LinkedList<TypeElement> modifiers;

        public StartFunctionVisitor(Environment environment, FunctionDecl functionDecl,
                                    LinkedList<TypeElement> modifiers) {
            this.environment = environment;
            this.functionDecl = functionDecl;
            this.modifiers = modifiers;
        }

        @Override
        public Void visitPointerDeclarator(PointerDeclarator pointerDeclarator, Void arg) {
            pointerDeclarator.getDeclarator().accept(this, null);
            return null;
        }

        @Override
        public Void visitQualifiedDeclarator(QualifiedDeclarator qualifiedDeclarator, Void arg) {
            qualifiedDeclarator.getDeclarator().accept(this, null);
            return null;
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
            final FunctionDeclaration functionDeclaration;

            /* Check previous declaration. */
            final Optional<? extends ObjectDeclaration> previousDeclarationOpt = environment.getObjects().get(name);
            if (!previousDeclarationOpt.isPresent()) {
                functionDeclaration = new FunctionDeclaration(name, startLocation);
                define(functionDeclaration, funDeclarator);
            } else {
                final ObjectDeclaration previousDeclaration = previousDeclarationOpt.get();
                /* Trying to redeclare non-function declaration. */
                if (!(previousDeclaration instanceof FunctionDeclaration)) {
                    Declarations.this.errorHelper.error(funDeclarator.getLocation(), funDeclarator.getEndLocation(),
                            format("redeclaration of '%s'", name));

                    /* Nevertheless, create declaration, put it into ast node
                     * but not into environment. */
                    functionDeclaration = new FunctionDeclaration(name, startLocation);
                }
                /* Previous declaration is a function declaration or
                 * definition. */
                else {
                    final FunctionDeclaration tmpDecl = (FunctionDeclaration) previousDeclaration;
                    if (!tmpDecl.isDefined()) {
                        /* Update previous declaration. */
                        functionDeclaration = tmpDecl;
                        functionDeclaration.setLocation(startLocation);
                    } else {
                        /* Function redefinition is forbidden. */
                        Declarations.this.errorHelper.error(funDeclarator.getLocation(), funDeclarator.getEndLocation(),
                                format("redefinition of '%s'", name));
                        functionDeclaration = new FunctionDeclaration(name, startLocation);
                    }

                    // TODO: check if types match in declarations
                }
            }

            functionDeclaration.setAstFunctionDeclarator(funDeclarator);
            functionDeclaration.setFunctionType(TypeElementUtils.getFunctionType(modifiers));
            functionDeclaration.setDefined(true);
            functionDecl.setDeclaration(functionDeclaration);
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
                define(declaration, funDeclarator);
                declaration.setDefined(true);
                functionDecl.setDeclaration(declaration);
            } else {
                throw new IllegalStateException("Unexpected declarator class " + innerDeclarator.getClass());
            }
        }

        private void define(ObjectDeclaration declaration, Declarator declarator) {
            if (!environment.getObjects().add(declaration.getName(), declaration)) {
                Declarations.this.errorHelper.error(declarator.getLocation(), Optional.of(declarator.getEndLocation()),
                        format("redefinition of '%s'", declaration.getName()));
            }
        }

        // TODO: adding tokens (e.g. for semantic colouring)
    }

    private class StartDeclarationVisitor extends ExceptionVisitor<Void, Void> {

        private final Environment environment;
        private final VariableDecl variableDecl;
        private final LinkedList<TypeElement> elements;

        @SuppressWarnings("UnusedParameters")
        StartDeclarationVisitor(Environment environment, VariableDecl variableDecl, Declarator declarator,
                                Optional<AsmStmt> asmStmt, LinkedList<TypeElement> elements,
                                LinkedList<Attribute> attributes) {
            this.environment = environment;
            this.variableDecl = variableDecl;
            this.elements = elements;
        }

        @Override
        public Void visitFunctionDeclarator(FunctionDeclarator funDeclarator, Void arg) {
            /*
             * Function declaration (not definition!). There can be many
             * declarations but only one definition.
             * All declarations and definition must have the same return type
             * and types of parameters.
             */
            variableDecl.setForward(true);
            final String name = getDeclaratorName(funDeclarator);
            final FunctionDeclaration functionDeclaration;
            /*
             * Check previous declarations.
             */
            final Optional<? extends ObjectDeclaration> previousDeclarationOpt = environment.getObjects().get(name);
            if (!previousDeclarationOpt.isPresent()) {
                functionDeclaration = new FunctionDeclaration(name, funDeclarator.getLocation());
                declare(functionDeclaration, funDeclarator);
            } else {
                final ObjectDeclaration previousDeclaration = previousDeclarationOpt.get();
                /* Trying to redeclare non-function declaration. */
                if (!(previousDeclaration instanceof FunctionDeclaration)) {
                    Declarations.this.errorHelper.error(funDeclarator.getLocation(), funDeclarator.getEndLocation(),
                            format("redeclaration of '%s'", name));

                    /* Nevertheless, create declaration, put it into ast node
                     * but not into environment. */
                    functionDeclaration = new FunctionDeclaration(name, funDeclarator.getLocation());
                }
                /* Previous declaration is a function declaration or
                 * definition. */
                else {
                    functionDeclaration = (FunctionDeclaration) previousDeclaration;
                    /* Update previous declaration. */
                    functionDeclaration.setLocation(funDeclarator.getLocation());

                    // TODO: check if types match in declarations
                }
            }

            functionDeclaration.setAstFunctionDeclarator(funDeclarator);
            variableDecl.setDeclaration(functionDeclaration);
            return null;
        }

        @Override
        public Void visitPointerDeclarator(PointerDeclarator declarator, Void arg) {
            return declarator.getDeclarator().accept(this, null);
        }

        @Override
        public Void visitQualifiedDeclarator(QualifiedDeclarator declarator, Void arg) {
            return declarator.getDeclarator().accept(this, null);
        }

        @Override
        public Void visitArrayDeclarator(ArrayDeclarator declarator, Void arg) {
            return declarator.getDeclarator().accept(this, null);
        }

        @Override
        public Void visitIdentifierDeclarator(IdentifierDeclarator declarator, Void arg) {
            final String name = declarator.getName();
            final Location startLocation = declarator.getLocation();
            final boolean isTypedef = TypeElementUtils.isTypedef(elements);
            final ObjectDeclaration declaration;
            if (isTypedef) {
                declaration = new TypenameDeclaration(name, startLocation);
            } else {
                declaration = new VariableDeclaration(name, startLocation);
            }
            declare(declaration, declarator);
            variableDecl.setDeclaration(declaration);
            return null;
        }

        @Override
        public Void visitInterfaceRefDeclarator(InterfaceRefDeclarator elem, Void arg) {
            throw new IllegalStateException();
        }

        private void declare(ObjectDeclaration declaration, Declarator declarator) {
            if (!environment.getObjects().add(declaration.getName(), declaration)) {
                Declarations.this.errorHelper.error(declarator.getLocation(),
                        Optional.of(declarator.getEndLocation()),
                        format("redeclaration of '%s'", declaration.getName()));
            }
        }

        // TODO: adding tokens (e.g. for semantic colouring)
    }

}
