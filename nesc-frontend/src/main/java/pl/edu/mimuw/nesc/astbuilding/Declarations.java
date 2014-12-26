package pl.edu.mimuw.nesc.astbuilding;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import pl.edu.mimuw.nesc.analysis.AttributeAnalyzer;
import pl.edu.mimuw.nesc.analysis.ExpressionsAnalysis;
import pl.edu.mimuw.nesc.analysis.SemanticListener;
import pl.edu.mimuw.nesc.ast.util.AstUtils;
import pl.edu.mimuw.nesc.ast.util.DeclaratorUtils;
import pl.edu.mimuw.nesc.ast.util.Interval;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.ast.TagRefSemantics;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.ast.type.FunctionType;
import pl.edu.mimuw.nesc.ast.type.TypeDefinitionType;
import pl.edu.mimuw.nesc.ast.type.VoidType;
import pl.edu.mimuw.nesc.ast.util.TypeElementUtils;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.declaration.object.*;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.environment.NescEntityEnvironment;
import pl.edu.mimuw.nesc.environment.ScopeType;
import pl.edu.mimuw.nesc.facade.component.specification.ModuleTable;
import pl.edu.mimuw.nesc.facade.iface.InterfaceEntity;
import pl.edu.mimuw.nesc.facade.iface.InterfaceRefFacade;
import pl.edu.mimuw.nesc.parser.TypeElementsAssociation;
import pl.edu.mimuw.nesc.problem.NescIssue;
import pl.edu.mimuw.nesc.problem.issue.*;
import pl.edu.mimuw.nesc.token.Token;
import pl.edu.mimuw.nesc.ast.type.Type;

import java.util.LinkedList;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;
import static pl.edu.mimuw.nesc.analysis.SpecifiersAnalysis.determineLinkage;
import static pl.edu.mimuw.nesc.analysis.SpecifiersAnalysis.NonTypeSpecifier;
import static pl.edu.mimuw.nesc.analysis.SpecifiersAnalysis.SpecifiersSet;
import static pl.edu.mimuw.nesc.analysis.TagsAnalysis.makeFieldDeclaration;
import static pl.edu.mimuw.nesc.analysis.TagsAnalysis.processTagReference;
import static pl.edu.mimuw.nesc.analysis.TypesAnalysis.checkFunctionParametersTypes;
import static pl.edu.mimuw.nesc.analysis.TypesAnalysis.checkVariableType;
import static pl.edu.mimuw.nesc.analysis.TypesAnalysis.resolveType;
import static pl.edu.mimuw.nesc.ast.util.AstUtils.getEndLocation;
import static pl.edu.mimuw.nesc.ast.util.AstUtils.getStartLocation;
import static pl.edu.mimuw.nesc.ast.util.DeclaratorUtils.getDeclaratorName;
import static pl.edu.mimuw.nesc.ast.util.DeclaratorUtils.getIdentifierInterval;
import static pl.edu.mimuw.nesc.ast.util.DeclaratorUtils.mangleDeclaratorName;
import static pl.edu.mimuw.nesc.problem.issue.RedeclarationError.RedeclarationKind;
import static pl.edu.mimuw.nesc.problem.issue.RedefinitionError.RedefinitionKind;

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
    /**
     * Type required for an identifier of a task.
     */
    public static final FunctionType TYPE_TASK = new FunctionType(new VoidType(), new Type[0], false);

    private static final ErrorDecl ERROR_DECLARATION;

    static {
        ERROR_DECLARATION = new ErrorDecl(Location.getDummyLocation());
        ERROR_DECLARATION.setEndLocation(Location.getDummyLocation());
    }

    /**
     * A function that returns the decayed type.
     */
    private static final Function<Type, Type> DECAY_TRANSFORMATION = new Function<Type, Type>() {
        @Override
        public Type apply(Type type) {
            checkNotNull(type, "type cannot be null");
            return type.decay();
        }
    };

    /**
     * Object that is present after a module specification is parsed and
     * analyzed. Information about implementations of commands and events is
     * passed to it. It must be present when parsing and analysis of a module
     * has started.
     */
    private Optional<ModuleTable> moduleTable = Optional.absent();

    public Declarations(NescEntityEnvironment nescEnvironment,
                        ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder,
                        ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder,
                        SemanticListener semanticListener, AttributeAnalyzer attributeAnalyzer) {
        super(nescEnvironment, issuesMultimapBuilder, tokensMultimapBuilder,
                semanticListener, attributeAnalyzer);
    }

    public ErrorDecl makeErrorDecl() {
        return ERROR_DECLARATION;
    }

    public VariableDecl startDecl(Environment environment, Declarator declarator, Optional<AsmStmt> asmStmt,
                                  TypeElementsAssociation association, LinkedList<Attribute> attributes,
                                  boolean initialised, boolean buildingUsesProvides) {
        /*
         * NOTE: This can be variable declaration, typedef declaration,
         * function declaration etc.
         * Consider the following example :)
         * int (*max)(int, int) = ({ int __fn__ (int x, int y) { return x > y ? x : y; } __fn__; });
         */
        final VariableDecl variableDecl = new VariableDecl(declarator.getLocation(), Optional.of(declarator),
                attributes, asmStmt);

        final Optional<String> identifier = DeclaratorUtils.getDeclaratorName(declarator);
        if (!initialised) {
            final Location endLocation = AstUtils.getEndLocation(
                    asmStmt.isPresent() ? asmStmt.get().getEndLocation() : declarator.getEndLocation(),
                    association.getTypeElements(),
                    attributes);
            variableDecl.setEndLocation(endLocation);
        }

        // Resolve and save type
        final Optional<Type> declaratorType = association.resolveType(Optional.of(declarator),
                environment, errorHelper, variableDecl.getLocation(),
                variableDecl.getLocation(), semanticListener, attributeAnalyzer);
        final Optional<Type> type = TypeElementUtils.isTypedef(association.getTypeElements())
                ? Optional.<Type>of(TypeDefinitionType.getInstance())
                : declaratorType;
        variableDecl.setType(type);

        // Determine linkage
        Optional<Linkage> linkage = Optional.absent();
        if (association.containsMainSpecifier(errorHelper)) {
            final Optional<NonTypeSpecifier> mainSpecifier = association.getMainSpecifier(errorHelper);
            if (identifier.isPresent() && type.isPresent()) {
                linkage = determineLinkage(identifier.get(), environment, mainSpecifier, type.get());
            }
        }

        // Check the type
        final Interval markerArea = getIdentifierInterval(declarator)
                .or(Interval.of(declarator.getLocation(), declarator.getEndLocation()));
        checkVariableType(type, linkage, identifier, markerArea, errorHelper);

        final StartDeclarationVisitor declarationVisitor = new StartDeclarationVisitor(environment, variableDecl,
                declarator, asmStmt, association.getTypeElements(), attributes, linkage, declaratorType,
                buildingUsesProvides);
        declarator.accept(declarationVisitor, null);
        return variableDecl;
    }

    public VariableDecl finishDecl(VariableDecl declaration, Environment environment,
                                   Optional<Expression> initializer) {
        if (initializer.isPresent()) {
            if (!AstUtils.IS_INITIALIZER.apply(initializer.get())) {
                ExpressionsAnalysis.analyze(initializer.get(), environment, errorHelper);
            }
            final Location endLocation = initializer.get().getEndLocation();
            declaration.setEndLocation(endLocation);
        }
        declaration.setInitializer(initializer);
        return declaration;
    }

    public DataDecl makeDataDecl(Environment environment, Location startLocation, Location endLocation,
                                 TypeElementsAssociation association, LinkedList<Declaration> decls) {
        // Process potential tag declarations from the type specifiers
        final Optional<Type> type = association.getType(environment, decls.isEmpty(), errorHelper,
                startLocation, startLocation, semanticListener, attributeAnalyzer);
        association.containsMainSpecifier(errorHelper);

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
    public Declarator finishArrayOrFnDeclarator(Optional<Declarator> nested, NestedDeclarator declarator) {
        if (nested.isPresent()) {
            declarator.setLocation(nested.get().getLocation());
        }
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
        final Optional<Type> maybeType = resolveType(environment, modifiers, Optional.of(declarator),
                errorHelper, startLocation, startLocation, semanticListener, attributeAnalyzer);
        final SpecifiersSet specifiersSet = new SpecifiersSet(modifiers, errorHelper);

        final StartFunctionVisitor startVisitor = new StartFunctionVisitor(environment,
                functionDecl, modifiers, attributes, specifiersSet, maybeType);
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

        /* Resolve the type and adjust it if necessary. */
        final Optional<Type> declaratorType = resolveType(environment, elements, declarator,
                errorHelper, varStartLocation, varEndLocation, semanticListener, attributeAnalyzer);
        final Optional<Type> adjustedType = environment.getScopeType() == ScopeType.FUNCTION_PARAMETER
                ? declaratorType.transform(DECAY_TRANSFORMATION)
                : declaratorType;
        variableDecl.setType(adjustedType);

        if (declarator.isPresent()) {
            final Optional<String> name = getDeclaratorName(declarator.get());
            final Optional<String> uniqueName = mangleDeclaratorName(declarator.get(), manglingFunction);

            if (name.isPresent()) {
                final VariableDeclaration symbol = VariableDeclaration.builder()
                        .uniqueName(uniqueName.get())
                        .type(variableDecl.getType().orNull())
                        .linkage(Linkage.NONE)
                        .name(name.get())
                        .startLocation(declarator.get().getLocation())
                        .build();
                if (!environment.getObjects().add(name.get(), symbol)) {
                    errorHelper.error(declarator.get().getLocation(), Optional.of(declarator.get().getEndLocation()),
                            format("redeclaration of '%s'", name.get()));
                }
                variableDecl.setDeclaration(symbol);
                attributeAnalyzer.analyzeAttributes(AstUtils.joinAttributes(elements, attributes),
                        symbol, environment);
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

        // TODO update symbol table, currently old-style declarations are ignored

        return decl;
    }

    public TagRef startNamedStruct(Environment environment, Location startLocation,
                                   Location endLocation, StructKind kind, Word tag) {
        return startNamedTagDefinition(environment, startLocation, endLocation, kind, tag);
    }

    public TagRef startNamedEnum(Environment environment, Location startLocation,
                                 Location endLocation, Word tag) {
        return startNamedTagDefinition(environment, startLocation, endLocation, StructKind.ENUM, tag);
    }

    private TagRef startNamedTagDefinition(Environment environment, Location startLocation,
                                           Location endLocation, StructKind kind, Word tag) {
        final TagRef result = makeTagRef(environment, startLocation, endLocation, kind, Optional.of(tag),
                Lists.<Declaration>newList(), Lists.<Attribute>newList(),
                TagRefSemantics.PREDEFINITION);
        processTagReference(result, environment, true, errorHelper, semanticListener, attributeAnalyzer);
        return result;
    }

    public void finishNamedTagDefinition(TagRef tagRef, Environment environment, Location endLocation,
                                         LinkedList<Declaration> fields, LinkedList<Attribute> attributes) {
        tagRef.setFields(fields);
        tagRef.setAttributes(attributes);
        tagRef.setEndLocation(endLocation);
        tagRef.setSemantics(TagRefSemantics.DEFINITION);
        processTagReference(tagRef, environment, true, errorHelper, semanticListener, attributeAnalyzer);
    }

    public TagRef makeStruct(Environment environment, Location startLocation, Location endLocation,
                             StructKind kind, Optional<Word> tag,  LinkedList<Declaration> fields,
                             LinkedList<Attribute> attributes) {
        return makeTagRef(environment, startLocation, endLocation, kind, tag, fields, attributes,
                          TagRefSemantics.DEFINITION);
    }

    public TagRef makeEnum(Environment environment, Location startLocation, Location endLocation, Optional<Word> tag,
                           LinkedList<Declaration> fields, LinkedList<Attribute> attributes) {
        return makeTagRef(environment, startLocation, endLocation, StructKind.ENUM, tag, fields,
                          attributes, TagRefSemantics.DEFINITION);
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
    public TagRef makeXrefTag(Environment environment, Location startLocation, Location endLocation,
                              StructKind structKind, Word tag) {
        return makeTagRef(environment, startLocation, endLocation, structKind, Optional.of(tag));
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
                startLocation, endLocation, semanticListener, attributeAnalyzer);

        // Names of fields are not mangled so set a unique name that is absent
        if (declarator.isPresent()) {
            DeclaratorUtils.setUniqueName(declarator.get(), Optional.<String>absent());
        }

        // FIXME: elements?
        endLocation = getEndLocation(endLocation, attributes);
        final FieldDecl decl = new FieldDecl(startLocation, declarator, attributes, bitfield);
        decl.setEndLocation(endLocation);
        makeFieldDeclaration(decl, maybeBaseType, environment, errorHelper);

        return decl;
    }

    public Enumerator makeEnumerator(Environment environment, Location startLocation, Location endLocation, String id,
                                     Optional<Expression> value) {
        if (value.isPresent()) {
            ExpressionsAnalysis.analyze(value.get(), environment, errorHelper);
        }

        final Enumerator enumerator = new Enumerator(startLocation, id, value.orNull());
        enumerator.setEndLocation(endLocation);

        final ConstantDeclaration symbol = ConstantDeclaration.builder()
                .uniqueName(semanticListener.nameManglingRequired(id))
                .name(id)
                .startLocation(startLocation)
                .build();

        // Emit a global name event for enumeration constants in global scope
        if (environment.getScopeType() == ScopeType.GLOBAL) {
            semanticListener.globalName(symbol.getUniqueName(), id);
        }

        if (!environment.getObjects().add(id, symbol)) {
            errorHelper.error(startLocation, Optional.of(endLocation), format("redeclaration of '%s'", id));
        }

        enumerator.setDeclaration(symbol);
        enumerator.setUniqueName(symbol.getUniqueName());
        enumerator.setNestedInNescEntity(environment.isEnclosedInNescEntity());

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
        final AstType type = new AstType(startLocation, declarator, elements);
        type.setEndLocation(endLocation);

        // Resolve the type
        type.setType(resolveType(environment, elements, declarator, errorHelper,
                                 startLocation, endLocation, semanticListener, attributeAnalyzer));

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
                declarator, qualifiers);
        qualifiedDeclarator.setEndLocation(declarator.isPresent()
                ? declarator.get().getEndLocation()
                : endLocation);

        final PointerDeclarator pointerDeclarator = new PointerDeclarator(startLocation,
                Optional.<Declarator>of(qualifiedDeclarator));
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

    private TagRef makeTagRef(Environment environment, Location startLocation, Location endLocation,
                              StructKind structKind, Optional<Word> tag) {
        final LinkedList<Attribute> attributes = Lists.newList();
        final LinkedList<Declaration> declarations = Lists.newList();
        return makeTagRef(environment, startLocation, endLocation, structKind, tag, declarations,
                          attributes, TagRefSemantics.OTHER);
    }

    private TagRef makeTagRef(Environment environment, Location startLocation, Location endLocation,
                              StructKind structKind, Optional<Word> tag, LinkedList<Declaration> declarations,
                              LinkedList<Attribute> attributes, TagRefSemantics semantics) {
        final TagRef tagRef;
        switch (structKind) {
            case STRUCT:
                tagRef = new StructRef(startLocation, attributes, declarations, tag.orNull(), semantics);
                break;
            case UNION:
                tagRef = new UnionRef(startLocation, attributes, declarations, tag.orNull(), semantics);
                break;
            case NX_STRUCT:
                tagRef = new NxStructRef(startLocation, attributes, declarations, tag.orNull(), semantics);
                break;
            case NX_UNION:
                tagRef = new NxUnionRef(startLocation, attributes, declarations, tag.orNull(), semantics);
                break;
            case ENUM:
                tagRef = new EnumRef(startLocation, attributes, declarations, tag.orNull(), semantics);
                break;
            case ATTRIBUTE:
                tagRef = new AttributeRef(startLocation, attributes, declarations, tag.orNull(), semantics);
                break;
            default:
                throw new IllegalArgumentException("Unexpected argument " + structKind);
        }
        tagRef.setEndLocation(endLocation);
        tagRef.setIsInvalid(false);
        tagRef.setNestedInNescEntity(environment.isEnclosedInNescEntity());
        return tagRef;
    }

    /**
     * Set the module table that will be notified about definitions of commands
     * and events. The table must be set when parsing the implementation of
     * a&nbsp;module.
     *
     * @param moduleTable Module table to set.
     * @throws NullPointerException Given argument is null.
     * @throws IllegalStateException A module table has been already set.
     */
    public void setModuleTable(ModuleTable moduleTable) {
        checkNotNull(moduleTable, "module table cannot be null");
        checkState(!this.moduleTable.isPresent(), "module table has been already set");

        this.moduleTable = Optional.of(moduleTable);
    }

    private class StartFunctionVisitor extends ExceptionVisitor<Void, Void> {

        private final Environment environment;
        private final FunctionDecl functionDecl;
        private final LinkedList<TypeElement> modifiers;
        private final LinkedList<Attribute> attributes;
        private final SpecifiersSet specifiersSet;
        private final Optional<Type> maybeType;

        private Location startLocation;
        private Location errorStartLocation;
        private Optional<ImmutableList<Optional<Type>>> providedInstanceParams = Optional.absent();
        private FunctionDeclaration.Builder funDeclBuilder;
        private Supplier<String> uniqueNameSupplier;

        public StartFunctionVisitor(Environment environment, FunctionDecl functionDecl,
                                    LinkedList<TypeElement> modifiers, LinkedList<Attribute> attributes,
                                    SpecifiersSet specifiersSet, Optional<Type> maybeType) {
            this.environment = environment;
            this.functionDecl = functionDecl;
            this.modifiers = modifiers;
            this.attributes = AstUtils.joinAttributes(modifiers, attributes);
            this.specifiersSet = specifiersSet;
            this.maybeType = maybeType;
        }

        @Override
        public Void visitPointerDeclarator(PointerDeclarator pointerDeclarator, Void arg) {
            if (pointerDeclarator.getDeclarator().isPresent()) {
                pointerDeclarator.getDeclarator().get().accept(this, null);
            }
            return null;
        }

        @Override
        public Void visitQualifiedDeclarator(QualifiedDeclarator qualifiedDeclarator, Void arg) {
            if (qualifiedDeclarator.getDeclarator().isPresent()) {
                qualifiedDeclarator.getDeclarator().get().accept(this, null);
            }
            return null;
        }

        @Override
        public Void visitFunctionDeclarator(FunctionDeclarator funDeclarator, Void arg) {
            final Declarator innerDeclarator = funDeclarator.getDeclarator().get();

            // Check types of parameters
            if (maybeType.isPresent()) {
                final Type type = maybeType.get();
                assert type.isFunctionType() : "unexpected type of a function in its definition '" + type.getClass().getCanonicalName() + "'";
                final FunctionType funType = (FunctionType) type;
                checkFunctionParametersTypes(funType.getArgumentsTypes(), funDeclarator.getParameters(),
                                             errorHelper);
            }

            providedInstanceParams = funDeclarator.getGenericParameters().isPresent()
                    ? Optional.of(AstUtils.getTypes(funDeclarator.getGenericParameters().get()))
                    : Optional.<ImmutableList<Optional<Type>>>absent();

            startLocation = funDeclarator.getLocation();

            errorStartLocation = !modifiers.isEmpty()
                    ? modifiers.getFirst().getLocation()
                    : startLocation;

            /* C function/task */
            if (innerDeclarator instanceof IdentifierDeclarator) {
                identifierDeclarator(funDeclarator, (IdentifierDeclarator) innerDeclarator);
            }
            /* command/event */
            else if (innerDeclarator instanceof InterfaceRefDeclarator) {
                interfaceRefDeclarator(funDeclarator, (InterfaceRefDeclarator) innerDeclarator);
            } else {
                throw new IllegalStateException("Unexpected declarator class " + innerDeclarator.getClass());
            }
            return null;
        }

        private void identifierDeclarator(final FunctionDeclarator funDeclarator, IdentifierDeclarator identifierDeclarator) {
            final String name = identifierDeclarator.getName();

            funDeclBuilder = FunctionDeclaration.builder();
            funDeclBuilder.instanceParameters(funDeclarator.getGenericParameters().orNull())
                    .type(maybeType.orNull())
                    .name(name)
                    .startLocation(startLocation);

            uniqueNameSupplier = new Supplier<String>() {
                @Override
                public String get() {
                    return mangleDeclaratorName(funDeclarator, manglingFunction).get();
                }
            };

            if (!specifiersSet.goodMainSpecifier()) {
                final ErroneousIssue error = new InvalidSpecifiersCombinationError(specifiersSet.getMainSpecifiers());
                errorHelper.error(errorStartLocation, funDeclarator.getEndLocation(), error);
            }

            final Optional<NonTypeSpecifier> mainSpecifier = specifiersSet.firstMainSpecifier();
            final Optional<InterfaceEntity.Kind> kind = getDeclaredKind();

            if (kind.isPresent()) {
                bareEntity(kind.get(), name, funDeclarator);
            } else if (mainSpecifier.isPresent() && mainSpecifier.get() == NonTypeSpecifier.TASK) {
                task(name, funDeclarator);
            } else {
                regularFunction(name, funDeclarator);
            }
        }

        private void interfaceRefDeclarator(FunctionDeclarator funDeclarator, InterfaceRefDeclarator refDeclaration) {
            final String ifaceName = refDeclaration.getName().getName();
            final Declarator innerDeclarator = refDeclaration.getDeclarator().get();

            if (innerDeclarator instanceof IdentifierDeclarator) {
                final IdentifierDeclarator idDeclarator = (IdentifierDeclarator) innerDeclarator;
                final String callableName = idDeclarator.getName();

                final Optional<? extends ErroneousIssue> error =
                        checkInterfaceRefDeclarator(ifaceName, callableName, funDeclarator);
                if (error.isPresent()) {
                    Declarations.this.errorHelper.error(errorStartLocation,
                            funDeclarator.getEndLocation(), error.get());
                }

                final FunctionDeclaration declaration = FunctionDeclaration.builder()
                        .uniqueName(mangleDeclaratorName(funDeclarator, manglingFunction).get())
                        .instanceParameters(funDeclarator.getGenericParameters().orNull())
                        .interfaceName(ifaceName)
                        .type(maybeType.orNull())
                        .name(callableName)
                        .startLocation(startLocation)
                        .build();
                declaration.setAstFunctionDeclarator(funDeclarator);
                define(declaration, funDeclarator);
                declaration.setDefined(true);
                functionDecl.setDeclaration(declaration);
                attributeAnalyzer.analyzeAttributes(attributes, declaration, environment);
            } else {
                throw new IllegalStateException("Unexpected declarator class " + innerDeclarator.getClass());
            }
        }

        private void bareEntity(InterfaceEntity.Kind kind, String name, FunctionDeclarator funDeclarator) {

            // Scope

            if (environment.getScopeType() != ScopeType.MODULE_IMPLEMENTATION) {
                finishWithError(InvalidInterfaceEntityDefinitionError.invalidScope(), false, funDeclarator, getFunctionType(kind));
                return;
            }

            // Declaration in the specification

            final Optional<? extends ObjectDeclaration> optBareDeclaration =
                    environment.getParent().get().getObjects().get(name, true);

            if (!optBareDeclaration.isPresent() || optBareDeclaration.get().getKind() != ObjectKind.FUNCTION) {
                finishWithError(InvalidInterfaceEntityDefinitionError.undeclaredBareEntity(kind, name), false, funDeclarator, getFunctionType(kind));
                return;
            }

            final FunctionDeclaration bareDeclaration = (FunctionDeclaration) optBareDeclaration.get();
            final Optional<InterfaceEntity.Kind> expectedKind = getInterfaceEntityKind(bareDeclaration.getFunctionType());

            if (!expectedKind.isPresent()) {
                finishWithError(InvalidInterfaceEntityDefinitionError.undeclaredBareEntity(kind, name), false, funDeclarator, getFunctionType(kind));
                return;
            }

            if (!bareDeclaration.isProvided().isPresent()) {
                finishWithError(InvalidInterfaceEntityDefinitionError.undeclaredBareEntity(kind, name), false, funDeclarator, getFunctionType(kind));
                return;
            }

            // Attributes analysis

            attributeAnalyzer.analyzeAttributes(attributes, bareDeclaration, environment);

            // Further correctness aspects

            final Optional<? extends ErroneousIssue> error =
                    checkInterfaceEntity(Optional.<String>absent(), name, bareDeclaration.getInstanceParameters(),
                            expectedKind.get(), bareDeclaration.getType(), bareDeclaration.isProvided().get(),
                            Optional.<Type>absent());

            if (error.isPresent()) {
                finishWithError(error.get(), false, funDeclarator, getFunctionType(kind));
                return;
            }

            if (bareDeclaration.isDefined()) {
                finishWithError(new RedefinitionError(name, RedefinitionKind.FUNCTION), false, funDeclarator, getFunctionType(kind));
                return;
            }

            // Update state

            checkState(moduleTable.isPresent(), "module table unexpectedly absent");

            moduleTable.get().markFulfilled(name);
            bareDeclaration.setDefined(true);
            bareDeclaration.setAstFunctionDeclarator(funDeclarator);
            functionDecl.setDeclaration(bareDeclaration);
            DeclaratorUtils.setUniqueName(funDeclarator, Optional.of(bareDeclaration.getUniqueName()));
        }

        private void task(String name, FunctionDeclarator funDeclarator) {
            final Optional<? extends ErroneousIssue> error;

            if (environment.getScopeType() != ScopeType.MODULE_IMPLEMENTATION) {
                error = Optional.of(InvalidTaskDeclarationError.invalidDefinitionScope());
            } else if (funDeclarator.getGenericParameters().isPresent()) {
                error = Optional.of(InvalidTaskDeclarationError.instanceParametersPresent(
                        funDeclarator.getGenericParameters().get().size()));
            } else if (maybeType.isPresent() && !maybeType.get().isCompatibleWith(TYPE_TASK)) {
                error = Optional.of(InvalidTaskDeclarationError.invalidType(name, maybeType.get()));
            } else {
                error = Optional.absent();
            }

            /* The task is accepted as implemented only if the location of its
               definition is correct. */
            if (environment.getScopeType() == ScopeType.MODULE_IMPLEMENTATION) {
                checkState(moduleTable.isPresent(), "module table unexpectedly absent");
                moduleTable.get().addTask(name);
                moduleTable.get().taskImplemented(name);
            }

            if (error.isPresent()) {
                finishWithError(error.get(), true, funDeclarator, FunctionDeclaration.FunctionType.TASK);
            } else {
                finish(name, funDeclarator, FunctionDeclaration.FunctionType.TASK);
            }
        }

        private void regularFunction(String name, FunctionDeclarator funDeclarator) {
            final FunctionDeclaration.FunctionType funKind =
                    TypeElementUtils.getFunctionType(modifiers);

            if (providedInstanceParams.isPresent()) {
                finishWithError(InvalidFunctionDeclarationError.instanceParametersPresent(name),
                        true, funDeclarator, funKind);
                return;
            }

            finish(name, funDeclarator, funKind);
        }

        private Optional<? extends ErroneousIssue> checkInterfaceRefDeclarator(String ifaceName,
                String callableName, FunctionDeclarator funDeclarator) {

            final Optional<? extends ObjectDeclaration> optDeclaration =
                    environment.getObjects().get(ifaceName);

            if (!optDeclaration.isPresent()) {
                return Optional.of(new UndeclaredIdentifierError(ifaceName));
            } else if (optDeclaration.get().getKind() != ObjectKind.INTERFACE) {
                return Optional.of(InvalidInterfaceEntityDefinitionError.interfaceExpected(ifaceName));
            }

            // Reference to an existing command or event

            final InterfaceRefFacade ifaceFacade = ((InterfaceRefDeclaration) optDeclaration.get()).getFacade();
            if (!ifaceFacade.goodInterfaceRef()) {
                return Optional.absent();
            }
            final Optional<InterfaceEntity> optIfaceEntity = ifaceFacade.get(callableName);
            if (!optIfaceEntity.isPresent()) {
                return Optional.of(InvalidInterfaceEntityDefinitionError.nonexistentInterfaceEntity(
                        ifaceFacade.getInterfaceName(), ifaceFacade.getInstanceName(), callableName));
            }

            // Further aspects

            final InterfaceEntity ifaceEntity = optIfaceEntity.get();
            final boolean isProvided = ifaceEntity.getKind() == InterfaceEntity.Kind.COMMAND && ifaceFacade.isProvided()
                    || ifaceEntity.getKind() == InterfaceEntity.Kind.EVENT && !ifaceFacade.isProvided();
            final Optional<? extends ErroneousIssue> error = checkInterfaceEntity(Optional.of(ifaceName),
                    callableName,ifaceFacade.getInstanceParameters(), ifaceEntity.getKind(),
                    ifaceEntity.getType(), isProvided, optDeclaration.get().getType());
            if (error.isPresent()) {
                return error;
            }

            // Scope

            final String name = format("%s.%s", ifaceName, callableName);
            if (environment.getScopeType() != ScopeType.MODULE_IMPLEMENTATION) {
                return Optional.of(InvalidInterfaceEntityDefinitionError.invalidScope());
            }

            // Everything is alright so mark the command or event as implemented

            if (!environment.getObjects().contains(name, true)) {
                moduleTable.get().markFulfilled(name);
            }

            return Optional.absent();
        }

        private Optional<InterfaceEntity.Kind> getDeclaredKind() {
            if (!specifiersSet.goodMainSpecifier()) {
                return Optional.absent();
            }

            final Optional<NonTypeSpecifier> mainSpecifier = specifiersSet.firstMainSpecifier();

            if (!mainSpecifier.isPresent()) {
                return Optional.absent();
            }

            switch (mainSpecifier.get()) {
                case COMMAND:
                    return Optional.of(InterfaceEntity.Kind.COMMAND);
                case EVENT:
                    return Optional.of(InterfaceEntity.Kind.EVENT);
                default:
                    return Optional.absent();
            }
        }

        private Optional<InterfaceEntity.Kind> getInterfaceEntityKind(FunctionDeclaration.FunctionType funKind) {
            switch (funKind) {
                case COMMAND:
                    return Optional.of(InterfaceEntity.Kind.COMMAND);
                case EVENT:
                    return Optional.of(InterfaceEntity.Kind.EVENT);
                default:
                    return Optional.absent();
            }
        }

        private Optional<? extends ErroneousIssue> checkInterfaceEntity(Optional<String> ifaceName,
                    String name, Optional<ImmutableList<Optional<Type>>> expectedInstanceParams,
                    InterfaceEntity.Kind expectedKind, Optional<? extends Type> expectedType,
                    boolean isProvided, Optional<Type> interfaceType) {

            // Instance parameters

            final Optional<? extends ErroneousIssue> instanceParamsError =
                    checkInstanceParameters(expectedInstanceParams, providedInstanceParams,
                                            ifaceName, name);

            if (instanceParamsError.isPresent()) {
                return instanceParamsError;
            }

            // Correct presence of 'command' or 'event' specifier

            final Optional<InterfaceEntity.Kind> declaredKind = getDeclaredKind();
            if (!declaredKind.isPresent() || declaredKind.get() != expectedKind) {
                return Optional.of(InvalidInterfaceEntityDefinitionError.interfaceEntitySpecifierExpected(expectedKind));
            }

            // Presence or absence of 'default' specifier

            final boolean isDefault = specifiersSet.contains(NonTypeSpecifier.DEFAULT);

            if (isProvided == isDefault) {
                return Optional.of(InvalidInterfaceEntityDefinitionError.providedDefaultMismatch(ifaceName,
                        name, expectedKind, isProvided));
            }

            // Type
            if (expectedType.isPresent() && maybeType.isPresent()
                    && !maybeType.get().isCompatibleWith(expectedType.get())) {

                return Optional.of(InvalidInterfaceEntityDefinitionError.invalidType(ifaceName,
                        name, expectedKind, expectedType.get(), maybeType.get(), interfaceType));
            }

            return Optional.absent();
        }

        private Optional<? extends ErroneousIssue> checkInstanceParameters(Optional<ImmutableList<Optional<Type>>> optExpectedParams,
                    Optional<ImmutableList<Optional<Type>>> optProvidedParams, Optional<String> ifaceName, String callableName) {

            if (optExpectedParams.isPresent() && !optProvidedParams.isPresent()) {
                return Optional.of(InvalidInterfaceEntityDefinitionError.instanceParametersExpected(ifaceName, callableName));
            } else if (!optExpectedParams.isPresent() && optProvidedParams.isPresent()) {
                return Optional.of(InvalidInterfaceEntityDefinitionError.unexpectedInstanceParameters(ifaceName, callableName));
            } else if (optExpectedParams.isPresent()) {

                final ImmutableList<Optional<Type>> expectedParams = optExpectedParams.get();
                final ImmutableList<Optional<Type>> providedParams = optProvidedParams.get();

                if (expectedParams.size() != providedParams.size()) {
                    return Optional.of(InvalidInterfaceEntityDefinitionError.invalidInstanceParametersCount(ifaceName,
                            callableName, expectedParams.size(), providedParams.size()));
                }

                // Check types of parameters

                for (int i = 0; i < expectedParams.size(); ++i) {
                    final Optional<Type> expectedType = expectedParams.get(i);
                    final Optional<Type> providedType = providedParams.get(i);

                    if (!expectedType.isPresent() || !providedType.isPresent()) {
                        continue;
                    }

                    if (!providedType.get().isCompatibleWith(expectedType.get())) {
                        return Optional.of(InvalidInterfaceEntityDefinitionError.invalidInstanceParamType(
                                i + 1, expectedType.get(), providedType.get()));
                    }
                }
            }

            return Optional.absent();
        }

        private void finish(String name, FunctionDeclarator funDeclarator, FunctionDeclaration.FunctionType funKind) {
            final FunctionDeclaration functionDeclaration;

            /* Check previous declaration. */
            final Optional<? extends ObjectDeclaration> previousDeclarationOpt = environment.getObjects().get(name, true);
            if (!previousDeclarationOpt.isPresent()) {
                final String uniqueName = uniqueNameSupplier.get();
                functionDeclaration = funDeclBuilder.uniqueName(uniqueName).build();
                emitGlobalNameEvent(uniqueName, getDeclaratorName(funDeclarator).get());
                define(functionDeclaration, funDeclarator);
            } else {
                final ObjectDeclaration previousDeclaration = previousDeclarationOpt.get();
                /* Trying to redeclare non-function declaration. */
                if (previousDeclaration.getKind() != ObjectKind.FUNCTION) {
                    Declarations.this.errorHelper.error(errorStartLocation, funDeclarator.getEndLocation(),
                            new RedeclarationError(name, RedeclarationKind.OTHER));

                    /* Nevertheless, create declaration, put it into ast node
                     * but not into environment. */
                    functionDeclaration = funDeclBuilder.uniqueName(uniqueNameSupplier.get()).build();
                }
                /* Previous declaration is a function declaration or
                 * definition. */
                else {
                    final FunctionDeclaration tmpDecl = (FunctionDeclaration) previousDeclaration;
                    final Optional<? extends ErroneousIssue> error;

                    if (!checkFunctionType(tmpDecl.getFunctionType(), funKind)) {
                        error = Optional.of(InvalidFunctionDeclarationError.kindMismatch(name));
                    } else if (tmpDecl.getType().isPresent() && maybeType.isPresent()
                            && !maybeType.get().isCompatibleWith(tmpDecl.getType().get())) {
                        error = Optional.of(InvalidFunctionDeclarationError.typeMismatch(name,
                                tmpDecl.getType().get(), maybeType.get()));
                    } else if (tmpDecl.isDefined()) {
                        /* Function redefinition is forbidden. */
                        error = Optional.of(new RedefinitionError(name, RedefinitionKind.FUNCTION));
                    } else {
                        error = Optional.absent();
                    }

                    if (error.isPresent()) {
                        Declarations.this.errorHelper.error(errorStartLocation,
                                funDeclarator.getEndLocation(), error.get());
                        functionDeclaration = funDeclBuilder.uniqueName(uniqueNameSupplier.get()).build();
                    } else {
                        /* Update previous declaration. */
                        functionDeclaration = tmpDecl;
                        functionDeclaration.setLocation(startLocation);
                        DeclaratorUtils.setUniqueName(funDeclarator, Optional.of(functionDeclaration.getUniqueName()));
                        emitGlobalNameEvent(functionDeclaration.getUniqueName(), getDeclaratorName(funDeclarator).get());
                    }
                }
            }

            updateState(functionDeclaration, funDeclarator, funKind);
            attributeAnalyzer.analyzeAttributes(attributes, functionDeclaration, environment);
        }

        private void finishWithError(ErroneousIssue error, boolean updateSymbolTable,
                FunctionDeclarator funDecl, FunctionDeclaration.FunctionType funKind) {
            // Report the error
            Declarations.this.errorHelper.error(errorStartLocation, funDecl.getEndLocation(), error);

            // Update declaration and AST node
            final FunctionDeclaration declaration = funDeclBuilder.uniqueName(uniqueNameSupplier.get()).build();
            updateState(declaration, funDecl, funKind);

            if (updateSymbolTable) {
                // Try to add the declaration to the symbol table
                environment.getObjects().add(declaration.getName(), declaration);
            }
        }

        private void updateState(FunctionDeclaration functionDeclaration, FunctionDeclarator funDeclarator,
                FunctionDeclaration.FunctionType funKind) {

            functionDeclaration.setAstFunctionDeclarator(funDeclarator);
            functionDeclaration.setFunctionType(funKind);
            functionDeclaration.setDefined(true);
            functionDecl.setDeclaration(functionDeclaration);
        }

        private FunctionDeclaration.FunctionType getFunctionType(InterfaceEntity.Kind ifaceEntityKind) {
            switch (ifaceEntityKind) {
                case COMMAND:
                    return FunctionDeclaration.FunctionType.COMMAND;
                case EVENT:
                    return FunctionDeclaration.FunctionType.EVENT;
                default:
                    throw new RuntimeException("unexpected interface entity kind '" + ifaceEntityKind + "'");
            }
        }

        private boolean checkFunctionType(FunctionDeclaration.FunctionType previousKind,
                FunctionDeclaration.FunctionType actualKind) {

            final boolean mustMatchExactly = previousKind == FunctionDeclaration.FunctionType.COMMAND
                    || previousKind == FunctionDeclaration.FunctionType.EVENT
                    || previousKind == FunctionDeclaration.FunctionType.TASK
                    || actualKind == FunctionDeclaration.FunctionType.COMMAND
                    || actualKind == FunctionDeclaration.FunctionType.EVENT
                    || actualKind == FunctionDeclaration.FunctionType.TASK;

            return !mustMatchExactly || previousKind == actualKind;
        }

        private void define(ObjectDeclaration declaration, Declarator declarator) {
            if (!environment.getObjects().add(declaration.getName(), declaration)) {
                Declarations.this.errorHelper.error(errorStartLocation, declarator.getEndLocation(),
                        new RedefinitionError(declaration.getName(), RedefinitionKind.FUNCTION));
            }
        }

        private void emitGlobalNameEvent(String uniqueName, String name) {
            if (environment.getScopeType() == ScopeType.GLOBAL) {
                Declarations.this.semanticListener.globalName(uniqueName, name);
            }
        }

        // TODO: adding tokens (e.g. for semantic colouring)
    }

    private class StartDeclarationVisitor extends ExceptionVisitor<Void, Void> {

        private final Environment environment;
        private final VariableDecl variableDecl;
        private final LinkedList<TypeElement> elements;
        private final SpecifiersSet nonTypeSpecifiers;
        private final LinkedList<Attribute> attributes;
        private final Optional<Type> declaratorType;
        private final Optional<Linkage> linkage;
        private final Interval errorInterval;
        private final boolean buildingUsesProvides;

        @SuppressWarnings("UnusedParameters")
        StartDeclarationVisitor(Environment environment, VariableDecl variableDecl, Declarator declarator,
                                Optional<AsmStmt> asmStmt, LinkedList<TypeElement> elements,
                                LinkedList<Attribute> attributes, Optional<Linkage> linkage,
                                Optional<Type> declaratorType, boolean buildingUsesProvides) {
            this.environment = environment;
            this.variableDecl = variableDecl;
            this.elements = elements;
            this.nonTypeSpecifiers = new SpecifiersSet(elements, errorHelper);
            this.attributes = AstUtils.joinAttributes(elements, attributes);
            this.linkage = linkage;
            this.declaratorType = declaratorType;
            this.errorInterval = Interval.builder()
                    .endLocation(declarator.getEndLocation())
                    .startLocation(elements.isEmpty() ? declarator.getLocation() : elements.getFirst().getLocation())
                    .build();
            this.buildingUsesProvides = buildingUsesProvides;
        }

        @Override
        public Void visitFunctionDeclarator(final FunctionDeclarator funDeclarator, Void arg) {
            if (!isFunctionDeclared(funDeclarator)) {
                funDeclarator.getDeclarator().get().accept(this, null);
                return null;
            }

            final Optional<NonTypeSpecifier> mainSpecifier = nonTypeSpecifiers.firstMainSpecifier();

            if (!mainSpecifier.isPresent()) {
                regularFunction(funDeclarator);
            } else if (mainSpecifier.get() == NonTypeSpecifier.TASK) {
                task(funDeclarator);
            } else if (mainSpecifier.get() == NonTypeSpecifier.COMMAND
                    || mainSpecifier.get() == NonTypeSpecifier.EVENT) {
                bareEntity(funDeclarator);
            } else {
                // FIXME error if invalid specifier, e.g. register or auto
                regularFunction(funDeclarator);
            }

            return null;
        }

        private void regularFunction(FunctionDeclarator funDeclarator) {
            final Optional<? extends ErroneousIssue> error;

            if (funDeclarator.getGenericParameters().isPresent()) {
                error = Optional.of(InvalidFunctionDeclarationError.instanceParametersPresent(
                        getDeclaratorName(funDeclarator).get()));
            } else if (buildingUsesProvides) {
                error = Optional.of(InvalidSpecificationDeclarationError.expectedBareEntity());
            } else {
                error = Optional.absent();
            }

            if (error.isPresent()) {
                errorHelper.error(errorInterval.getLocation(), errorInterval.getEndLocation(),
                        error.get());
            }

            finishFunction(funDeclarator);
        }

        private void task(FunctionDeclarator funDeclarator) {
            final Optional<? extends ErroneousIssue> error;
            final String name = getDeclaratorName(funDeclarator).get();
            final Optional<? extends ObjectDeclaration> optPreviousDeclaration =
                    environment.getObjects().get(name, true);

            if (buildingUsesProvides) {
                error = Optional.of(InvalidSpecificationDeclarationError.expectedBareEntity());
            } else if (!environment.isEnclosedInScope(ScopeType.MODULE_IMPLEMENTATION)) {
                error = Optional.of(InvalidTaskDeclarationError.invalidDeclarationScope());
            } else if (funDeclarator.getGenericParameters().isPresent()) {
                error = Optional.of(InvalidTaskDeclarationError.instanceParametersPresent(
                        funDeclarator.getGenericParameters().get().size()));
            } else if (declaratorType.isPresent() && !declaratorType.get().isCompatibleWith(TYPE_TASK)) {
                error = Optional.of(InvalidTaskDeclarationError.invalidType(
                        name, declaratorType.get()));
            } else {
                error = Optional.absent();
            }

            if (error.isPresent()) {
                errorHelper.error(errorInterval.getLocation(), errorInterval.getEndLocation(),
                        error.get());
            }

            /* Add the task only if it is the first declaration with the name in
               this scope. If it is redeclaration, then invalid task will not be
               added. If it is correct, then the task has been added when
               processing the first declaration. */
            if (moduleTable.isPresent() && !optPreviousDeclaration.isPresent()) {
                moduleTable.get().addTask(name);
            }

            finishFunction(funDeclarator);
        }

        private void bareEntity(FunctionDeclarator funDeclarator) {
            if ((environment.getScopeType() != ScopeType.SPECIFICATION || !buildingUsesProvides)
                    && environment.getScopeType() != ScopeType.INTERFACE) {
                errorHelper.error(errorInterval.getLocation(), errorInterval.getEndLocation(),
                        InvalidBareEntityDeclarationError.invalidScope());
            }

            finishFunction(funDeclarator);
        }

        private void finishFunction(final FunctionDeclarator funDeclarator) {
            // FIXME: refactoring needed
            /*
             * Function declaration (not definition!). There can be many
             * declarations but only one definition.
             * All declarations and definition must have the same return type
             * and types of parameters.
             */
            variableDecl.setForward(true);
            final Optional<String> name = getDeclaratorName(funDeclarator);
            final FunctionDeclaration functionDeclaration;
            /*
             * Check previous declarations.
             */
            final Optional<? extends ObjectDeclaration> previousDeclarationOpt =
                    environment.getObjects().get(name.get(), true);
            final FunctionDeclaration.Builder builder = FunctionDeclaration.builder();
            builder.functionType(TypeElementUtils.getFunctionType(elements))
                    .instanceParameters(funDeclarator.getGenericParameters().orNull())
                    .type(variableDecl.getType().orNull())
                    .linkage(linkage.orNull())
                    .name(name.get())
                    .startLocation(funDeclarator.getLocation());

            final Supplier<String> uniqueNameSupplier = new Supplier<String> () {
                @Override
                public String get() {
                    return mangleDeclaratorName(funDeclarator, manglingFunction).get();
                }
            };

            if (!previousDeclarationOpt.isPresent()) {
                final String uniqueName = uniqueNameSupplier.get();
                functionDeclaration = builder.uniqueName(uniqueName).build();
                emitGlobalNameEvent(uniqueName, name.get());
                declare(functionDeclaration, funDeclarator);
            } else {
                final ObjectDeclaration previousDeclaration = previousDeclarationOpt.get();
                /* Trying to redeclare non-function declaration. */
                if (!(previousDeclaration instanceof FunctionDeclaration)) {
                    Declarations.this.errorHelper.error(funDeclarator.getLocation(), funDeclarator.getEndLocation(),
                            new RedeclarationError(name.get(), RedeclarationKind.OTHER));

                    /* Nevertheless, create declaration, put it into ast node
                     * but not into environment. */
                    functionDeclaration = builder.uniqueName(uniqueNameSupplier.get()).build();
                }
                /* Previous declaration is a function declaration or
                 * definition. */
                else {
                    functionDeclaration = (FunctionDeclaration) previousDeclaration;
                    /* Update previous declaration. */
                    functionDeclaration.setLocation(funDeclarator.getLocation());
                    DeclaratorUtils.setUniqueName(funDeclarator, Optional.of(functionDeclaration.getUniqueName()));
                    emitGlobalNameEvent(functionDeclaration.getUniqueName(), name.get());

                    // TODO: check if types match in declarations
                }
            }

            functionDeclaration.setAstFunctionDeclarator(funDeclarator);
            variableDecl.setDeclaration(functionDeclaration);
            attributeAnalyzer.analyzeAttributes(attributes, functionDeclaration, environment);
        }

        @Override
        public Void visitPointerDeclarator(PointerDeclarator declarator, Void arg) {
            if (declarator.getDeclarator().isPresent()) {
                return declarator.getDeclarator().get().accept(this, null);
            }
            return null;
        }

        @Override
        public Void visitQualifiedDeclarator(QualifiedDeclarator declarator, Void arg) {
            if (declarator.getDeclarator().isPresent()) {
                return declarator.getDeclarator().get().accept(this, null);
            }
            return null;
        }

        @Override
        public Void visitArrayDeclarator(ArrayDeclarator declarator, Void arg) {
            if (declarator.getDeclarator().isPresent()) {
                return declarator.getDeclarator().get().accept(this, null);
            }
            return null;
        }

        @Override
        public Void visitIdentifierDeclarator(IdentifierDeclarator declarator, Void arg) {
            final String name = declarator.getName();
            final Location startLocation = declarator.getLocation();
            final boolean isTypedef = TypeElementUtils.isTypedef(elements);
            final ObjectDeclaration.Builder<? extends ObjectDeclaration> builder;
            final String uniqueName = semanticListener.nameManglingRequired(name);

            if (buildingUsesProvides) {
                errorHelper.error(errorInterval.getLocation(), errorInterval.getEndLocation(),
                        InvalidSpecificationDeclarationError.expectedBareEntity());
            }

            if (isTypedef) {
                builder = TypenameDeclaration.builder()
                        .uniqueName(uniqueName)
                        .denotedType(declaratorType.orNull());
            } else {
                builder = VariableDeclaration.builder()
                        .uniqueName(uniqueName)
                        .type(declaratorType.orNull())
                        .linkage(linkage.orNull());
            }

            final ObjectDeclaration declaration = builder.name(name)
                    .startLocation(startLocation)
                    .build();

            emitGlobalNameEvent(uniqueName, name);
            declare(declaration, declarator);
            variableDecl.setDeclaration(declaration);
            declarator.setUniqueName(Optional.of(uniqueName));
            attributeAnalyzer.analyzeAttributes(attributes, declaration, environment);
            return null;
        }

        @Override
        public Void visitInterfaceRefDeclarator(InterfaceRefDeclarator elem, Void arg) {
            Declarations.this.errorHelper.error(elem.getLocation(), Optional.of(elem.getEndLocation()),
                    "unexpected interface reference");
            return null;
        }

        private boolean isFunctionDeclared(FunctionDeclarator startDeclarator) {
            Optional<Declarator> declarator = startDeclarator.getDeclarator();

            while (declarator.isPresent() && declarator.get() instanceof NestedDeclarator) {
                final NestedDeclarator nestedDeclarator = (NestedDeclarator) declarator.get();
                if (!(nestedDeclarator instanceof QualifiedDeclarator)) {
                    return false;
                }
                declarator = nestedDeclarator.getDeclarator();
            }

            return true;
        }

        private void declare(ObjectDeclaration declaration, Declarator declarator) {
            if (!environment.getObjects().add(declaration.getName(), declaration)) {
                Declarations.this.errorHelper.error(declarator.getLocation(),
                        declarator.getEndLocation(),
                        new RedeclarationError(declaration.getName(), RedeclarationKind.OTHER));
            }
        }

        /**
         * Checks the usage of the @C() attribute and emits the global event if
         * it occurs (it happens also while declaring a function or variable in
         * the global scope).
         *
         * @param uniqueName Unique name of the object.
         * @param normalName Name from the declarator for the object.
         */
        private void emitGlobalNameEvent(String uniqueName, String normalName) {
            if (environment.getScopeType() == ScopeType.GLOBAL) {
                Declarations.this.semanticListener.globalName(uniqueName, normalName);
            }
        }

        // TODO: adding tokens (e.g. for semantic colouring)
    }

}
