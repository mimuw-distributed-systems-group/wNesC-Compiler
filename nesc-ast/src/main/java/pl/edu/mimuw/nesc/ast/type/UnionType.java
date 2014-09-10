package pl.edu.mimuw.nesc.ast.type;

import pl.edu.mimuw.nesc.declaration.tag.UnionDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.BlockElement.BlockType;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Reflects a non-external union type, e.g.
 *  <code>union U { signed i; float f; };</code>.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
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

    @Override
    public final UnionType addQualifiers(boolean addConst, boolean addVolatile,
                                         boolean addRestrict) {
        return new UnionType(addConstQualifier(addConst),
                addVolatileQualifier(addVolatile), getDeclaration());
    }

    @Override
    public final UnionType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                            boolean removeRestrict) {
        return new UnionType(removeConstQualifier(removeConst),
                removeVolatileQualifier(removeVolatile), getDeclaration());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
