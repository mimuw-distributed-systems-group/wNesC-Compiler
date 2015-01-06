package pl.edu.mimuw.nesc.type;

import pl.edu.mimuw.nesc.declaration.tag.UnionDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.BlockElement.BlockType;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Reflects an external union type, e.g.
 * <code>nx_union { nx_int32_t u[10]; };</code>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ExternalUnionType extends FieldTagType<UnionDeclaration> {
    public ExternalUnionType(boolean constQualified, boolean volatileQualified,
                             UnionDeclaration unionDecl) {
        super(constQualified, volatileQualified, unionDecl, BlockType.EXTERNAL_UNION);
        checkArgument(unionDecl.isExternal(), "the union must be external");
    }

    public ExternalUnionType(UnionDeclaration unionDecl) {
        this(false, false, unionDecl);
    }

    @Override
    public final ExternalUnionType addQualifiers(boolean addConst, boolean addVolatile,
                                                 boolean addRestrict) {
        return new ExternalUnionType(addConstQualifier(addConst),
                addVolatileQualifier(addVolatile), getDeclaration());
    }

    @Override
    public final ExternalUnionType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                                    boolean removeRestrict) {
        return new ExternalUnionType(removeConstQualifier(removeConst),
                removeVolatileQualifier(removeVolatile), getDeclaration());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
