package pl.edu.mimuw.nesc.analysis;

import com.google.common.base.Function;
import pl.edu.mimuw.nesc.declaration.tag.EnumDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.StructDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.TagDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.UnionDeclaration;
import pl.edu.mimuw.nesc.ast.type.*;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.problem.ErrorHelper;

import com.google.common.base.Optional;
import com.google.common.collect.EnumMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static pl.edu.mimuw.nesc.analysis.TagsAnalysis.*;
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
     * Constants used throughout the whole class.
     */
    private static final String FMT_WARN_QUALIFIER = "'%s' type qualifier ignored because it has been already specified; remove it";

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
         * Constants with error format strings.
         */
        private static final String FMT_WARN_RESTRICT = "'%s' type qualifier ignored because it cannot be applied to a non-pointer type; remove it";
        private static final String FMT_ERR_SPECIFIER = "Cannot use '%s' type specifier because it has been already specified";
        private static final String FMT_ERR_LONG = "Cannot use '%s' type specifier because it has been already specified twice";
        private static final String FMT_ERR_TAG = "Cannot combine '%s' type specifier with a tag type specifier";
        private static final String FMT_ERR_MULTIPLE_TAGS = "Cannot combine a tag type specifier with an another tag type specifier";
        private static final String FMT_ERR_TAG_CONFLICT = "Cannot combine a tag type specifier with previously used type specifiers";
        private static final String FMT_ERR_INVALID_COMBINATION = "Invalid combination of type specifiers";
        private static final String FMT_ERR_NO_TYPE_SPECIFIERS = "Expecting a type specifier";

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
         * Keys are classes that represent tag type specifiers and values are
         * classes expected for them in the symbol table.
         */
        private static final Map<Class<? extends TagRef>, Class<? extends TagDeclaration>> TAG_REF_MAP;
        static {
            final Map<Class<? extends TagRef>, Class<? extends TagDeclaration>> tagRefMap = new HashMap<>();
            tagRefMap.put(EnumRef.class, EnumDeclaration.class);
            tagRefMap.put(StructRef.class, StructDeclaration.class);
            tagRefMap.put(NxStructRef.class, StructDeclaration.class);
            tagRefMap.put(UnionRef.class, UnionDeclaration.class);
            tagRefMap.put(NxUnionRef.class, UnionDeclaration.class);
            TAG_REF_MAP = Collections.unmodifiableMap(tagRefMap);
        }

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
        private final Multiset<RID> typeSpecifiers = EnumMultiset.create(RID.class);
        private TagRef tagReference;

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

            if (!typeSpecifiers.isEmpty()) {
                return finishFundamentalType();
            } else if (tagAccepted()) {
                return finishTagType();
            } else {
                typeError = true;
                errorHelper.error(startFuzzy, endFuzzy, FMT_ERR_NO_TYPE_SPECIFIERS);
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
                errorHelper.error(startTypeSpecifiers, endTypeSpecifiers,
                                  FMT_ERR_INVALID_COMBINATION);
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
            final Optional<? extends TagDeclaration> maybeTagDeclaration = environment.getTags().get(name);
            if (!maybeTagDeclaration.isPresent()) {
                typeError = true;
                return Optional.absent();
            }

            final TagDeclaration tagDeclaration = maybeTagDeclaration.get();
            final Class<? extends TagDeclaration> expectedClass = TAG_REF_MAP.get(tagReference.getClass());
            assert expectedClass != null : "unexpected tag reference class '"
                    + tagReference.getClass().getCanonicalName() + "'";
            if (!tagDeclaration.getClass().equals(expectedClass)) {
                /* This error should have been already emitted because of the
                   earlier processing of tags declarations. */
                typeError = true;
                return Optional.absent();
            }

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

        @Override
        public Void visitRid(Rid rid, Void v) {
            processRID(rid.getId(), rid.getLocation(), rid.getEndLocation());
            return null;
        }

        @Override
        public Void visitQualifier(Qualifier qualifier, Void v) {
            processRID(qualifier.getId(), qualifier.getLocation(), qualifier.getEndLocation());
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
            // TODO resolve typedefs
            return null;
        }

        @Override
        public Void visitComponentTyperef(ComponentTyperef typeref, Void v) {
            // TODO resolve typedefs from other components
            return null;
        }

        @Override
        public Void visitAttributeRef(AttributeRef attrRef, Void v) {
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

        private void processRID(RID rid, Location startLoc, Location endLoc) {
            String warnMsg = null;
            String errMsg = null;

            switch (rid) {
                case CONST:
                    if (isConstQualified) {
                        warnMsg = format(FMT_WARN_QUALIFIER, rid.getName());
                    }
                    isConstQualified = true;
                    break;
                case VOLATILE:
                    if (isVolatileQualified) {
                        warnMsg = format(FMT_WARN_QUALIFIER, rid.getName());
                    }
                    isVolatileQualified = true;
                    break;
                case RESTRICT:
                    warnMsg = format(FMT_WARN_RESTRICT, rid.getName());
                    break;
                default:
                    if (!TYPE_SPECIFIERS.contains(rid)) {
                        return;
                    }
                    updateLocations(startLoc, endLoc);
                    if (tagAccepted()) {
                        errMsg = format(FMT_ERR_TAG, rid.getName());
                    } else if (rid != RID.LONG && typeSpecifiers.contains(rid)) {
                        errMsg = format(FMT_ERR_SPECIFIER, rid.getName());
                    } else if (rid == RID.LONG && typeSpecifiers.count(rid) >= MAX_LONG_COUNT) {
                        errMsg = format(FMT_ERR_LONG, rid.getName());
                    } else {
                        typeSpecifiers.add(rid);
                    }
                    break;
            }

            if (errMsg != null) {
                typeError = true;
                errorHelper.error(startLoc, endLoc, errMsg);
            }
            if (warnMsg != null) {
                errorHelper.warning(startLoc, Optional.of(endLoc), warnMsg);
            }
        }

        private void acceptTag(TagRef tagRef) {
            updateLocations(tagRef.getLocation(), tagRef.getEndLocation());
            String errMsg = null;

            if (tagAccepted()) {
                errMsg = FMT_ERR_MULTIPLE_TAGS;
            } else if (!typeSpecifiers.isEmpty()) {
                errMsg = FMT_ERR_TAG_CONFLICT;
            }

            if (errMsg != null) {
                typeError = true;
                errorHelper.error(tagRef.getLocation(), tagRef.getEndLocation(), errMsg);
                return;
            }

            processTagReference(tagRef, environment, isStandalone, errorHelper);
            tagReference = tagRef;
        }

        /**
         * @return <code>true</code> if and only if a tag has been accepted as
         *         a type specifier by this object.
         */
        private boolean tagAccepted() {
            return tagReference != null;
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
            if (startTypeSpecifiers == null) {
                startTypeSpecifiers = startLoc;
            }

            endTypeSpecifiers = endLoc;
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
         * Constants with error and warning messages.
         */
        private static final String FMT_ERR_INVALID_SPECIFIER = "Unexpected declaration specifier '%s'";
        private static final String FMT_WARN_QUALIFIERS_IGNORED = "Type qualifiers ignored because they cannot be used in this context; remove them";

        /**
         * Object that will be notified about detected errors.
         */
        private final ErrorHelper errorHelper;

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
            accumulatedType = new ArrayType(accumulatedType, declarator.getIndex() != null);
            jump(declarator.getDeclarator());
            return null;
        }

        @Override
        public Void visitPointerDeclarator(PointerDeclarator declarator, Void v) {
            final PointerTypeQualifiers qualifiers = processPointerQualifiers(declarator);
            accumulatedType = new PointerType(qualifiers.constQualified,
                    qualifiers.volatileQualified, qualifiers.restrictQualified,
                    accumulatedType);
            jump(qualifiers.nextDeclarator);
            return null;
        }

        @Override
        public Void visitFunctionDeclarator(FunctionDeclarator declarator, Void v) {
            final ParametersTypesVisitor paramsVisitor = new ParametersTypesVisitor(
                    declarator.getEnvironment(),
                    errorHelper
            );

            for (Declaration paramDecl : declarator.getParameters()) {
                paramDecl.accept(paramsVisitor, null);
            }

            typeError = typeError || paramsVisitor.typeError;
            accumulatedType = new FunctionType(accumulatedType, paramsVisitor.types,
                    paramsVisitor.variableArguments);
            jump(ignoreQualifiers(declarator.getDeclarator()));
            return null;
        }

        private void jump(Declarator next) {
            if (next != null) {
                next.accept(this, null);
            }
        }

        private PointerTypeQualifiers processPointerQualifiers(PointerDeclarator startDeclarator) {
            boolean isConstQualified = false,
                    isVolatileQualified = false,
                    isRestrictQualified = false;
            Declarator nextDeclarator = startDeclarator.getDeclarator();

            while (nextDeclarator instanceof QualifiedDeclarator) {
                final QualifiedDeclarator qualified = (QualifiedDeclarator) nextDeclarator;

                for (TypeElement typeElement : qualified.getModifiers()) {
                    if (!(typeElement instanceof Qualifier)) {
                        throw new RuntimeException(format("unexpected type element of class '%s' in a pointer declarator",
                                typeElement.getClass().getCanonicalName()));
                    }
                    final Qualifier qualifier = (Qualifier) typeElement;

                    boolean repetition = false;
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
                            typeError = true;
                            errorHelper.error(qualifier.getLocation(), qualifier.getEndLocation(),
                                    format(FMT_ERR_INVALID_SPECIFIER, qualifier.getId().getName()));
                            break;
                    }

                    if (repetition) {
                        errorHelper.warning(qualifier.getLocation(), Optional.of(qualifier.getEndLocation()),
                                format(FMT_WARN_QUALIFIER, qualifier.getId().getName()));
                    }
                }

                nextDeclarator = qualified.getDeclarator();
            }

            return new PointerTypeQualifiers(isConstQualified, isVolatileQualified,
                    isRestrictQualified, nextDeclarator);
        }

        private Declarator ignoreQualifiers(Declarator declarator) {
            while (declarator instanceof QualifiedDeclarator) {
                final QualifiedDeclarator qualified = (QualifiedDeclarator) declarator;

                if (!qualified.getModifiers().isEmpty()) {
                    errorHelper.warning(
                            qualified.getLocation(),
                            Optional.of(qualified.getEndLocation()),
                            FMT_WARN_QUALIFIERS_IGNORED
                    );
                }

                declarator = qualified.getDeclarator();
            }

            return declarator;
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
            private final Declarator nextDeclarator;

            private PointerTypeQualifiers(boolean constQualified, boolean volatileQualified,
                    boolean restrictQualified, Declarator nextDeclarator) {
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
        private final List<Type> types = new ArrayList<>();

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
            // TODO support for 'void' that indicate no parameters

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
                return null;
            }

            types.add(curArgType.get());
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
}
