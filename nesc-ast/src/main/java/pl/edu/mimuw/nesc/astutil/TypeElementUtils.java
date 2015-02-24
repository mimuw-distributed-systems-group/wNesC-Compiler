package pl.edu.mimuw.nesc.astutil;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration.FunctionType;

import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utilities for {@link TypeElement}.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class TypeElementUtils {

    private static final IsTypedefVisitor IS_TYPEDEF_VISITOR = new IsTypedefVisitor();
    private static final StructKindVisitor STRUCT_KIND_VISITOR = new StructKindVisitor();
    private static final SpecifierCollectorVisitor SPECIFIER_COLLECTOR_VISITOR = new SpecifierCollectorVisitor();

    /**
     * Set that contains keywords added to C in NesC language.
     */
    private static final ImmutableSet<RID> NESC_RID = ImmutableSet.of(
            RID.ASYNC,
            RID.COMMAND,
            RID.EVENT,
            RID.DEFAULT,
            RID.TASK,
            RID.NORACE
    );

    /**
     * Checks whether type elements contains <tt>TYPEDEF</tt> keyword.
     *
     * @param elements list of type elements of declaration
     * @return <code>true</code> if list contains <tt>TYPEDEF</tt>
     */
    public static boolean isTypedef(LinkedList<TypeElement> elements) {
        checkNotNull(elements, "elements list cannot be null");
        for (TypeElement element : elements) {
            if (element.accept(IS_TYPEDEF_VISITOR, null)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets function type implied by given modifiers.
     *
     * @param modifiers declaration's modifiers
     * @return one of nesc function types or <code>normal</code> if it is
     * plain C function
     */
    public static FunctionType getFunctionType(List<TypeElement> modifiers) {
        // FIXME: temporary solution, this kind of information should be
        // kept in type object
        for (TypeElement element : modifiers) {
            if (element instanceof Rid) {
                final Rid rid = (Rid) element;
                if (rid.getId() == RID.COMMAND) {
                    return FunctionType.COMMAND;
                }
                if (rid.getId() == RID.EVENT) {
                    return FunctionType.EVENT;
                }
                if (rid.getId() == RID.TASK) {
                    return FunctionType.TASK;
                }
            }
        }
        return FunctionType.NORMAL;
    }

    public static StructKind getStructKind(TagRef tagRef) {
        checkNotNull(tagRef, "tag reference cannot be null");
        return tagRef.accept(STRUCT_KIND_VISITOR, null);
    }

    /**
     * Get unique name of the type definition referred in given list.
     *
     * @param typeElements List with potential typename.
     * @return Unique name of the typename from the given list or absent if it
     *         does not contain a typename.
     */
    public static Optional<String> getTypedefUniqueName(List<? extends TypeElement> typeElements) {
        checkNotNull(typeElements, "type elements cannot be null");

        for (TypeElement typeElement : typeElements) {
            if (typeElement instanceof Typename) {
                return Optional.of(((Typename) typeElement).getUniqueName());
            }
        }

        return Optional.absent();
    }

    /**
     * Removes NesC-specific type elements from the given collection, i.e. NesC
     * keywords and NesC attributes. The iterator returned by the given
     * collection must support elements removal.
     *
     * @param typeElements Collection of type elements with an iterator
     *                     supporting elements removal.
     */
    public static void removeNescTypeElements(Collection<? extends TypeElement> typeElements) {
        checkNotNull(typeElements, "type elements cannot be null");

        final Iterator<? extends TypeElement> it = typeElements.iterator();
        while (it.hasNext()) {
            final TypeElement typeElement = it.next();
            final Optional<RID> optRID;

            if (typeElement instanceof Rid) {
                optRID = Optional.of(((Rid) typeElement).getId());
            } else if (typeElement instanceof Qualifier) {
                optRID = Optional.of(((Qualifier) typeElement).getId());
            } else {
                optRID = Optional.absent();
            }

            if (optRID.isPresent() && NESC_RID.contains(optRID.get())
                    || !optRID.isPresent() && typeElement instanceof NescAttribute) {
                it.remove();
            }
        }
    }

    /**
     * Collect all RID specifiers from <code>Rid</code> and
     * <code>Qualifier</code> AST nodes into a single set.
     *
     * @param typeElements Type elements with RID to collect.
     * @return Newly created set of RID found in <code>Rid</code> and
     *         <code>Qualifier</code> AST nodes from given list.
     */
    public static EnumSet<RID> collectRID(Collection<? extends TypeElement> typeElements) {
        checkNotNull(typeElements, "type elements cannot be null");

        final EnumSet<RID> ridSet = EnumSet.noneOf(RID.class);
        for (TypeElement typeElement : typeElements) {
            typeElement.accept(SPECIFIER_COLLECTOR_VISITOR, ridSet);
        }

        return ridSet;
    }

    /**
     * Remove from the given collection <code>Rid</code> and
     * <code>Qualifier</code> AST nodes that contain one of given rids.
     *
     * @param typeElements Collection with elements to removal.
     * @param rid First element for removal.
     * @param other Remaining elements from removal.
     */
    public static void removeRID(Collection<? extends TypeElement> typeElements, RID rid, RID... other) {
        checkNotNull(typeElements, "type elements cannot be null");

        final EnumSet<RID> ridSet = EnumSet.of(rid, other);
        final Iterator<? extends TypeElement> typeElementIt = typeElements.iterator();

        while (typeElementIt.hasNext()) {
            final TypeElement typeElement = typeElementIt.next();
            final Optional<RID> nestedRID;

            if (typeElement instanceof Rid) {
                nestedRID = Optional.of(((Rid) typeElement).getId());
            } else if (typeElement instanceof Qualifier) {
                nestedRID = Optional.of(((Qualifier) typeElement).getId());
            } else {
                nestedRID = Optional.absent();
            }

            if (nestedRID.isPresent() && ridSet.contains(nestedRID.get())) {
                typeElementIt.remove();
            }
        }
    }

    private TypeElementUtils() {
    }

    private static class IsTypedefVisitor extends ExceptionVisitor<Boolean, Void> {

        public Boolean visitTypeElement(TypeElement elem, Void arg) {
            throw new IllegalStateException("TypeElement object must not be instantiated.");
        }

        public Boolean visitTypename(Typename elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitTypeofExpr(TypeofExpr elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitTypeofType(TypeofType elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitAttribute(Attribute elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitGccAttribute(GccAttribute elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitRid(Rid elem, Void arg) {
            return RID.TYPEDEF.equals(elem.getId());
        }

        public Boolean visitQualifier(Qualifier elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitTagRef(TagRef elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitStructRef(StructRef elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitAttributeRef(AttributeRef elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitUnionRef(UnionRef elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitEnumRef(EnumRef elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitNxStructRef(NxStructRef elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitNxUnionRef(NxUnionRef elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitNescAttribute(NescAttribute elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitTargetAttribute(TargetAttribute elem, Void arg) {
            return Boolean.FALSE;
        }

    }

    /**
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class StructKindVisitor extends ExceptionVisitor<StructKind, Void> {

        @Override
        public StructKind visitAttributeRef(AttributeRef tagRef, Void arg) {
            return StructKind.ATTRIBUTE;
        }

        @Override
        public StructKind visitStructRef(StructRef tagRef, Void arg) {
            return StructKind.STRUCT;
        }

        @Override
        public StructKind visitNxStructRef(NxStructRef tagRef, Void arg) {
            return StructKind.NX_STRUCT;
        }

        @Override
        public StructKind visitUnionRef(UnionRef tagRef, Void arg) {
            return StructKind.UNION;
        }

        @Override
        public StructKind visitNxUnionRef(NxUnionRef tagRef, Void arg) {
            return StructKind.NX_UNION;
        }

        @Override
        public StructKind visitEnumRef(EnumRef tagRef, Void arg) {
            return StructKind.ENUM;
        }
    }

    /**
     * Visitor that adds visited specifiers to given collection.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class SpecifierCollectorVisitor extends ExceptionVisitor<Void, Collection<RID>> {

        @Override
        public Void visitRid(Rid elem, Collection<RID> arg) {
            arg.add(elem.getId());
            return null;
        }

        @Override
        public Void visitQualifier(Qualifier elem, Collection<RID> arg) {
            arg.add(elem.getId());
            return null;
        }

        @Override
        public Void visitTypename(Typename elem, Collection<RID> arg) {
            return null;
        }

        @Override
        public Void visitTypeofExpr(TypeofExpr elem, Collection<RID> arg) {
            return null;
        }

        @Override
        public Void visitTypeofType(TypeofType elem, Collection<RID> arg) {
            return null;
        }

        @Override
        public Void visitAttribute(Attribute elem, Collection<RID> arg) {
            return null;
        }

        @Override
        public Void visitGccAttribute(GccAttribute elem, Collection<RID> arg) {
            return null;
        }

        @Override
        public Void visitTagRef(TagRef elem, Collection<RID> arg) {
            return null;
        }

        @Override
        public Void visitStructRef(StructRef elem, Collection<RID> arg) {
            return null;
        }

        @Override
        public Void visitAttributeRef(AttributeRef elem, Collection<RID> arg) {
            return null;
        }

        @Override
        public Void visitUnionRef(UnionRef elem, Collection<RID> arg) {
            return null;
        }

        @Override
        public Void visitEnumRef(EnumRef elem, Collection<RID> arg) {
            return null;
        }

        @Override
        public Void visitComponentTyperef(ComponentTyperef elem, Collection<RID> arg) {
            return null;
        }

        @Override
        public Void visitNxStructRef(NxStructRef elem, Collection<RID> arg) {
            return null;
        }

        @Override
        public Void visitNxUnionRef(NxUnionRef elem, Collection<RID> arg) {
            return null;
        }

        @Override
        public Void visitNescAttribute(NescAttribute elem, Collection<RID> arg) {
            return null;
        }

        @Override
        public Void visitTargetAttribute(TargetAttribute elem, Collection<RID> arg) {
            return null;
        }
    }

}
