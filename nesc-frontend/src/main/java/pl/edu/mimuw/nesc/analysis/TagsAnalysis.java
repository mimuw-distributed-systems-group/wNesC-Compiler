package pl.edu.mimuw.nesc.analysis;

import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.declaration.object.ConstantDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.*;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.symboltable.SymbolTable;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;

/**
 * Class that contains code responsible for the semantic analysis.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class TagsAnalysis {
    /**
     * Updates information in the given environment that is related to tag
     * references encountered in the given specifiers collection. All detected
     * errors are reported.
     *
     * @param specifiers Collection of specifiers to process.
     * @param environment Environment to update with information related to
     *                    tags found in the given collection.
     * @param isStandalone <code>true</code> if and only if the declarations of
     *                     tags that will be potentially encountered are
     *                     standalone (the meaning of standalone definition is
     *                     written in the definition of
     *                     <code>TagRefVisitor</code> class).
     * @param errorHelper Object that will be notified about detected errors.
     * @throws NullPointerException One of the arguments is null
     *                              (except <code>isStandalone</code>).
     */
    public static void processTagReferences(Collection<TypeElement> specifiers,
                Environment environment, boolean isStandalone, ErrorHelper errorHelper) {
        // Validate arguments
        checkNotNull(specifiers, "specifiers cannot be null");
        checkNotNull(environment, "environment cannot be null");
        checkNotNull(errorHelper, "error helper cannot be null");

        // Process tag references
        final TagRefVisitor tagRefVisitor = new TagRefVisitor(environment, isStandalone, errorHelper);
        for (TypeElement specifier : specifiers) {
            specifier.accept(tagRefVisitor, null);
        }
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

        /**
         * List that is used to accumulate the enumerators from an enumeration
         * tag definition.
         */
        private final List<ConstantDeclaration> enumerators = new LinkedList<>();

        private TagRefVisitor(Environment environment, boolean isStandalone, ErrorHelper errorHelper) {
            this.errorHelper = errorHelper;
            this.environment = environment;
            this.isStandalone = isStandalone;
        }

        @Override
        public Void visitTypeElement(TypeElement typeElement, Void v) {
            return null;
        }

        @Override
        public Void visitTypeofType(TypeofType typeofType, Void v) {
            return null;
        }

        @Override
        public Void visitTypeofExpr(TypeofExpr typeofExpr, Void v) {
            return null;
        }

        @Override
        public Void visitTypename(Typename typename, Void v) {
            return null;
        }

        @Override
        public Void visitComponentTyperef(ComponentTyperef typeref, Void v) {
            return null;
        }

        @Override
        public Void visitRid(Rid rid, Void v) {
            return null;
        }

        @Override
        public Void visitQualifier(Qualifier qualifier, Void v) {
            return null;
        }

        @Override
        public Void visitAttribute(Attribute attribute, Void v) {
            return null;
        }

        @Override
        public Void visitGccAttribute(GccAttribute gccAttribute, Void v) {
            return null;
        }

        @Override
        public Void visitTargetAttribute(TargetAttribute targetAttribute, Void v) {
            return null;
        }

        @Override
        public Void visitNescAttribute(NescAttribute nescAttribute, Void v) {
            return null;
        }

        @Override
        public Void visitStructRef(StructRef structRef, Void v) {
            if (structRef.getName() == null) {
                return null;
            }

            if (!structRef.getIsDefined()) {
                declare(new StructDeclaration(Optional.of(structRef.getName().getName()),
                        structRef.getLocation(), structRef, false), structRef);
            }

            // TODO process the definition

            return null;
        }

        @Override
        public Void visitUnionRef(UnionRef unionRef, Void v) {
            if (unionRef.getName() == null) {
                return null;
            }

            if (!unionRef.getIsDefined()) {
                declare(new UnionDeclaration(Optional.of(unionRef.getName().getName()),
                        unionRef.getLocation(), unionRef, false), unionRef);
            }

            // TODO process the definition

            return null;
        }

        @Override
        public Void visitNxStructRef(NxStructRef nxStructRef, Void v) {
            // TODO
            return null;
        }

        @Override
        public Void visitNxUnionRef(NxUnionRef nxUnionRef, Void v) {
            // TODO
            return null;
        }

        @Override
        public Void visitAttributeRef(AttributeRef attrRef, Void v) {
            // TODO
            return null;
        }

        @Override
        public Void visitEnumRef(EnumRef enumRef, Void v) {
            if (enumRef.getName() == null) {
                return null;
            }

            EnumDeclaration enumDeclaration;
            if (!enumRef.getIsDefined()) {
                enumDeclaration = new EnumDeclaration(enumRef.getName().getName(),
                        enumRef.getLocation(), enumRef);
                declare(enumDeclaration, enumRef);
            } else {
                // Collect all enumerators before creating the object
                enumerators.clear();
                for (Declaration declaration : enumRef.getFields()) {
                    declaration.accept(this, null);
                }

                enumDeclaration = new EnumDeclaration(Optional.of(enumRef.getName().getName()),
                        enumRef.getLocation(), enumerators, enumRef);
                define(enumDeclaration, enumRef);
            }
            enumRef.setDeclaration(enumDeclaration);

            return null;
        }

        @Override
        public Void visitEnumerator(Enumerator enumerator, Void v) {
            enumerators.add(enumerator.getDeclaration());
            return null;
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
}
