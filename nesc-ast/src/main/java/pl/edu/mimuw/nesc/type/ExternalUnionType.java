package pl.edu.mimuw.nesc.type;

import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.declaration.tag.UnionDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.BlockElement.BlockType;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Reflects an external union type, e.g.
 * <code>nx_union { nx_int32_t u[10]; };</code>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 * @see FieldTagType
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

    public ExternalUnionType(boolean constQualified, boolean volatileQualified,
            ImmutableList<Field> fields) {
        super(constQualified, volatileQualified, fields, BlockType.EXTERNAL_UNION);
    }

    public ExternalUnionType(ImmutableList<Field> fields) {
        this(false, false, fields);
    }

    @Override
    public final ExternalUnionType addQualifiers(boolean addConst, boolean addVolatile,
                                                 boolean addRestrict) {
        switch (getVariant()) {
            case ONLY_DECLARATION:
                return new ExternalUnionType(addConstQualifier(addConst),
                        addVolatileQualifier(addVolatile), getDeclaration());
            case ONLY_FIELDS:
            case FULL:
                return new ExternalUnionType(addConstQualifier(addConst),
                        addVolatileQualifier(addVolatile), getFields());
            default:
                throw new RuntimeException("unexpected variant of a tag type object");
        }
    }

    @Override
    public final ExternalUnionType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                                    boolean removeRestrict) {
        switch (getVariant()) {
            case ONLY_DECLARATION:
                return new ExternalUnionType(removeConstQualifier(removeConst),
                        removeVolatileQualifier(removeVolatile), getDeclaration());
            case ONLY_FIELDS:
            case FULL:
                return new ExternalUnionType(removeConstQualifier(removeConst),
                        removeVolatileQualifier(removeVolatile), getFields());
            default:
                throw new RuntimeException("unexpected variant of a tag type object");
        }
    }

    @Override
    public final boolean isExternal() {
        return true;
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
