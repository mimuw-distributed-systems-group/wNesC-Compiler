package pl.edu.mimuw.nesc.type;

import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.declaration.tag.UnionDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.BlockElement.BlockType;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Reflects a non-external union type, e.g.
 *  <code>union U { signed i; float f; };</code>.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 * @see FieldTagType
 */
public final class UnionType extends FieldTagType<UnionDeclaration> {
    /**
     * Initializes this union type with given arguments.
     *
     * @param unionDecl Object that actually represents this type.
     * @throws NullPointerException <code>unionDecl</code> is null.
     * @throws IllegalArgumentException Given union declaration corresponds to
     *                                  an external union.
     */
    public UnionType(boolean constQualified, boolean volatileQualified,
                     UnionDeclaration unionDecl) {
        super(constQualified, volatileQualified, unionDecl, BlockType.UNION);
        checkArgument(!unionDecl.isExternal(), "the union type cannot be external");
    }

    public UnionType(UnionDeclaration unionDecl) {
        this(false, false, unionDecl);
    }

    public UnionType(boolean constQualified, boolean volatileQualified,
            ImmutableList<Field> fields) {
        super(constQualified, volatileQualified, fields, BlockType.UNION);
    }

    public UnionType(ImmutableList<Field> fields) {
        this(false, false, fields);
    }

    @Override
    public final UnionType addQualifiers(boolean addConst, boolean addVolatile,
                                         boolean addRestrict) {
        switch (getVariant()) {
            case ONLY_DECLARATION:
                return new UnionType(addConstQualifier(addConst),
                        addVolatileQualifier(addVolatile), getDeclaration());
            case ONLY_FIELDS:
            case FULL:
                return new UnionType(addConstQualifier(addConst),
                        addVolatileQualifier(addVolatile), getFields());
            default:
                throw new RuntimeException("unexpected variant of a tag type object");
        }
    }

    @Override
    public final UnionType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                            boolean removeRestrict) {
        switch (getVariant()) {
            case ONLY_DECLARATION:
                return new UnionType(removeConstQualifier(removeConst),
                        removeVolatileQualifier(removeVolatile), getDeclaration());
            case ONLY_FIELDS:
            case FULL:
                return new UnionType(removeConstQualifier(removeConst),
                        removeVolatileQualifier(removeVolatile), getFields());
            default:
                throw new RuntimeException("unexpected variant of a tag type object");
        }
    }

    @Override
    public final boolean isExternal() {
        return false;
    }

    @Override
    public final boolean isExternalBaseType() {
        return false;
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
