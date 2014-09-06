package pl.edu.mimuw.nesc.analysis;

import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.astbuilding.DeclaratorUtils;
import pl.edu.mimuw.nesc.declaration.object.ConstantDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.*;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.symboltable.SymbolTable;
import pl.edu.mimuw.nesc.ast.type.FieldTagType;
import pl.edu.mimuw.nesc.ast.type.Type;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static pl.edu.mimuw.nesc.analysis.TypesAnalysis.resolveDeclaratorType;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * Class that contains code responsible for the semantic analysis.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class TagsAnalysis {
    /**
     * Updates information in the given environment that is related to the given
     * tag reference. All detected errors are reported.
     *
     * @param tagReference Tag reference to process.
     * @param environment Environment to update with information related to
     *                    given tag.
     * @param isStandalone <code>true</code> if and only if the given tag
     *                     reference is standalone (the meaning of standalone
     *                     definition is written in the definition of
     *                     <code>TagRefVisitor</code> class).
     * @param errorHelper Object that will be notified about detected errors.
     * @throws NullPointerException One of the arguments is null
     *                              (except <code>isStandalone</code>).
     */
    static void processTagReference(TagRef tagReference, Environment environment,
            boolean isStandalone, ErrorHelper errorHelper) {
        // Validate arguments
        checkNotNull(tagReference, "tag reference cannot be null");
        checkNotNull(environment, "environment cannot be null");
        checkNotNull(errorHelper, "error helper cannot be null");

        // Process tag references
        final TagRefVisitor tagRefVisitor = new TagRefVisitor(environment, isStandalone, errorHelper);
        tagReference.accept(tagRefVisitor, null);
    }

    /**
     * Unconditionally creates a <code>StructDeclaration</code> object that
     * reflects the given structure reference.
     *
     * @return Newly created declaration that reflects the given structure
     *         reference.
     * @throws NullPointerException One of the arguments is null.
     */
    static StructDeclaration makeStructDeclaration(ErrorHelper errorHelper, StructRef structRef) {
        return (StructDeclaration) makeFieldTagDeclaration(errorHelper, structRef);
    }

    /**
     * Unconditionally creates a <code>UnionDeclaration</code> object that
     * reflects the given union reference.
     *
     * @return Newly created declaration object that reflects the given union
     *         reference.
     * @throws NullPointerException One of the arguments is null.
     */
    static UnionDeclaration makeUnionDeclaration(ErrorHelper errorHelper, UnionRef unionRef) {
        return (UnionDeclaration) makeFieldTagDeclaration(errorHelper, unionRef);
    }

    /**
     * Creates a new <code>FieldDeclaration</code> object that corresponds to
     * the given field. The given field is associated with the created object
     * and is available with its <code>getDeclaration</code> method.
     */
    public static void makeFieldDeclaration(FieldDecl fieldDecl, Optional<Type> maybeBaseType,
                                            ErrorHelper errorHelper) {
        final Optional<Declarator> maybeDeclarator = fieldDecl.getDeclarator();
        final Optional<Expression> maybeBitField = fieldDecl.getBitfield();

        Optional<Type> fullType = maybeBaseType;
        Optional<String> name = Optional.absent();

        if (maybeDeclarator.isPresent()) {
            final Declarator declarator = maybeDeclarator.get();
            name = Optional.fromNullable(DeclaratorUtils.getDeclaratorName(declarator));

            if (maybeBaseType.isPresent()) {
                fullType = resolveDeclaratorType(declarator, errorHelper, maybeBaseType.get());
            }
        }

        // Create and acknowledge the field
        final FieldDeclaration newField = new FieldDeclaration(name, fieldDecl.getLocation(),
                fieldDecl.getEndLocation(), fullType, maybeBitField.isPresent());
        fieldDecl.setDeclaration(newField);
    }

    /**
     * This method is created only not to duplicate code.
     * <code>makeStructDeclaration</code> and <code>makeUnionDeclaration</code>
     * methods should be used instead.
     */
    private static FieldTagDeclaration makeFieldTagDeclaration(ErrorHelper errorHelper,
            final TagRef tagRef) {
        checkNotNull(errorHelper, "error helper cannot be null");
        checkNotNull(tagRef, "tag reference cannot be null");
        checkArgument(tagRef instanceof StructRef || tagRef instanceof UnionRef,
                "unexpected class of the given tag reference");

        final boolean isExternal = tagRef instanceof NxStructRef
                || tagRef instanceof NxUnionRef;

        // Declaration but not definition
        if (!tagRef.getIsDefined()) {
            if (tagRef instanceof StructRef) {
                return new StructDeclaration(tagRef.getName().getName(), tagRef.getLocation(),
                        (StructRef) tagRef, isExternal);
            } else {
                return new UnionDeclaration(tagRef.getName().getName(), tagRef.getLocation(),
                        (UnionRef) tagRef, isExternal);
            }
        }

        // Definition
        final FieldTagDefinitionVisitor visitor = new FieldTagDefinitionVisitor(errorHelper);
        for (Declaration declaration : tagRef.getFields()) {
            declaration.accept(visitor, null);
        }

        if (tagRef instanceof StructRef) {
            return new StructDeclaration(getTagName(tagRef), tagRef.getLocation(),
                    (StructRef) tagRef, isExternal, visitor.fields);
        } else {
            return new UnionDeclaration(getTagName(tagRef), tagRef.getLocation(),
                    (UnionRef) tagRef, isExternal, visitor.fields);
        }
    }

    /**
     * Unconditionally creates an <code>EnumDeclaration</code> object that
     * reflects the given <code>EnumRef</code> object.
     *
     * @return Newly created declaration object that reflects the given
     *         enumeration reference.
     * @throws NullPointerException Given argument is null.
     */
    static EnumDeclaration makeEnumDeclaration(EnumRef enumRef) {
        checkNotNull(enumRef, "enum reference cannot be null");

        if (!enumRef.getIsDefined()) {
            return new EnumDeclaration(enumRef.getName().getName(), enumRef.getLocation(),
                                       enumRef);
        }

        final List<ConstantDeclaration> enumerators = new LinkedList<>();
        for (Declaration declaration : enumRef.getFields()) {
            if (!(declaration instanceof Enumerator)) {
                throw new RuntimeException("an enumerator has class '"
                        + declaration.getClass().getCanonicalName() + "'");
            }

            final Enumerator enumerator = (Enumerator) declaration;
            enumerators.add(enumerator.getDeclaration());
        }

        return new EnumDeclaration(getTagName(enumRef), enumRef.getLocation(),
                                   enumerators, enumRef);
    }

    private static Optional<String> getTagName(TagRef tagRef) {
        final String name =   tagRef.getName() != null
                            ? tagRef.getName().getName()
                            : null;
        return Optional.fromNullable(name);
    }

    /**
     * A visitor that adds information about encountered tags to the symbol
     * table. It should be used only on objects of classes derived from
     * <code>TypeElement</code>.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class TagRefVisitor extends ExceptionVisitor<Void, Void> {
        /**
         * Object that will be informed about each encountered error.
         */
        private final ErrorHelper errorHelper;

        /**
         * Environment that will be modified by this Visitor.
         */
        private final Environment environment;

        /**
         * <code>true</code> if and only if the tag that will be encountered by
         * this Visitor has been declared exactly in one of the following ways
         * (where 'S' and 'U' are arbitrary identifiers):
         *    struct S;
         *    union U;
         */
        private final boolean isStandalone;

        private TagRefVisitor(Environment environment, boolean isStandalone, ErrorHelper errorHelper) {
            this.errorHelper = errorHelper;
            this.environment = environment;
            this.isStandalone = isStandalone;
        }

        @Override
        public Void visitStructRef(final StructRef structRef, Void v) {
            final Supplier<StructDeclaration> supplier = new Supplier<StructDeclaration>() {
                @Override
                public StructDeclaration get() {
                    return makeStructDeclaration(errorHelper, structRef);
                }
            };

            final Optional<StructDeclaration> declaration = processTagRef(structRef, supplier);
            if (declaration.isPresent()) {
                structRef.setDeclaration(declaration.get());
            }

            return null;
        }

        @Override
        public Void visitUnionRef(final UnionRef unionRef, Void v) {
            final Supplier<UnionDeclaration> supplier = new Supplier<UnionDeclaration>() {
                @Override
                public UnionDeclaration get() {
                    return makeUnionDeclaration(errorHelper, unionRef);
                }
            };

            final Optional<UnionDeclaration> declaration = processTagRef(unionRef, supplier);
            if (declaration.isPresent()) {
                unionRef.setDeclaration(declaration.get());
            }

            return null;
        }

        @Override
        public Void visitNxStructRef(NxStructRef nxStructRef, Void v) {
            visitStructRef(nxStructRef, v);
            return null;
        }

        @Override
        public Void visitNxUnionRef(NxUnionRef nxUnionRef, Void v) {
            visitUnionRef(nxUnionRef, v);
            return null;
        }

        @Override
        public Void visitAttributeRef(AttributeRef attrRef, Void v) {
            checkState(attrRef.getIsDefined(), "attribute reference that is not definition of an attribute");
            checkState(attrRef.getName() != null, "name of an attribute in its definition is null");

            if (!isStandalone) {
                errorHelper.error(attrRef.getLocation(), attrRef.getEndLocation(),
                        "Cannot use an attribute definition as a type");
            }

            // Get fields of the attribute definition
            FieldTagDefinitionVisitor visitor = new FieldTagDefinitionVisitor(errorHelper);
            for (Declaration declaration : attrRef.getFields()) {
                declaration.accept(visitor, null);
            }

            // Create the object that represents the attribute and define it
            final AttributeDeclaration attrDeclaration = new AttributeDeclaration(attrRef.getName().getName(),
                    attrRef.getLocation(), attrRef, visitor.fields);
            define(attrDeclaration, attrRef);

            return null;
        }

        @Override
        public Void visitEnumRef(final EnumRef enumRef, Void v) {
            final Supplier<EnumDeclaration> supplier = new Supplier<EnumDeclaration>() {
                @Override
                public EnumDeclaration get() {
                    return makeEnumDeclaration(enumRef);
                }
            };

            final Optional<EnumDeclaration> declaration = processTagRef(enumRef, supplier);
            if (declaration.isPresent()) {
                enumRef.setDeclaration(declaration.get());
            }

            return null;
        }

        private <T extends TagDeclaration> Optional<T> processTagRef(TagRef tagRef, Supplier<T> supplier) {
            if (tagRef.getName() == null) {
                return Optional.absent();
            }

            final T declaration = supplier.get();
            if (!tagRef.getIsDefined()) {
                declare(declaration, tagRef);
            } else {
                define(declaration, tagRef);
            }

            return Optional.of(declaration);
        }

        private void declare(TagDeclaration tagDeclaration, TagRef tagRef) {
            if (!tagDeclaration.getName().isPresent()) {
                return;
            }

            final String name = tagDeclaration.getName().get();
            final SymbolTable<TagDeclaration> tagsTable = environment.getTags();
            final TagPredicate predicate = new TagPredicate(tagDeclaration.getClass(), false);

            // Check if it is actually a declaration
            if (!isStandalone && tagsTable.contains(name)) {
                final Optional<Boolean> sameTag = tagsTable.test(name, predicate);
                if (!sameTag.isPresent()) {
                    throw new RuntimeException("unexpected test result in the tags table");
                } else if (!sameTag.get()) {
                    errorHelper.error(tagRef.getLocation(), tagRef.getEndLocation(),
                            format("'%s' has been earlier declared as a tag of an another type",
                                    name));
                }
                return;
            }

            final Optional<Boolean> sameTag = tagsTable.test(name, predicate, true);

            if (!sameTag.isPresent()) {
                environment.getTags().add(name, tagDeclaration);
            } else if (!sameTag.get()) {
                errorHelper.error(tagRef.getLocation(), tagRef.getEndLocation(),
                        format("'%s' has been previously declared as a tag of an another "
                                + "type", name));
            }

            // Check the correctness of an enumeration tag declaration
            if (tagRef instanceof EnumRef) {
                String errMsg = null;
                if (!isStandalone && (!predicate.isDefined || !sameTag.isPresent())) {
                    errMsg = format("'%s' is undefined; cannot use an enumeration type "
                            + "with no visible definition", name);
                } else if (isStandalone) {
                    errMsg = "Invalid declaration; forward declarations of enumeration types "
                            + "are forbidden in the ISO C standard";
                }

                if (errMsg != null) {
                    errorHelper.error(tagRef.getLocation(), tagRef.getEndLocation(), errMsg);
                }
            }
        }

        private void define(TagDeclaration tagDeclaration, TagRef tagRef) {
            if (!tagDeclaration.getName().isPresent()) {
                return;
            }

            final String name = tagDeclaration.getName().get();
            final TagPredicate predicate = new TagPredicate(tagDeclaration.getClass(), true);
            final boolean result = environment.getTags().addOrOverwriteIf(name, tagDeclaration, predicate);

            if (!result) {
                if (!predicate.sameClass) {
                    errorHelper.error(tagRef.getLocation(), tagRef.getEndLocation(),
                            format("'%s' has been previously declared as a tag of an another type", name));
                } else if (predicate.isDefined) {
                    errorHelper.error(tagRef.getLocation(), tagRef.getEndLocation(),
                            format("'%s' has been already defined", name));
                } else {
                    throw new RuntimeException("unexpected symbol table result during a tag definition");
                }
            }
        }

        /**
         * A class that allows testing information about the tags that are
         * currently in the symbol table.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private class TagPredicate implements Predicate<TagDeclaration> {
            private boolean isDefined;
            private boolean sameClass;
            private final boolean mustBeUndefined;
            private final Class<?> expectedClass;

            private TagPredicate(Class<?> expectedClass, boolean mustBeUndefined) {
                this.expectedClass = expectedClass;
                this.mustBeUndefined = mustBeUndefined;
            }

            @Override
            public boolean apply(TagDeclaration decl) {
                sameClass = decl.getClass().equals(expectedClass);
                isDefined = decl.isDefined();
                return sameClass && (!mustBeUndefined || !isDefined);
            }
        }
    }

    /**
     * Visitor class that accumulates information about a field tag definition.
     * It expects to visit <code>DataDecl</code> objects. One instance of this
     * object shall be used to examine only one field tag definition.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class FieldTagDefinitionVisitor extends ExceptionVisitor<Void, Void> {
        /**
         * Constants used by this visitor.
         */
        private static final String FMT_ERR_FIELD_REDECLARATION = "Redeclaration of field '%s'";

        /**
         * Object that will be notified about detected errors and warnings.
         */
        private final ErrorHelper errorHelper;

        /**
         * Set with names of fields that has been already acknowledged.
         */
        private final Set<String> fieldsNames = new HashSet<>();

        /**
         * Consecutive fields of the analyzed field tag.
         */
        private final List<FieldDeclaration> fields = new ArrayList<>();

        private FieldTagDefinitionVisitor(ErrorHelper errorHelper) {
            this.errorHelper = errorHelper;
        }

        @Override
        public Void visitDataDecl(DataDecl dataDecl, Void v) {
            final List<Declaration> declarations = dataDecl.getDeclarations();

            // Determine the base type of elements declared in this declaration
            final Optional<Type> maybeBaseType = dataDecl.getType();
            checkState(maybeBaseType != null, "base type in a DataDecl object is null");

            // Process new fields
            if (!declarations.isEmpty()) {
                for (Declaration declaration : declarations) {
                    declaration.accept(this, null);
                }
            } else {
                /* Check if it is an unnamed field of an unnamed field tag type.
                   If so, add fields from it to this structure. */
                if (maybeBaseType.isPresent()) {
                    final Type baseType = maybeBaseType.get();

                    if (baseType.isFieldTagType()) {
                        final FieldTagType fieldTagType = (FieldTagType) baseType;
                        final FieldTagDeclaration fieldDecl = fieldTagType.getDeclaration();

                        if (!fieldDecl.getName().isPresent()) {
                            final Optional<List<FieldDeclaration>> maybeAllFields = fieldDecl.getAllFields();
                            final List<FieldDeclaration> allFields = maybeAllFields.get();

                            for (FieldDeclaration field : allFields) {
                                appendField(field);
                            }
                        }
                    }
                }
            }

            return null;
        }

        @Override
        public Void visitFieldDecl(FieldDecl fieldDecl, Void v) {
            final FieldDeclaration fieldDeclaration = fieldDecl.getDeclaration();
            checkState(fieldDeclaration != null, "a FieldDecl object is not " +
                       "associated with its FieldDeclaration object");
            appendField(fieldDeclaration);
            return null;
        }

        @Override
        public Void visitExtensionDecl(ExtensionDecl extDecl, Void v) {
            extDecl.getDeclaration().accept(this, null);
            return null;
        }

        @Override
        public Void visitErrorDecl(ErrorDecl errorDecl, Void v) {
            return null;
        }

        private void appendField(FieldDeclaration field) {
            // Check if there has not been any fields with the name
            final Optional<String> maybeName = field.getName();
            if (maybeName.isPresent()) {
                final String name = maybeName.get();

                if (fieldsNames.contains(name)) {
                    errorHelper.error(field.getLocation(), field.getEndLocation(),
                                      format(FMT_ERR_FIELD_REDECLARATION, name));
                }

                fieldsNames.add(name);
            }

            // Add the field
            fields.add(field);
        }
    }
}
