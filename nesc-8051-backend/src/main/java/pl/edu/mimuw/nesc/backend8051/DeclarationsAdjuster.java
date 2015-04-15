package pl.edu.mimuw.nesc.backend8051;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.astutil.predicates.TypeDeclaratorPredicate;
import pl.edu.mimuw.nesc.common.util.list.Lists;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Class that is responsible for adjusting declarations to SDCC. One of its
 * responsibilities are moving SDCC attributes that are storage-class extensions
 * to appropriate places and adjusting types of variables with one of SDCC
 * storage-class specifiers that are also types:</p>
 * <ul>
 *     <li><code>__sfr</code></li>
 *     <li><code>__sfr16</code></li>
 *     <li><code>__sfr32</code></li>
 *     <li><code>__sbit</code></li>
 *     <li><code>__bit</code></li>
 * </ul>
 *
 * <p>Moreover, the adjuster adds storage-class extensions directly before uses
 * of type definitions whose names have one of the following suffixes:</p>
 * <ul>
 *     <li><code>_xdata</code> (<code>__xdata</code> is added)</li>
 *     <li><code>_data</code> (<code>__data</code> is added)</li>
 *     <li><code>_code</code> (<code>__code</code> is added)</li>
 * </ul>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class DeclarationsAdjuster {
    /**
     * Set with keywords of attributes that will be moved close to the
     * identifier, to a qualified declarator or directly to type elements.
     */
    private static final ImmutableSet<String> KEYWORDS_CLOSE;
    static {
        final ImmutableSet.Builder<String> closeKeywordsBuilder = ImmutableSet.builder();
        closeKeywordsBuilder.addAll(StorageClassExtension.getPureKeywords());
        closeKeywordsBuilder.add("__at");
        KEYWORDS_CLOSE = closeKeywordsBuilder.build();
    }

    /**
     * Set with suffixes of names of type definitions that imply
     * a storage-class.
     */
    private static final ImmutableSet<String> STORAGE_TYPEDEFS_SUFFIXES =
            ImmutableSet.of("_xdata", "_data", "_code");

    /**
     * Visitor that will adjust the specifiers.
     */
    private final AdjustingVisitor adjustingVisitor = new AdjustingVisitor();

    /**
     * Type declarator predicate used by this instance of specifiers adjuster.
     */
    private final TypeDeclaratorPredicate typeDeclaratorPredicate = new TypeDeclaratorPredicate();

    /**
     * Adjust properly specifiers in declarations according to attributes of
     * declared variables.
     *
     * @param declarations Declarations that will be adjusted.
     */
    public void adjust(ImmutableList<Declaration> declarations) {
        checkNotNull(declarations, "declarations cannot be null");

        for (Declaration declaration : declarations) {
            declaration.traverse(adjustingVisitor, null);
        }
    }

    /**
     * Builds an attributes partition from the elements in the given list.
     * Elements that are in the partition are removed from the iterable.
     *
     * @param typeElements Iterable with elements to partition.
     * @return Attributes partition created from the given iterable.
     */
    private AttributesPartition moveAttributesFrom(Iterable<? extends TypeElement> typeElements) {
        final Iterator<? extends TypeElement> typeElementsIt = typeElements.iterator();
        final ImmutableList.Builder<TargetAttribute> closeAttributesBuilder = ImmutableList.builder();
        final ImmutableList.Builder<TargetAttribute> typeAttributesBuilder = ImmutableList.builder();

        while (typeElementsIt.hasNext()) {
            final TypeElement typeElement = typeElementsIt.next();

            if (!(typeElement instanceof TargetAttribute)) {
                continue;
            }

            final TargetAttribute attribute = (TargetAttribute) typeElement;
            final String keyword = attribute.getName().getName();
            final boolean remove;

            if (KEYWORDS_CLOSE.contains(keyword)) {
                closeAttributesBuilder.add(attribute);
                remove = true;
            } else if (StorageClassExtension.getTypeKeywords().contains(keyword)) {
                typeAttributesBuilder.add(attribute);
                remove = true;
            } else {
                remove = false;
            }

            if (remove) {
                typeElementsIt.remove();
            }
        }

        return new AttributesPartition(closeAttributesBuilder.build(),
                typeAttributesBuilder.build());
    }

    private void moveCloseToIdentifier(ImmutableList<TargetAttribute> attributes,
                DataDecl dataDecl, ImmutableList<Declarator> declarators) {
        if (attributes.isEmpty()) {
            return;
        }

        if (declarators.size() == 1) {
            // There are no nested declarators
            dataDecl.getModifiers().addAll(attributes);
        } else if (declarators.get(declarators.size() - 2) instanceof InterfaceRefDeclarator) {
            throw new RuntimeException("unexpected interface reference declarator");
        } else if (declarators.get(declarators.size() - 2) instanceof FunctionDeclarator) {
            throw new RuntimeException("attribute '" + attributes.get(0).getName().getName()
                    + "' applied to function '"
                    + ((IdentifierDeclarator) declarators.get(declarators.size() - 1)).getName()
                    + "' in its forward declaration");
        } else {
            // Look for a declarator that will contain the attributes
            int index = declarators.size() - 2;
            while (index - 1 >= 0 && declarators.get(index) instanceof ArrayDeclarator) {
                --index;
            }

            if (declarators.get(index) instanceof ArrayDeclarator) {
                // All nested declarators are array declarators
                dataDecl.getModifiers().addAll(attributes);
            } else if (declarators.get(index) instanceof FunctionDeclarator) {
                throw new RuntimeException("array of functions encountered");
            } else {
                final QualifiedDeclarator qualifiedDeclarator;
                if (declarators.get(index) instanceof QualifiedDeclarator) {
                    qualifiedDeclarator = (QualifiedDeclarator) declarators.get(index);
                } else {
                    final NestedDeclarator precedingDeclarator = (NestedDeclarator) declarators.get(index);
                    qualifiedDeclarator = new QualifiedDeclarator(Location.getDummyLocation(),
                            precedingDeclarator.getDeclarator(), Lists.<TypeElement>newList());
                    precedingDeclarator.setDeclarator(Optional.<Declarator>of(qualifiedDeclarator));
                }
                qualifiedDeclarator.getModifiers().addAll(attributes);
            }
        }
    }

    private void moveTypeToSpecifiers(ImmutableList<TargetAttribute> attributes,
                DataDecl dataDecl, ImmutableList<Declarator> declarators) {
        if (attributes.isEmpty()) {
            return;
        }

        if (Iterables.any(declarators, typeDeclaratorPredicate)) {
            throw new RuntimeException("a storage-class specifier that is also a type applied to a derived type");
        }

        // Remove colliding specifiers
        Iterables.removeIf(dataDecl.getModifiers(), new TypeElementRemoveController());

        // Add the storage-class extensions
        dataDecl.getModifiers().addAll(attributes);
    }

    private void handleTypedefsWithStorage(List<TypeElement> typeElements) {
        final ListIterator<TypeElement> typeElementsIt =
                typeElements.listIterator();

        while (typeElementsIt.hasNext()) {
            final TypeElement typeElement = typeElementsIt.next();

            if (!(typeElement instanceof Typename)) {
                continue;
            }

            final String typename = typeElement instanceof ComponentTyperef
                    ? ((ComponentTyperef) typeElement).getTypeName()
                    : ((Typename) typeElement).getName();
            Optional<String> storageClassKeyword = Optional.absent();

            for (String suffix : STORAGE_TYPEDEFS_SUFFIXES) {
                if (typename.endsWith(suffix)) {
                    storageClassKeyword = Optional.of("_" + suffix);
                }
            }

            if (storageClassKeyword.isPresent()) {
                typeElementsIt.set(AstUtils.newTargetAttribute0(storageClassKeyword.get()));
                typeElementsIt.add(typeElement);
            }
        }
    }

    /**
     * Visitor that performs the adjustment.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class AdjustingVisitor extends IdentityVisitor<Void> {
        @Override
        public Void visitAstType(AstType astType, Void arg) {
            handleTypedefsWithStorage(astType.getQualifiers());
            return null;
        }

        @Override
        public Void visitDataDecl(DataDecl dataDecl, Void arg) {
            handleTypedefsWithStorage(dataDecl.getModifiers());

            if (dataDecl.getDeclarations().isEmpty()) {
                return null;
            }

            final boolean containsVariableDecl = Iterables.any(dataDecl.getDeclarations(),
                    Predicates.instanceOf(VariableDecl.class));
            checkState(!containsVariableDecl || dataDecl.getDeclarations().size() == 1,
                    "a variable declaration that is not separated is present");

            if (!containsVariableDecl) {
                return null;
            }

            final VariableDecl variableDecl = (VariableDecl) dataDecl.getDeclarations().getFirst();

            if (!variableDecl.getDeclarator().isPresent()) {
                // Declaration of a parameter of a function
                return null;
            }

            final ImmutableList<Declarator> declarators =
                    DeclaratorUtils.createDeclaratorsList(variableDecl.getDeclarator().get());
            checkState(!declarators.isEmpty() && declarators.get(declarators.size() - 1) instanceof IdentifierDeclarator,
                    "unexpected structure of declarators of a variable");

            final AttributesPartition partition = moveAttributesFrom(variableDecl.getAttributes());
            moveTypeToSpecifiers(partition.typeAttributes, dataDecl, declarators);
            moveCloseToIdentifier(partition.closeAttributes, dataDecl, declarators);

            return null;
        }

        @Override
        public Void visitFunctionDecl(FunctionDecl functionDecl, Void arg) {
            handleTypedefsWithStorage(functionDecl.getModifiers());
            return null;
        }
    }

    /**
     * Visitor that decides if a type element is to be removed if
     * a storage-class extension that is also a type is used. Its purpose
     * its to identify type elements that collide with the storage-class
     * extension and point them for removal. <code>true</code> is returned
     * if a type element is to be removed.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class TypeElementRemoveController extends ExceptionVisitor<Boolean, Void>
                implements Predicate<TypeElement> {
        @Override
        public boolean apply(TypeElement typeElement) {
            checkNotNull(typeElement, "type element cannot be null");
            return typeElement.accept(this, null);
        }

        @Override
        public Boolean visitTypename(Typename typeElement, Void arg) {
            return true;
        }

        @Override
        public Boolean visitComponentTyperef(ComponentTyperef typeElement, Void arg) {
            return true;
        }

        @Override
        public Boolean visitRid(Rid typeElement, Void arg) {
            switch (typeElement.getId()) {
                case INT:
                case SHORT:
                case LONG:
                case CHAR:
                    return true;
                case FLOAT:
                case DOUBLE:
                    throw new RuntimeException("a storage-class extension that ia also a type applied to a variable of a floating-point type");
                case COMPLEX:
                    throw new RuntimeException("a storage-class extension that is also a type applied to a variable of a complex type");
                case VOID:
                    throw new RuntimeException("variable of a void type");
                default:
                    return false;
            }
        }

        @Override
        public Boolean visitQualifier(Qualifier typeElement, Void arg) {
            return false;
        }

        @Override
        public Boolean visitEnumRef(EnumRef typeElement, Void arg) {
            return true;
        }

        @Override
        public Boolean visitStructRef(StructRef typeElement, Void arg) {
            throw new RuntimeException("a storage-class extension that is a type applied to a variable that has a structure type");
        }

        @Override
        public Boolean visitUnionRef(UnionRef typeElement, Void arg) {
            throw new RuntimeException("a storage-class extension that is a type applied to a variable that has a union type");
        }

        @Override
        public Boolean visitNxStructRef(NxStructRef typeElement, Void arg) {
            throw new RuntimeException("a storage-class extension that is a type applied to a variable that has an external structure type");
        }

        @Override
        public Boolean visitNxUnionRef(NxUnionRef typeElement, Void arg) {
            throw new RuntimeException("a storage-class extension that is a type applied to a variable that has an external union type");
        }

        @Override
        public Boolean visitAttributeRef(AttributeRef typeElement, Void arg) {
            throw new RuntimeException("attribute reference as a type specifier");
        }

        @Override
        public Boolean visitTypeofExpr(TypeofExpr typeElement, Void arg) {
            return true;
        }

        @Override
        public Boolean visitTypeofType(TypeofType typeElement, Void arg) {
            return true;
        }

        @Override
        public Boolean visitGccAttribute(GccAttribute typeElement, Void arg) {
            return false;
        }

        @Override
        public Boolean visitTargetAttribute(TargetAttribute typeElement, Void arg) {
            return false;
        }

        @Override
        public Boolean visitNescAttribute(NescAttribute typeElement, Void arg) {
            return false;
        }
    }

    /**
     * Helper class for passing the two kinds of attributes (type and close)
     * separated.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class AttributesPartition {
        private final ImmutableList<TargetAttribute> closeAttributes;
        private final ImmutableList<TargetAttribute> typeAttributes;

        private AttributesPartition(ImmutableList<TargetAttribute> closeAttributes,
                    ImmutableList<TargetAttribute> typeAttributes) {
            checkNotNull(closeAttributes, "close attributes cannot be null");
            checkNotNull(typeAttributes, "type attributes cannot be null");
            this.closeAttributes = closeAttributes;
            this.typeAttributes = typeAttributes;
        }
    }
}
