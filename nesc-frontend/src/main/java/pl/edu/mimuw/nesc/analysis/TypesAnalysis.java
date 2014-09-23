package pl.edu.mimuw.nesc.analysis;

import pl.edu.mimuw.nesc.ast.util.Interval;
import pl.edu.mimuw.nesc.ast.TagRefSemantics;
import pl.edu.mimuw.nesc.declaration.object.Linkage;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.object.TypenameDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.TagDeclaration;
import pl.edu.mimuw.nesc.ast.type.*;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.problem.issue.*;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.EnumMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static pl.edu.mimuw.nesc.analysis.TagsAnalysis.*;
import static pl.edu.mimuw.nesc.astbuilding.DeclaratorUtils.getDeclaratorName;
import static pl.edu.mimuw.nesc.problem.issue.InvalidTypeSpecifiersMixError.InvalidCombinationType.*;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * Class with methods that perform various kinds of types analysis.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class TypesAnalysis {
    /**
     * Checks the type of an identifier in a standard declaration
     * <code>type</code> and <code>linkage</code> parameters are of
     * <code>Optional</code> class only for convenience - if any of these values
     * is absent, this method does nothing.
     *
     * @param type Type of the identifier to check.
     * @param linkage Linkage of the identifier to check.
     * @param identifier String with the identifier that will be checked.
     * @param interval Position of the identifier that will be checked.
     * @param errorHelper Object that will be notified about detected errors.
     * @throws NullPointerException One of the arguments is null.
     */
    public static void checkVariableType(Optional<Type> type, Optional<Linkage> linkage,
            Optional<String> identifier, Interval interval, ErrorHelper errorHelper) {
        // Check arguments
        checkNotNull(type, "the type cannot be null");
        checkNotNull(linkage, "the linkage cannot be null");
        checkNotNull(identifier, "the identifier cannot be null");
        checkNotNull(interval, "the interval cannot be null");
        checkNotNull(errorHelper, "the error helper cannot be null");

        // Return if there is not enough data
        if (!type.isPresent() || !linkage.isPresent()) {
            return;
        }

        // Prepare
        final Type unwrappedType = type.get();
        final Linkage unwrappedLinkage = linkage.get();
        if (unwrappedLinkage != Linkage.NONE || !unwrappedType.isObjectType()) {
            return;
        }

        // Check type
        if (!unwrappedType.isComplete()) {
            errorHelper.error(interval.getLocation(), interval.getEndLocation(),
                    new IncompleteVariableTypeError(identifier, unwrappedType));
        }
    }

    /**
     * Checks the correctness of types used as function parameters and reports
     * all detected errors to the given error helper. This method should only be
     * invoked to check the parameters of a function <u>definition</u>.
     *
     * @param parametersTypes List with types of function parameters (in proper
     *                        order).
     * @param parametersDeclarations List with declarations of the parameters
     *                               (in proper order) used for retrieving
     *                               locations.
     * @param errorHelper Object that will be notified about detected errors.
     * @throws NullPointerException A value of a parameter is null (except
     *                              <code>isDefinition</code>).
     * @throws IllegalArgumentException Size of the declarations list is lesser
     *                                  than the size of types list.
     */
    public static void checkFunctionParametersTypes(List<Optional<Type>> parametersTypes,
            List<Declaration> parametersDeclarations, ErrorHelper errorHelper) {
        // Validate arguments
        checkNotNull(parametersTypes, "parameters types cannot be null");
        checkNotNull(parametersDeclarations, "declarations of parameters cannot be null");
        checkNotNull(errorHelper, "the error helper cannot be null");
        checkArgument(parametersTypes.size() <= parametersDeclarations.size(),
                "the size of the parameters declarations list is lesser than the size of list of types");

        // Check the types
        final Iterator<Optional<Type>> typesIt = parametersTypes.iterator();
        final Iterator<Declaration> declIt = parametersDeclarations.iterator();
        while (typesIt.hasNext()) {
            final Optional<Type> maybeParamType = typesIt.next();
            final Declaration declaration = declIt.next();
            if (!maybeParamType.isPresent()) {
                continue;
            }

            checkFunctionParameterType(maybeParamType.get(), declaration, errorHelper);
        }
    }

    private static void checkFunctionParameterType(Type paramType, Declaration declaration,
                                                   ErrorHelper errorHelper) {
        final Type decayedType = paramType.decay();
        if (!decayedType.isComplete()) {
            final Optional<String> maybeName = getParameterName(declaration);
            errorHelper.error(declaration.getLocation(), declaration.getEndLocation(),
                              new IncompleteParameterTypeError(maybeName, paramType));
        }
    }

    private static Optional<String> getParameterName(Declaration declaration) {
        if (!(declaration instanceof DataDecl)) {
            return Optional.absent();
        }

        final DataDecl decl = (DataDecl) declaration;
        if (decl.getDeclarations().isEmpty()) {
            return Optional.absent();
        }

        final Declaration firstDecl = decl.getDeclarations().getFirst();
        if (!(firstDecl instanceof VariableDecl)) {
            return Optional.absent();
        }

        final VariableDecl varDecl = (VariableDecl) firstDecl;
        if (!varDecl.getDeclarator().isPresent()) {
            return Optional.absent();
        }

        return Optional.fromNullable(getDeclaratorName(varDecl.getDeclarator().get()));
    }

    /**
     * Shortcut that combines functionality of <code>resolveBaseType</code>
     * and <code>resolveDeclaratorType</code>. It should be used if there is
     * only one declarator for some type elements and both can be easily
     * accessed. Parameters have meaning that is identical to this of the
     * methods mentioned above.
     */
    public static Optional<Type> resolveType(Environment environment,
            List<TypeElement> typeElements, Optional<Declarator> declarator,
            ErrorHelper errorHelper, Location apxStartLoc, Location apxEndLoc) {

        final Optional<Type> maybeBaseType = resolveBaseType(environment,
                typeElements, false, errorHelper, apxStartLoc, apxEndLoc);

        return   maybeBaseType.isPresent() && declarator.isPresent()
               ? resolveDeclaratorType(declarator.get(), errorHelper, maybeBaseType.get())
               : maybeBaseType;
    }

    /**
     * Resolves the type of the identifier from the given declarator.
     *
     * @param declarator Declarator to extract the type from.
     * @param errorHelper Object that will be notified about detected errors
     *                    and warnings.
     * @param baseType Type extracted from the declaration specifiers.
     * @return Type of the identifier from the given declarator wrapped by
     *         <code>Optional</code> if no error is detected or an absent
     *         object, otherwise.
     * @throws NullPointerException One of the arguments is null.
     */
    public static Optional<Type> resolveDeclaratorType(Declarator declarator,
                ErrorHelper errorHelper, Type baseType) {
        // Validate arguments
        checkNotNull(declarator, "declarator cannot be null");
        checkNotNull(errorHelper, "error helper cannot be null");
        checkNotNull(baseType, "base type cannot be null");

        // Resolve the type
        final DeclaratorTypeVisitor visitor = new DeclaratorTypeVisitor(baseType, errorHelper);
        declarator.accept(visitor, null);
        return visitor.get();
    }

    /**
     * Resolves the type specified by given type elements. Potential tag
     * declarations or definitions in given type elements are also processed
     * and the symbol table for tags is properly updated if necessary.
     *
     * @param environment  Environment from the place of the processed
     *                     declaration.
     * @param typeElements Declaration specifiers that contain information about
     *                     type that is not derived.
     * @param isStandalone <code>true</code> if and only if the given type
     *                     elements are not associated with any object.
     * @param errorHelper Object that will be notified about detected errors or
     *                    warnings.
     * @param apxStartLoc Approximate start location to point to when no type
     *                    specifiers are given in the type elements.
     * @param apxEndLoc Approximate end location to point to when no type
     *                  specifiers are given in the type elements.
     * @return Type wrapped by Optional if the type is resolved successfully or
     *         nothing, otherwise.
     * @throws NullPointerException One of the arguments is null.
     */
    public static Optional<Type> resolveBaseType(Environment environment,
            List<TypeElement> typeElements, boolean isStandalone,
            ErrorHelper errorHelper, Location apxStartLoc, Location apxEndLoc) {
        // Validate arguments
        checkNotNull(environment, "environment cannot be null");
        checkNotNull(typeElements, "list of type elements cannot be null");
        checkNotNull(errorHelper, "error helper cannot be null");
        checkNotNull(apxStartLoc, "approximate start location cannot be null");
        checkNotNull(apxEndLoc, "approximate end location cannot be null");

        // Resolve the type
        final BaseTypeVisitor visitor = new BaseTypeVisitor(environment,
                errorHelper, isStandalone, apxStartLoc, apxEndLoc);
        for (TypeElement typeElement : typeElements) {
            typeElement.accept(visitor, null);
        }
        return visitor.finish();
    }

    /**
     * Class for accumulating the type information. It contains method for every
     * class derived from <code>TypeElement</code> that is used.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class BaseTypeVisitor extends ExceptionVisitor<Void, Void> {
        /**
         * Type specifiers that build each fundamental type.
         */
        private static final List<ImmutableMultiset<RID>> SPECIFIERS_VOID = Collections.unmodifiableList(Arrays.asList(
            ImmutableMultiset.of(RID.VOID)
        ));
        private static final List<ImmutableMultiset<RID>> SPECIFIERS_CHAR = Collections.unmodifiableList(Arrays.asList(
            ImmutableMultiset.of(RID.CHAR)
        ));
        private static final List<ImmutableMultiset<RID>> SPECIFIERS_SIGNED_CHAR = Collections.unmodifiableList(Arrays.asList(
            ImmutableMultiset.of(RID.SIGNED, RID.CHAR)
        ));
        private static final List<ImmutableMultiset<RID>> SPECIFIERS_UNSIGNED_CHAR = Collections.unmodifiableList(Arrays.asList(
            ImmutableMultiset.of(RID.UNSIGNED, RID.CHAR)
        ));
        private static final List<ImmutableMultiset<RID>> SPECIFIERS_SHORT = Collections.unmodifiableList(Arrays.asList(
            ImmutableMultiset.of(RID.SHORT),
            ImmutableMultiset.of(RID.SIGNED, RID.SHORT),
            ImmutableMultiset.of(RID.SHORT, RID.INT),
            ImmutableMultiset.of(RID.SIGNED, RID.SHORT, RID.INT)
        ));
        private static final List<ImmutableMultiset<RID>> SPECIFIERS_UNSIGNED_SHORT = Collections.unmodifiableList(Arrays.asList(
            ImmutableMultiset.of(RID.UNSIGNED, RID.SHORT),
            ImmutableMultiset.of(RID.UNSIGNED, RID.SHORT, RID.INT)
        ));
        private static final List<ImmutableMultiset<RID>> SPECIFIERS_INT = Collections.unmodifiableList(Arrays.asList(
            ImmutableMultiset.of(RID.INT),
            ImmutableMultiset.of(RID.SIGNED),
            ImmutableMultiset.of(RID.SIGNED, RID.INT)
        ));
        private static final List<ImmutableMultiset<RID>> SPECIFIERS_UNSIGNED_INT = Collections.unmodifiableList(Arrays.asList(
            ImmutableMultiset.of(RID.UNSIGNED),
            ImmutableMultiset.of(RID.UNSIGNED, RID.INT)
        ));
        private static final List<ImmutableMultiset<RID>> SPECIFIERS_LONG = Collections.unmodifiableList(Arrays.asList(
            ImmutableMultiset.of(RID.LONG),
            ImmutableMultiset.of(RID.SIGNED, RID.LONG),
            ImmutableMultiset.of(RID.LONG, RID.INT),
            ImmutableMultiset.of(RID.SIGNED, RID.LONG, RID.INT)
        ));
        private static final List<ImmutableMultiset<RID>> SPECIFIERS_UNSIGNED_LONG = Collections.unmodifiableList(Arrays.asList(
            ImmutableMultiset.of(RID.UNSIGNED, RID.LONG),
            ImmutableMultiset.of(RID.UNSIGNED, RID.LONG, RID.INT)
        ));
        private static final List<ImmutableMultiset<RID>> SPECIFIERS_LONG_LONG = Collections.unmodifiableList(Arrays.asList(
            ImmutableMultiset.of(RID.LONG, RID.LONG),
            ImmutableMultiset.of(RID.SIGNED, RID.LONG, RID.LONG),
            ImmutableMultiset.of(RID.LONG, RID.LONG, RID.INT),
            ImmutableMultiset.of(RID.SIGNED, RID.LONG, RID.LONG, RID.INT)
        ));
        private static final List<ImmutableMultiset<RID>> SPECIFIERS_UNSIGNED_LONG_LONG = Collections.unmodifiableList(Arrays.asList(
            ImmutableMultiset.of(RID.UNSIGNED, RID.LONG, RID.LONG),
            ImmutableMultiset.of(RID.UNSIGNED, RID.LONG, RID.LONG, RID.INT)
        ));
        private static final List<ImmutableMultiset<RID>> SPECIFIERS_FLOAT = Collections.unmodifiableList(Arrays.asList(
            ImmutableMultiset.of(RID.FLOAT)
        ));
        private static final List<ImmutableMultiset<RID>> SPECIFIERS_DOUBLE = Collections.unmodifiableList(Arrays.asList(
            ImmutableMultiset.of(RID.DOUBLE)
        ));
        private static final List<ImmutableMultiset<RID>> SPECIFIERS_LONG_DOUBLE = Collections.unmodifiableList(Arrays.asList(
            ImmutableMultiset.of(RID.LONG, RID.DOUBLE)
        ));

        /**
         * Set with all specifiers that affect the type.
         */
        private static final Set<RID> TYPE_SPECIFIERS = Collections.unmodifiableSet(EnumSet.of(
            RID.VOID,
            RID.CHAR,
            RID.SHORT,
            RID.INT,
            RID.LONG,
            RID.FLOAT,
            RID.DOUBLE,
            RID.SIGNED,
            RID.UNSIGNED
        ));

        /**
         * Maximum number of occurrences of <code>long</code> type specifier in
         * a declaration.
         */
        private static final int MAX_LONG_COUNT = 2;

        /**
         * Environment of the processed declaration.
         */
        private final Environment environment;

        /**
         * Object that will be notified about detected errors and warnings.
         */
        private final ErrorHelper errorHelper;

        /**
         * <code>true</code> if and only if the type elements processed by this
         * visitor are in a standalone declaration (that does not declare any
         * objects).
         */
        private final boolean isStandalone;

        /**
         * <code>true</code> if and only if an error has been detected during
         * the processing that makes it impossible to resolve the type.
         */
        private boolean typeError = false;

        /**
         * Locations for some error messages.
         */
        private Location startTypeSpecifiers = null;
        private Location endTypeSpecifiers = null;
        private final Location startFuzzy;
        private final Location endFuzzy;

        /**
         * Variables that accumulate the type information.
         */
        private boolean isConstQualified = false;
        private boolean isVolatileQualified = false;
        private Optional<Interval> restrictQualifier = Optional.absent();
        private final Multiset<RID> typeSpecifiers = EnumMultiset.create(RID.class);
        private TagRef tagReference;
        private Typename typename;
        private AttributeRef attributeDefinition;

        private BaseTypeVisitor(Environment environment, ErrorHelper errorHelper,
                                boolean isStandalone, Location startFuzzy, Location endFuzzy) {
            this.environment = environment;
            this.errorHelper = errorHelper;
            this.isStandalone = isStandalone;
            this.startFuzzy = startFuzzy;
            this.endFuzzy = endFuzzy;
        }

        /**
         * @return Object that represents the determined type if it is valid
         *         wrapped by <code>Optional</code>. Otherwise, the object is
         *         not present.
         */
        private Optional<Type> finish() {
            if (typeError) {
                return Optional.absent();
            }

            if (!typenameAccepted()) {
                emitRestrictWarning();
            }

            if (attributeAccepted()) {
                return Optional.absent();
            } else if (simpleAccepted()) {
                return finishFundamentalType();
            } else if (tagAccepted()) {
                return finishTagType();
            } else if (typenameAccepted()) {
                return finishTypename();
            } else {
                typeError = true;
                errorHelper.error(startFuzzy, endFuzzy, new NoTypeSpecifiersError());
                return Optional.absent();
            }
        }

        private Optional<Type> finishFundamentalType() {
            Type result = null;

            if (compareTypeSpecifiers(SPECIFIERS_VOID)) {
                result = new VoidType(isConstQualified, isVolatileQualified);
            } else if (compareTypeSpecifiers(SPECIFIERS_CHAR)) {
                result = new CharType(isConstQualified, isVolatileQualified);
            } else if (compareTypeSpecifiers(SPECIFIERS_SIGNED_CHAR)) {
                result = new SignedCharType(isConstQualified, isVolatileQualified);
            } else if (compareTypeSpecifiers(SPECIFIERS_UNSIGNED_CHAR)) {
                result = new UnsignedCharType(isConstQualified, isVolatileQualified);
            } else if (compareTypeSpecifiers(SPECIFIERS_SHORT)) {
                result = new ShortType(isConstQualified, isVolatileQualified);
            } else if (compareTypeSpecifiers(SPECIFIERS_UNSIGNED_SHORT)) {
                result = new UnsignedShortType(isConstQualified, isVolatileQualified);
            } else if (compareTypeSpecifiers(SPECIFIERS_INT)) {
                result = new IntType(isConstQualified, isVolatileQualified);
            } else if (compareTypeSpecifiers(SPECIFIERS_UNSIGNED_INT)) {
                result = new UnsignedIntType(isConstQualified, isVolatileQualified);
            } else if (compareTypeSpecifiers(SPECIFIERS_LONG)) {
                result = new LongType(isConstQualified, isVolatileQualified);
            } else if (compareTypeSpecifiers(SPECIFIERS_UNSIGNED_LONG)) {
                result = new UnsignedLongType(isConstQualified, isVolatileQualified);
            } else if (compareTypeSpecifiers(SPECIFIERS_LONG_LONG)) {
                result = new LongLongType(isConstQualified, isVolatileQualified);
            } else if (compareTypeSpecifiers(SPECIFIERS_UNSIGNED_LONG_LONG)) {
                result = new UnsignedLongLongType(isConstQualified, isVolatileQualified);
            } else if (compareTypeSpecifiers(SPECIFIERS_FLOAT)) {
                result = new FloatType(isConstQualified, isVolatileQualified);
            } else if (compareTypeSpecifiers(SPECIFIERS_DOUBLE)) {
                result = new DoubleType(isConstQualified, isVolatileQualified);
            } else if (compareTypeSpecifiers(SPECIFIERS_LONG_DOUBLE)) {
                result = new LongDoubleType(isConstQualified, isVolatileQualified);
            } else {
                typeError = true;
                errorHelper.error(
                        startTypeSpecifiers,
                        endTypeSpecifiers,
                        new InvalidTypeSpecifiersMixError(SIMPLE_WITH_SIMPLE, Optional.<String>absent())
                );
            }

            return Optional.fromNullable(result);
        }

        private Optional<Type> finishTagType() {
            final Optional<Word> maybeName = Optional.fromNullable(tagReference.getName());
            final Optional<TagDeclaration> maybeTagDeclaration =
                      maybeName.isPresent()
                   ? finishNamedTagType(maybeName.get().getName())
                   : finishUnnamedTagType();
            final Function<TagDeclaration, Type> transformFun = new Function<TagDeclaration, Type>() {
                @Override
                public Type apply(TagDeclaration tagDeclaration) {
                    return tagDeclaration.getType(isConstQualified, isVolatileQualified);
                }
            };

            return maybeTagDeclaration.transform(transformFun);
        }

        private Optional<TagDeclaration> finishNamedTagType(String name) {
            if (tagReference.getIsInvalid()) {
                typeError = true;
                return Optional.absent();
            }

            final Optional<? extends TagDeclaration> maybeTagDeclaration = environment.getTags().get(name);
            if (!maybeTagDeclaration.isPresent()) {
                typeError = true;
                return Optional.absent();
            }

            final TagDeclaration tagDeclaration = maybeTagDeclaration.get();
            return Optional.of(tagDeclaration);
        }

        private Optional<TagDeclaration> finishUnnamedTagType() {
            TagDeclaration tagDeclaration = null;

            if (tagReference instanceof StructRef) {
                tagDeclaration = makeStructDeclaration(errorHelper, (StructRef) tagReference);
            } else if (tagReference instanceof UnionRef) {
                tagDeclaration = makeUnionDeclaration(errorHelper, (UnionRef) tagReference);
            } else if (tagReference instanceof EnumRef) {
                tagDeclaration = makeEnumDeclaration((EnumRef) tagReference);
            } else {
                assert false : "unexpected tag reference class '"
                        + tagReference.getClass() + "'";
            }

            return Optional.of(tagDeclaration);
        }

        private Optional<Type> finishTypename() {
            if (typename instanceof ComponentTyperef) {
                // TODO handle typedefs from other components
                return Optional.absent();
            }

            // Resolve the typename
            final String typenameStr = typename.getName();
            Optional<? extends ErroneousIssue> error;
            final Optional<? extends ObjectDeclaration> maybeDecl = environment.getObjects().get(typenameStr);
            if (!maybeDecl.isPresent()) {
                error = Optional.of(new UndeclaredIdentifierError(typenameStr));
            } else if (!maybeDecl.get().getType().isPresent()) {
                return Optional.absent();
            } else {
                final Type type = maybeDecl.get().getType().get();
                if (!type.isTypeDefinition()) {
                    error = Optional.of(new InvalidIdentifierTypeError(typenameStr, type,
                                        TypeDefinitionType.getInstance()));
                } else {
                    final TypenameDeclaration declaration = (TypenameDeclaration) maybeDecl.get();
                    final Optional<Type> denotedType = declaration.getDenotedType();
                    final Function<Type, Type> transformFun = new Function<Type, Type>() {
                        @Override
                        public final Type apply(Type type) {
                            return type.addQualifiers(isConstQualified, isVolatileQualified,
                                                      restrictQualifier.isPresent());
                        }
                    };

                    if (denotedType.isPresent() && !denotedType.get().isPointerType()) {
                        emitRestrictWarning();
                    }

                    return declaration.getDenotedType().transform(transformFun);
                }
            }

            errorHelper.error(typename.getLocation(), typename.getEndLocation(), error.get());
            return Optional.absent();
        }

        @Override
        public Void visitRid(Rid rid, Void v) {
            acceptSimple(rid.getId(), rid.getLocation(), rid.getEndLocation());
            return null;
        }

        @Override
        public Void visitQualifier(Qualifier qualifier, Void v) {
            acceptSimple(qualifier.getId(), qualifier.getLocation(), qualifier.getEndLocation());
            return null;
        }

        @Override
        public Void visitEnumRef(EnumRef enumRef, Void v) {
            acceptTag(enumRef);
            return null;
        }

        @Override
        public Void visitStructRef(StructRef structRef, Void v) {
            acceptTag(structRef);
            return null;
        }

        @Override
        public Void visitUnionRef(UnionRef unionRef, Void v) {
            acceptTag(unionRef);
            return null;
        }

        @Override
        public Void visitNxStructRef(NxStructRef nxStructRef, Void v) {
            acceptTag(nxStructRef);
            return null;
        }

        @Override
        public Void visitNxUnionRef(NxUnionRef nxUnionRef, Void v) {
            acceptTag(nxUnionRef);
            return null;
        }

        @Override
        public Void visitTypename(Typename typename, Void v) {
            acceptTypename(typename);
            return null;
        }

        @Override
        public Void visitComponentTyperef(ComponentTyperef typeref, Void v) {
            acceptTypename(typeref);
            return null;
        }

        @Override
        public Void visitAttributeRef(AttributeRef attrRef, Void v) {
            acceptAttribute(attrRef);
            return null;
        }

        @Override
        public Void visitTypeofType(TypeofType type, Void v) {
            return null;
        }

        @Override
        public Void visitTypeofExpr(TypeofExpr type, Void v) {
            return null;
        }

        @Override
        public Void visitAttribute(Attribute attr, Void v) {
            return null;
        }

        @Override
        public Void visitGccAttribute(GccAttribute attr, Void v) {
            return null;
        }

        @Override
        public Void visitTargetAttribute(TargetAttribute attr, Void v) {
            return null;
        }

        @Override
        public Void visitNescAttribute(NescAttribute attr, Void v) {
            return null;
        }

        private void acceptSimple(RID rid, Location startLoc, Location endLoc) {
            boolean typeQualifierRepeated = false;
            Optional<? extends ErroneousIssue> error = Optional.absent();

            switch (rid) {
                case CONST:
                    typeQualifierRepeated = isConstQualified;
                    isConstQualified = true;
                    break;
                case VOLATILE:
                    typeQualifierRepeated = isVolatileQualified;
                    isVolatileQualified = true;
                    break;
                case RESTRICT:
                    typeQualifierRepeated = restrictQualifier.isPresent();
                    if (!restrictQualifier.isPresent()) {
                        restrictQualifier = Optional.of(Interval.of(startLoc, endLoc));
                    }
                    break;
                default:
                    if (!TYPE_SPECIFIERS.contains(rid)) {
                        return;
                    }
                    updateLocations(startLoc, endLoc);
                    if (attributeAccepted()) {
                        error = Optional.of(new InvalidTypeSpecifiersMixError(SIMPLE_WITH_ATTRIBUTE,
                                            Optional.of(rid.getName())));
                    } else if (tagAccepted()) {
                        error = Optional.of(new InvalidTypeSpecifiersMixError(SIMPLE_WITH_TAG,
                                            Optional.of(rid.getName())));
                    } else if (typenameAccepted()) {
                        error = Optional.of(new InvalidTypeSpecifiersMixError(SIMPLE_WITH_TYPENAME,
                                            Optional.of(rid.getName())));
                    } else if (rid != RID.LONG && typeSpecifiers.contains(rid)) {
                        error = Optional.of(TypeSpecifierRepetitionError.ridRepetition(rid, 1));
                    } else if (rid == RID.LONG && typeSpecifiers.count(rid) >= MAX_LONG_COUNT) {
                        error = Optional.of(TypeSpecifierRepetitionError.ridRepetition(rid, MAX_LONG_COUNT));
                    } else {
                        typeSpecifiers.add(rid);
                    }
                    break;
            }

            if (error.isPresent()) {
                typeError = true;
                errorHelper.error(startLoc, endLoc, error.get());
            }
            if (typeQualifierRepeated) {
                errorHelper.warning(startLoc, endLoc, new TypeQualifierRepetitionWarning(rid));
            }
        }

        private void acceptTag(TagRef tagRef) {
            updateLocations(tagRef.getLocation(), tagRef.getEndLocation());
            Optional<? extends ErroneousIssue> error = Optional.absent();

            if (attributeAccepted()) {
                error = Optional.of(new InvalidTypeSpecifiersMixError(TAG_WITH_ATTRIBUTE, Optional.<String>absent()));
            } else if (tagAccepted()) {
                error = Optional.of(TypeSpecifierRepetitionError.tagRefRepetition(1));
            } else if (simpleAccepted() || typenameAccepted()) {
                error = Optional.of(new InvalidTypeSpecifiersMixError(TAG_WITH_OTHER, Optional.<String>absent()));
            }

            if (error.isPresent()) {
                typeError = true;
                errorHelper.error(tagRef.getLocation(), tagRef.getEndLocation(), error.get());
                return;
            }

            if (tagRef.getSemantics() == TagRefSemantics.OTHER) {
                processTagReference(tagRef, environment, isStandalone, errorHelper);
            }
            tagReference = tagRef;
        }

        private void acceptTypename(Typename typename) {
            updateLocations(typename.getLocation(), typename.getEndLocation());
            Optional<? extends ErroneousIssue> error = Optional.absent();

            if (attributeAccepted()) {
                error = Optional.of(new InvalidTypeSpecifiersMixError(TYPENAME_WITH_ATTRIBUTE,
                                    Optional.of(typename.getName())));
            } else if (tagAccepted() || simpleAccepted()) {
                error = Optional.of(new InvalidTypeSpecifiersMixError(TYPENAME_WITH_OTHER,
                                    Optional.of(typename.getName())));
            }

            if (error.isPresent()) {
                typeError = true;
                errorHelper.error(typename.getLocation(), typename.getEndLocation(), error.get());
                return;
            }

            this.typename = typename;
        }

        private void acceptAttribute(AttributeRef attributeDef) {
            updateLocations(attributeDef.getLocation(), attributeDef.getEndLocation());
            Optional<? extends ErroneousIssue> error = Optional.absent();

            if (attributeAccepted()) {
                error = Optional.of(new InvalidTypeSpecifiersMixError(ATTRIBUTE_WITH_ATTRIBUTE,
                                    Optional.<String>absent()));
            } else if (tagAccepted() || typenameAccepted() || simpleAccepted()) {
                error = Optional.of(new InvalidTypeSpecifiersMixError(ATTRIBUTE_WITH_OTHER,
                                    Optional.<String>absent()));
            }

            if (!error.isPresent()) {
                this.attributeDefinition = attributeDef;

                if (!isStandalone) {
                    error = Optional.of(new AttributeUsageAsTypeError());
                }
            }

            if (error.isPresent()) {
                typeError = true;
                errorHelper.error(attributeDef.getLocation(), attributeDef.getEndLocation(), error.get());
            }
        }

        /**
         * A simple type specifier is a RID type specifier, e.g.
         * <code>int</code> or <code>long</code>.
         *
         * @return <code>true</code> if and only if a simple type specifier has
         *         been accepted as a type specifier by this object.
         */
        private boolean simpleAccepted() {
            return !typeSpecifiers.isEmpty();
        }

        /**
         * @return <code>true</code> if and only if a tag has been accepted as
         *         a type specifier by this object.
         */
        private boolean tagAccepted() {
            return tagReference != null;
        }

        /**
         * @return <code>true</code> if and only if a typename has been accepted
         *         as a type specifier by this object.
         */
        private boolean typenameAccepted() {
            return typename != null;
        }

        /**
         * @return <code>true</code> if and only if an attribute definition has
         *         been accepted as the specifier by this object.
         */
        private boolean attributeAccepted() {
            return attributeDefinition != null;
        }

        /**
         * @return <code>true</code> if and only if the multiset of type
         *         specifiers collected by this visitor is equal to one of the
         *         multisets from given list.
         */
        private boolean compareTypeSpecifiers(List<ImmutableMultiset<RID>> specsList) {
            for (ImmutableMultiset<RID> specs : specsList) {
                if (specs.equals(typeSpecifiers)) {
                    return true;
                }
            }

            return false;
        }

        private void updateLocations(Location startLoc, Location endLoc) {
            if (startTypeSpecifiers == null && startLoc != null) {
                startTypeSpecifiers = startLoc;
            }

            if (endLoc != null) {
                endTypeSpecifiers = endLoc;
            }
        }

        private void emitRestrictWarning() {
            if (restrictQualifier.isPresent()) {
                errorHelper.warning(
                    restrictQualifier.get().getLocation(),
                    restrictQualifier.get().getEndLocation(),
                    new InvalidRestrictUsageWarning()
                );
            }
        }
    }

    /**
     * Extracts the type from a declarator. This visitor visits exactly the
     * objects of classes derived from <code>Declarator</code>.
     * <code>GenericDeclarator</code> and <code>NestedDeclarator</code> are
     * intentionally omitted because no instance of these classes is created.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class DeclaratorTypeVisitor extends ExceptionVisitor<Void, Void> {
        /**
         * Object that will be notified about detected errors.
         */
        private final ErrorHelper errorHelper;

        /**
         * Object that will check created types for validity.
         */
        private final TypeValidityVisitor validityVisitor;

        /**
         * <code>true</code> if and only if the declarators are not valid and
         * thus the type cannot be determined.
         */
        private boolean typeError = false;

        /**
         * Type that will be the argument for the next declarator or the final
         * type if there are no further declarators.
         */
        private Type accumulatedType;

        private DeclaratorTypeVisitor(Type baseType, ErrorHelper errorHelper) {
            this.accumulatedType = baseType;
            this.errorHelper = errorHelper;
            this.validityVisitor = new TypeValidityVisitor(errorHelper);
        }

        /**
         * @return Type of the identifier in the visited declarator wrapped by
         *         <code>Optional</code> if it is correct or no object,
         *         otherwise.
         */
        private Optional<Type> get() {
            return   typeError
                   ? Optional.<Type>absent()
                   : Optional.of(accumulatedType);
        }

        @Override
        public Void visitIdentifierDeclarator(IdentifierDeclarator declarator, Void v) {
            return null;
        }

        @Override
        public Void visitInterfaceRefDeclarator(InterfaceRefDeclarator declarator, Void v) {
            jump(declarator.getDeclarator());
            return null;
        }

        @Override
        public Void visitArrayDeclarator(ArrayDeclarator declarator, Void v) {
            accumulatedType = new ArrayType(accumulatedType, declarator.getSize().isPresent());
            checkType(declarator);
            jump(declarator.getDeclarator());
            return null;
        }

        @Override
        public Void visitPointerDeclarator(PointerDeclarator declarator, Void v) {
            final PointerTypeQualifiers qualifiers = processPointerQualifiers(declarator);
            accumulatedType = new PointerType(qualifiers.constQualified,
                    qualifiers.volatileQualified, qualifiers.restrictQualified,
                    accumulatedType);
            checkType(declarator);
            jump(qualifiers.nextDeclarator);
            return null;
        }

        @Override
        public Void visitFunctionDeclarator(FunctionDeclarator declarator, Void v) {
            final ParametersTypesVisitor paramsVisitor = new ParametersTypesVisitor(
                    declarator.getEnvironment(),
                    errorHelper
            );

            if (hasParameters(declarator)) {
                for (Declaration paramDecl : declarator.getParameters()) {
                    paramDecl.accept(paramsVisitor, null);
                }
            }

            typeError = typeError || paramsVisitor.typeError;
            accumulatedType = new FunctionType(accumulatedType, paramsVisitor.types,
                    paramsVisitor.variableArguments);
            checkType(declarator);
            jump(declarator.getDeclarator());
            return null;
        }

        /**
         * @return <code>true</code> if and only if the function indicated by
         *         given declarator takes some arguments. The special case of the
         *         <code>void</code> keyword is included.
         */
        private static boolean hasParameters(FunctionDeclarator declarator) {
            // Check the usual case
            final LinkedList<Declaration> params = declarator.getParameters();
            if (params.isEmpty()) {
                return false;
            }
            if (params.size() != 1) {
                return true;
            }

            // Check the case of 'void' keyword
            final Declaration onlyParam = params.getFirst();
            if (!(onlyParam instanceof DataDecl)) {
                return true;
            }
            final DataDecl dataDecl = (DataDecl) onlyParam;
            final LinkedList<Declaration> paramDecls = dataDecl.getDeclarations();
            assert paramDecls.size() == 1 : "a parameter with multiple declarations";
            final Declaration onlyParamDecl = paramDecls.getFirst();
            assert onlyParamDecl instanceof VariableDecl :
                    format("unexpected variable parameter declaration of class '%s'",
                            onlyParamDecl.getClass().getCanonicalName());
            final VariableDecl paramDecl = (VariableDecl) onlyParamDecl;

            if (paramDecl.getDeclarator().isPresent() || dataDecl.getModifiers().size() != 1) {
                return true;
            }

            final TypeElement onlyModifier = dataDecl.getModifiers().getFirst();
            RID modifier;
            if (onlyModifier instanceof Rid) {
                modifier = ((Rid) onlyModifier).getId();
            } else if (onlyModifier instanceof Qualifier) {
                modifier = ((Qualifier) onlyModifier).getId();
            } else {
                return true;
            }
            return modifier != RID.VOID;
        }

        @Override
        public Void visitQualifiedDeclarator(QualifiedDeclarator declarator, Void arg) {
            final LinkedList<TypeElement> specifiers = declarator.getModifiers();
            if (!specifiers.isEmpty()) {
                errorHelper.warning(
                        specifiers.getFirst().getLocation(),
                        specifiers.getLast().getEndLocation(),
                        new SuperfluousSpecifiersWarning()
                );
            }

            jump(declarator.getDeclarator());
            return null;
        }

        private void jump(Optional<Declarator> next) {
            if (next.isPresent()) {
                next.get().accept(this, null);
            }
        }

        private PointerTypeQualifiers processPointerQualifiers(PointerDeclarator startDeclarator) {
            boolean isConstQualified = false,
                    isVolatileQualified = false,
                    isRestrictQualified = false;
            Optional<Declarator> nextDeclarator = startDeclarator.getDeclarator();

            while (nextDeclarator.isPresent() && nextDeclarator.get() instanceof QualifiedDeclarator) {
                final QualifiedDeclarator qualified = (QualifiedDeclarator) nextDeclarator.get();

                for (TypeElement typeElement : qualified.getModifiers()) {
                    if (typeElement instanceof Attribute) {
                        continue;
                    } else if (!(typeElement instanceof Qualifier)) {
                        throw new RuntimeException(format("unexpected type element of class '%s' in a pointer declarator",
                                typeElement.getClass().getCanonicalName()));
                    }
                    final Qualifier qualifier = (Qualifier) typeElement;

                    boolean repetition;
                    switch (qualifier.getId()) {
                        case CONST:
                            repetition = isConstQualified;
                            isConstQualified = true;
                            break;
                        case VOLATILE:
                            repetition = isVolatileQualified;
                            isVolatileQualified = true;
                            break;
                        case RESTRICT:
                            repetition = isRestrictQualified;
                            isRestrictQualified = true;
                            break;
                        default:
                            throw new RuntimeException(format("unexpected specifier '%s' in a pointer declarator",
                                                       qualifier.getId().getName()));
                    }

                    if (repetition) {
                        errorHelper.warning(qualifier.getLocation(), qualifier.getEndLocation(),
                                new TypeQualifierRepetitionWarning(qualifier.getId()));
                    }
                }

                nextDeclarator = qualified.getDeclarator();
            }

            return new PointerTypeQualifiers(isConstQualified, isVolatileQualified,
                    isRestrictQualified, nextDeclarator);
        }

        private void checkType(Declarator declarator) {
            accumulatedType.accept(validityVisitor, Interval.of(declarator.getLocation(),
                    declarator.getEndLocation()));
        }

        /**
         * Helper class for returning the result of processing qualifiers of
         * a pointer type.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private static class PointerTypeQualifiers {
            private final boolean constQualified;
            private final boolean volatileQualified;
            private final boolean restrictQualified;
            private final Optional<Declarator> nextDeclarator;

            private PointerTypeQualifiers(boolean constQualified, boolean volatileQualified,
                    boolean restrictQualified, Optional<Declarator> nextDeclarator) {
                this.constQualified = constQualified;
                this.volatileQualified = volatileQualified;
                this.restrictQualified = restrictQualified;
                this.nextDeclarator = nextDeclarator;
            }
        }
    }

    /**
     * Visitor used for extracting types of parameters. It expects
     * <code>DataDecl</code> and <code>VariableDecl</code> declarations.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class ParametersTypesVisitor extends ExceptionVisitor<Void, Void> {
        /**
         * Environment that contains the processed parameters.
         */
        private final Environment environment;

        /**
         * Object that will be notified about detected errors and warnings.
         */
        private final ErrorHelper errorHelper;

        /**
         * Types of the parameters that are resolved.
         */
        private final List<Optional<Type>> types = new ArrayList<>();

        /**
         * <code>true</code> if and only if ellipsis has been encountered.
         */
        private boolean variableArguments = false;

        /**
         * <code>true</code> if and only if resolving the type of a parameter
         * failed.
         */
        private boolean typeError = false;

        private ParametersTypesVisitor(Environment environment, ErrorHelper errorHelper) {
            checkNotNull(environment, "the environment cannot be null");
            checkNotNull(errorHelper, "the error helper cannot be null");

            this.environment = environment;
            this.errorHelper = errorHelper;
        }

        @Override
        public Void visitDataDecl(DataDecl dataDecl, Void v) {
            checkState(dataDecl.getDeclarations().size() == 1, "expecting exactly one declaration for a parameter");
            dataDecl.getDeclarations().getFirst().accept(this, null);

            return null;
        }

        @Override
        public Void visitVariableDecl(VariableDecl variableDecl, Void v) {
            final Optional<Type> curArgType = variableDecl.getType();
            checkState(curArgType != null, "type in an variable declaration AST not set");

            if (!curArgType.isPresent()) {
                typeError = true;
            }

            types.add(curArgType);
            return null;
        }

        @Override
        public Void visitEllipsisDecl(EllipsisDecl ellipsisDecl, Void v) {
            variableArguments = true;
            return null;
        }

        @Override
        public Void visitErrorDecl(ErrorDecl errorDecl, Void v) {
            typeError = true;
            return null;
        }

        @Override
        public Void visitOldIdentifierDecl(OldIdentifierDecl oldStyleParamDecl, Void v) {
            // TODO add support for old-style parameters
            typeError = true;
            return null;
        }
    }

    /**
     * Visitor that visits types and checks if they are valid. Every detected
     * error and warning is passed to an error helper object.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class TypeValidityVisitor extends NullTypeVisitor<Void, Interval> {
        /**
         * Error helper that will be notified about detected errors and
         * warnings.
         */
        private final ErrorHelper errorHelper;

        /**
         * Remember the types of errors that have been emitted not to report too
         * many similar errors for one declaration.
         */
        private boolean incompleteElementType = false;
        private boolean functionElementType = false;
        private boolean arrayReturnType = false;
        private boolean functionReturnType = false;

        private TypeValidityVisitor(ErrorHelper errorHelper) {
            checkNotNull(errorHelper, "error helper cannot be null");
            this.errorHelper = errorHelper;
        }

        @Override
        public Void visit(ArrayType arrayType, Interval interval) {
            final Type elementType = arrayType.getElementType();
            boolean error = false;

            if (!incompleteElementType && !elementType.isComplete()) {
                incompleteElementType = error = true;
            }

            if (!functionElementType && elementType.isFunctionType()) {
                functionElementType = error = true;
            }

            if (error) {
                emitError(interval, new InvalidArrayElementTypeError(elementType));
            }

            return null;
        }

        @Override
        public Void visit(FunctionType funType, Interval interval) {
            final Type returnType = funType.getReturnType();
            boolean error = false;

            if (!functionReturnType && returnType.isFunctionType()) {
                functionReturnType = error = true;
            }

            if (!arrayReturnType && returnType.isArrayType()) {
                arrayReturnType = error = true;
            }

            if (error) {
                emitError(interval, new InvalidFunctionReturnTypeError(returnType));
            }

            return null;
        }

        private void emitError(Interval interval, ErroneousIssue error) {
            errorHelper.error(
                    interval.getLocation(),
                    interval.getEndLocation(),
                    error
            );
        }
    }
}
