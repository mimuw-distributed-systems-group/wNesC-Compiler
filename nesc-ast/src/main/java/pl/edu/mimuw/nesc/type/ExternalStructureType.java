package pl.edu.mimuw.nesc.type;

import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.declaration.tag.StructDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.BlockElement.BlockType;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Reflects an external structure type, e.g.
 * <code>nx_struct { nx_int32_t n; nx_int8_t c; }</code>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 * @see FieldTagType
 */
public final class ExternalStructureType extends FieldTagType<StructDeclaration> {
    /**
     * Initializes this structure type with given arguments.
     *
     * @param structDecl Declaration that actually represents this structure
     *                   type.
     * @throws NullPointerException <code>structDecl</code> is null.
     * @throws IllegalArgumentException The given structure declaration does not
     *                                  correspond to an external structure.
     */
    public ExternalStructureType(boolean constQualified, boolean volatileQualified,
                                 StructDeclaration structDecl) {
        super(constQualified, volatileQualified, structDecl, BlockType.EXTERNAL_STRUCTURE);
        checkArgument(structDecl.isExternal(), "the structure declaration must be external");
    }

    public ExternalStructureType(StructDeclaration structDecl) {
        this(false, false, structDecl);
    }

    @Override
    public final ExternalStructureType addQualifiers(boolean addConst, boolean addVolatile,
                                                     boolean addRestrict) {
        return new ExternalStructureType(addConstQualifier(addConst),
                    addVolatileQualifier(addVolatile), getDeclaration());
    }

    @Override
    public final ExternalStructureType removeQualifiers(boolean removeConst,
            boolean removeVolatile, boolean removeRestrict) {

        return new ExternalStructureType(removeConstQualifier(removeConst),
                        removeVolatileQualifier(removeVolatile), getDeclaration());
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
