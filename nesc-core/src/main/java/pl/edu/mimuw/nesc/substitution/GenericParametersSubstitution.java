package pl.edu.mimuw.nesc.substitution;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.astutil.TypeElementUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Class that is responsible for performing the substitution of generic
 * parameters for actual types and expressions. It also constructs the
 * appropriate substitution.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class GenericParametersSubstitution implements SubstitutionManager {
    /**
     * Names of type generic parameters mapped to corresponding types that
     * will replace the references to the generic parameters.
     */
    private final ImmutableMap<String, AstType> types;

    /**
     * Names of non-type generic parameters mapped to expressions with their
     * values that will replace the references of parameters.
     */
    private final ImmutableMap<String, Expression> expressions;

    /**
     * Get a builder that will create a generic parameters substitution.
     *
     * @return Newly created builder that will build a generic parameters
     *         substitution.
     */
    public static FromComponentRefBuilder forComponent() {
        return new FromComponentRefPrivateBuilder();
    }

    /**
     * Get a builder that will create a generic parameters substitution for
     * a generic interface.
     *
     * @return Newly created builder that will build a generic parameters
     *         substitution for a generic interface.
     */
    public static FromInterfaceRefBuilder forInterface() {
        return new FromInterfaceRefPrivateBuilder();
    }

    /**
     * Initializes this object with information from given builder.
     *
     * @param builder Builder with information necessary to build the object.
     */
    private GenericParametersSubstitution(PrivateBuilder builder) {
        builder.buildSubstitutions();

        this.types = builder.getTypesSubstitution();
        this.expressions = builder.getExpressionsSubstitution();
    }

    @Override
    public Optional<Declaration> substitute(Declaration declaration) {
        if (declaration instanceof FunctionDecl) {
            final FunctionDecl functionDecl = (FunctionDecl) declaration;
            final Optional<Declarator> newDeclarator = performSimpleTypeSubstitution(
                    functionDecl.getModifiers(),
                    Optional.of(functionDecl.getDeclarator())
            );

            if (newDeclarator.isPresent()) {
                functionDecl.setDeclarator(newDeclarator.get());
            }
        } else if (declaration instanceof DataDecl) {
            final DataDecl dataDecl = (DataDecl) declaration;
            performChainedTypeSubstitution(dataDecl.getModifiers(), dataDecl.getDeclarations());
        }

        return Optional.absent();
    }

    @Override
    public Optional<AstType> substitute(AstType astType) {
        final Optional<Declarator> newDeclarator = performSimpleTypeSubstitution(
                astType.getQualifiers(),
                astType.getDeclarator()
        );

        if (newDeclarator.isPresent()) {
            astType.setDeclarator(newDeclarator);
        }

        return Optional.absent();
    }

    @Override
    public Optional<Expression> substitute(Expression expr) {

        /* Substitute if the expression is an identifier that refers to
           a generic parameter. */

        if (!(expr instanceof Identifier)) {
            return Optional.absent();
        }

        final Identifier identifier = (Identifier) expr;

        if (!identifier.getIsGenericReference()) {
            return Optional.absent();
        }

        return Optional.of(expressions.get(identifier.getName()).deepCopy(true));
    }

    /**
     * <p>Checks if given type elements contains a typename that refers to
     * a generic type parameter. If so, the list of specifiers is updated with
     * the substituted type. If there is a need to replace the given declarator
     * to another one, it is returned.</p>
     *
     * @param specifiers List with specifiers and potential typename.
     * @param declarator Declarator of the original type.
     * @return Declarator if there is a need to replace the given one.
     *         Otherwise, it is absent.
     */
    private Optional<Declarator> performSimpleTypeSubstitution(List<TypeElement> specifiers,
                Optional<Declarator> declarator) {

        // Check if the substitution is necessary
        final Optional<String> optReferredParamName = lookForGenericReference(specifiers);
        if (!optReferredParamName.isPresent()) {
            return Optional.absent();
        }
        final String referredParamName = optReferredParamName.get();

        // Make the substitution - prepare new nodes
        final Optional<AstType> pattern = Optional.fromNullable(types.get(referredParamName));
        checkState(pattern.isPresent(), "the substitution type for generic parameter '%s' is absent", referredParamName);
        final AstType replacement = pattern.get().deepCopy(true);
        replacement.setPastedFlagDeep(true);

        // Make the substitution - type elements
        specifiers.addAll(replacement.getQualifiers());
        cleanSpecifiers(specifiers);

        // Make the substitution - declarator
        if (combineDeclarators(replacement.getDeclarator(), declarator)) {
            return replacement.getDeclarator();
        } else {
            return Optional.absent();
        }
    }

    /**
     * <p>Performs the substitution of a generic type parameter in the case the
     * declarators are separated from type elements and there may be multiple
     * declarators. The substitution is performed if the given list of
     * specifiers contains a typename that refers to a generic type parameter.
     * </p>
     *
     * @param specifiers List with specifiers that is modified if it refers to
     *                   a generic parameters.
     * @param declarations List with declarations that contain declarators.
     */
    private void performChainedTypeSubstitution(List<TypeElement> specifiers,
            List<Declaration> declarations) {

        // Check if the substitution is necessary
        final Optional<String> optReferredParamName = lookForGenericReference(specifiers);
        if (!optReferredParamName.isPresent()) {
            return;
        }
        final String referredParamName = optReferredParamName.get();

        // Make the substitution - prepare new nodes
        final Optional<AstType> pattern = Optional.fromNullable(types.get(referredParamName));
        checkState(pattern.isPresent(), "the substitution type for generic parameter '%s' is absent", referredParamName);
        final AstType replacement = pattern.get().deepCopy(true);
        replacement.setPastedFlagDeep(true);
        final Supplier<Optional<Declarator>> declaratorSupplier =
                new DeclaratorSupplier(replacement.getDeclarator());

        // Make the substitution - type elements
        specifiers.addAll(replacement.getQualifiers());
        cleanSpecifiers(specifiers);

        // Make the substitution - declarators
        for (Declaration declaration : declarations) {
            if (declaration instanceof VariableDecl) {
                final VariableDecl varDecl = (VariableDecl) declaration;
                final Optional<Declarator> newDeclarator = declaratorSupplier.get();

                if (combineDeclarators(newDeclarator, varDecl.getDeclarator())) {
                    varDecl.setDeclarator(newDeclarator);
                }
            } else if (declaration instanceof FieldDecl) {
                final FieldDecl fieldDecl = (FieldDecl) declaration;
                final Optional<Declarator> newDeclarator = declaratorSupplier.get();

                if (combineDeclarators(newDeclarator, fieldDecl.getDeclarator())) {
                    fieldDecl.setDeclarator(newDeclarator);
                }
            } else {
                throw new IllegalStateException("unexpected inner declaration '"
                        + declaration.getClass() + "'");
            }
        }
    }

    /**
     * <p>Looks for a reference to a generic parameter in given list of
     * specifiers. If such reference is found, then the name of the referred
     * type parameter is returned and the reference is removed from the given
     * list.</p>
     *
     * @param specifiers List with specifiers to examine.
     * @return Name of the removed reference to generic type parameter from
     *         the given list if any. Otherwise, it is absent and the list is
     *         unmodified.
     */
    private Optional<String> lookForGenericReference(List<TypeElement> specifiers) {
        final Iterator<TypeElement> it = specifiers.iterator();

        while (it.hasNext()) {
            final TypeElement typeElement = it.next();

            if (typeElement instanceof Typename) {
                final Typename typename = (Typename) typeElement;

                if (typename.getIsGenericReference()) {
                    it.remove();
                    return Optional.of(typename.getName());
                }
            }
        }

        return Optional.absent();
    }

    /**
     * <p>Remove potential repetitions of type qualifiers from the given
     * specifiers list.</p>
     *
     * @param specifiers List of specifiers to clean.
     */
    private void cleanSpecifiers(List<TypeElement> specifiers) {
        boolean constOccurred = false, volatileOccurred = false;
        final Iterator<TypeElement> it = specifiers.iterator();

        while (it.hasNext()) {
            final TypeElement typeElement = it.next();
            final Optional<RID> currentRID;

            if (typeElement instanceof Rid) {
                currentRID = Optional.of(((Rid) typeElement).getId());
            } else if (typeElement instanceof Qualifier) {
                currentRID = Optional.of(((Qualifier) typeElement).getId());
            } else {
                currentRID = Optional.absent();
            }

            if (currentRID.isPresent()) {
                final boolean remove;

                switch (currentRID.get()) {
                    case CONST:
                        remove = constOccurred;
                        constOccurred = true;
                        break;
                    case VOLATILE:
                        remove = volatileOccurred;
                        volatileOccurred = true;
                        break;
                    default:
                        remove = false;
                        break;
                }

                if (remove) {
                    it.remove();
                }
            }
        }
    }

    /**
     * <p>Make the part of the type substitution that involves declarators.</p>
     *
     * @param pastedDeclarator Declarator from the generic component reference.
     * @param origDeclarator Declarator that is to be extended.
     * @return <code>true</code> if the pasted declarator should be replaced
     *         with the original one.
     */
    private boolean combineDeclarators(Optional<Declarator> pastedDeclarator,
            Optional<Declarator> origDeclarator) {

        if (!pastedDeclarator.isPresent()) {
            return false;
        }

        // Look for the bottom of the pasted declarator

        Declarator previousDeclarator = pastedDeclarator.get();
        Optional<Declarator> declaratorIt = pastedDeclarator.get() instanceof NestedDeclarator
                ? ((NestedDeclarator) pastedDeclarator.get()).getDeclarator()
                : Optional.<Declarator>absent();

        while (declaratorIt.isPresent() && declaratorIt.get() instanceof NestedDeclarator) {
            final NestedDeclarator curDeclarator = (NestedDeclarator) declaratorIt.get();

            previousDeclarator = curDeclarator;
            declaratorIt = curDeclarator.getDeclarator();
        }

        // Make the substitution

        final NestedDeclarator parentDeclarator;

        if (previousDeclarator instanceof NestedDeclarator) {
            parentDeclarator = (NestedDeclarator) previousDeclarator;
        } else {
            return false;
        }

        parentDeclarator.setDeclarator(origDeclarator);
        return true;
    }

    /**
     * <p>A class that supplies the element given at construction (at first
     * invocation) and deep copies subsequently.</p>
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class DeclaratorSupplier implements Supplier<Optional<Declarator>> {
        private final Optional<Declarator> origSpecimen;
        private boolean origSupplied = false;

        private DeclaratorSupplier(Optional<Declarator> specimen) {
            checkNotNull(specimen, "the specimen declarator cannot be null");
            this.origSpecimen = specimen;
        }

        @Override
        public Optional<Declarator> get() {
            if (!origSpecimen.isPresent()) {
                return origSpecimen;
            }

            final Declarator result;

            if (!origSupplied) {
                result = origSpecimen.get();
                origSupplied = true;
            } else {
                result = origSpecimen.get().deepCopy(true);
            }

            result.setPastedFlagDeep(true);
            return Optional.of(result);
        }
    }

    /**
     * Builder for particular elements of the substitution.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private interface PrivateBuilder {
        void buildSubstitutions();
        ImmutableMap<String, AstType> getTypesSubstitution();
        ImmutableMap<String, Expression> getExpressionsSubstitution();
    }

    /**
     * Builder for the substitution.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static abstract class FromComponentRefBuilder {
        /**
         * Data needed to build a generic parameters substitution.
         */
        protected ComponentRef componentRef;
        protected Component genericComponent;

        /**
         * Private constructor to limit its accessibility.
         */
        private FromComponentRefBuilder() {
        }

        /**
         * <p>Set the component reference that contains the values for generic
         * parameters.</p>
         *
         * @param componentReference Value to set.
         * @return <code>this</code>
         */
        public FromComponentRefBuilder componentRef(ComponentRef componentReference) {
            this.componentRef = componentReference;
            return this;
        }

        /**
         * <p>Set the generic component that contain declarations of generic
         * parameters that will be substituted.</p>
         *
         * @param component Value to set.
         * @return <code>this</code>
         */
        public FromComponentRefBuilder genericComponent(Component component) {
            this.genericComponent = component;
            return this;
        }

        private void validate() {
            checkNotNull(componentRef, "component reference node cannot be null");
            checkNotNull(genericComponent, "generic component cannot be null");
            checkState(componentRef.getIsAbstract(), "component reference does not instantiate a new component");
            checkState(genericComponent.getIsAbstract(), "provided component is not generic");
            checkState(componentRef.getArguments() != null, "provided arguments are null");
            checkState(genericComponent.getParameters() != null, "list of declared generic parameters is null");
            checkState(genericComponent.getParameters().isPresent(), "the list of declared generic parameters is absent");
            checkState(componentRef.getArguments().size() == genericComponent.getParameters().get().size(),
                    "counts of the parameters in the component and the reference differ");
        }

        /**
         * Get the new instance of the substitution.
         *
         * @return Newly created instance of the class.
         */
        protected abstract GenericParametersSubstitution create();

        public final GenericParametersSubstitution build() {
            validate();
            return create();
        }
    }

    /**
     * Class that implements methods for creating the substitution from
     * component reference.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class FromComponentRefPrivateBuilder extends FromComponentRefBuilder implements PrivateBuilder {
        /**
         * Data for the building process.
         */
        private ImmutableMap.Builder<String, AstType> typesBuilder;
        private ImmutableMap.Builder<String, Expression> expressionsBuilder;

        @Override
        protected GenericParametersSubstitution create() {
            return new GenericParametersSubstitution(this);
        }

        @Override
        public void buildSubstitutions() {
            typesBuilder = ImmutableMap.builder();
            expressionsBuilder = ImmutableMap.builder();

            final Set<String> paramsNames = new HashSet<>();
            final Iterator<Declaration> declIt = genericComponent.getParameters().get().iterator();
            final Iterator<Expression> argIt = componentRef.getArguments().iterator();

            while (declIt.hasNext()) {
                final DataDecl decl = (DataDecl) declIt.next();
                final Expression arg = argIt.next();

                final VariableDecl variableDecl = (VariableDecl) decl.getDeclarations().getFirst();
                final String paramName = DeclaratorUtils.getDeclaratorName(variableDecl.getDeclarator().get()).get();

                if (!paramsNames.add(paramName)) {
                    throw new IllegalStateException("'" + paramName + "' has been already used as a parameter name.");
                }

                if (TypeElementUtils.isTypedef(decl.getModifiers())) {
                    checkState(arg instanceof TypeArgument, "invalid value for a generic type parameter");
                    final TypeArgument typeArg = (TypeArgument) arg;
                    checkState(typeArg.getAsttype() != null, "type for parameter '%s' is null", paramName);

                    typesBuilder.put(paramName, typeArg.getAsttype());
                } else {
                    checkState(!(arg instanceof TypeArgument), "type value provided for a non-type generic parameter");

                    arg.setPastedFlagDeep(true);
                    expressionsBuilder.put(paramName, arg);
                }
            }
        }

        @Override
        public ImmutableMap<String, AstType> getTypesSubstitution() {
            checkState(typesBuilder != null, "the substitution has not been built yet");
            return typesBuilder.build();
        }

        @Override
        public ImmutableMap<String, Expression> getExpressionsSubstitution() {
            checkState(expressionsBuilder != null, "the substitution has not been built yet");
            return expressionsBuilder.build();
        }
    }

    /**
     * Builder for a substitution of generic parameters in an interface.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static abstract class FromInterfaceRefBuilder {
        /**
         * Data needed to build a generic parameters substitution for a generic
         * interface.
         */
        protected InterfaceRef interfaceRef;
        protected Interface interfaceAst;

        /**
         * Private constructor to limit its accessibility.
         */
        private FromInterfaceRefBuilder() {
        }

        /**
         * Set the interface reference with type parameters to substitute.
         *
         * @param interfaceRef Interface reference to set.
         * @return <code>this</code>
         */
        public FromInterfaceRefBuilder interfaceRef(InterfaceRef interfaceRef) {
            this.interfaceRef = interfaceRef;
            return this;
        }

        /**
         * Set the interface AST node (with the definition of the interface).
         *
         * @param interfaceAst AST node to set.
         * @return <code>this</code>
         */
        public FromInterfaceRefBuilder interfaceAstNode(Interface interfaceAst) {
            this.interfaceAst = interfaceAst;
            return this;
        }

        private void validate() {
            checkNotNull(interfaceRef, "interface reference cannot be null");
            checkNotNull(interfaceAst, "interface AST node cannot be null");
            checkState(interfaceRef.getArguments().isPresent(), "interface reference contains no generic parameters to substitute");
            checkState(interfaceAst.getParameters().isPresent(), "interface that has been set is not generic");
            checkState(interfaceRef.getArguments().get().size() == interfaceAst.getParameters().get().size(),
                    "count of parameters provided in the interface reference and in the AST node is different");;
        }

        protected abstract GenericParametersSubstitution create();

        public final GenericParametersSubstitution build() {
            validate();
            return create();
        }
    }

    /**
     * Builder from interface reference with implementation of methods that
     * build particular elements of the substitution.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class FromInterfaceRefPrivateBuilder extends FromInterfaceRefBuilder implements PrivateBuilder {
        private ImmutableMap.Builder<String, AstType> typesSubstitutionBuilder;

        @Override
        protected GenericParametersSubstitution create() {
            return new GenericParametersSubstitution(this);
        }

        @Override
        public void buildSubstitutions() {
            typesSubstitutionBuilder = ImmutableMap.builder();

            final Iterator<Declaration> paramDeclIt = interfaceAst.getParameters().get().iterator();
            final Iterator<Expression> providedTypesIt = interfaceRef.getArguments().get().iterator();

            while (paramDeclIt.hasNext()) {
                final Declaration declaration = paramDeclIt.next();
                final Expression providedType = providedTypesIt.next();

                checkState(declaration instanceof TypeParmDecl, "declaration of the type parameter has unexpected class '%s'",
                        declaration.getClass());
                checkState(providedType instanceof TypeArgument, "type for generic interface reference has unexpected class '%s'",
                        providedType.getClass());

                final TypeParmDecl typeParmDecl = (TypeParmDecl) declaration;
                final TypeArgument typeArgument = (TypeArgument) providedType;

                typesSubstitutionBuilder.put(typeParmDecl.getName(), typeArgument.getAsttype());
            }
        }

        @Override
        public ImmutableMap<String, AstType> getTypesSubstitution() {
            return typesSubstitutionBuilder.build();
        }

        @Override
        public ImmutableMap<String, Expression> getExpressionsSubstitution() {
            return ImmutableMap.of();
        }
    }
}
