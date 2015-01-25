package pl.edu.mimuw.nesc.type;

import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.declaration.tag.StructDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.BlockElement.BlockType;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Reflects a non-external structure, e.g.
 * <code>struct S { int n; unsigned int p; }</code>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 * @see FieldTagType
 */
public final class StructureType extends FieldTagType<StructDeclaration> {
    /**
     * Initializes this structure type with given parameters.
     *
     * @param structDeclaration Structure declaration that actually depicts
     *                          this structure type.
     * @throws NullPointerException <code>structDeclaration</code> is null.
     * @throws IllegalArgumentException The given structure declaration
     *                                  represents an external structure.
     */
    public StructureType(boolean constQualified, boolean volatileQualified,
                         StructDeclaration structDeclaration) {
        super(constQualified, volatileQualified, structDeclaration, BlockType.STRUCTURE);
        checkArgument(!structDeclaration.isExternal(), "the structure must not be external");
    }

    public StructureType(boolean constQualified, boolean volatileQualified,
                ImmutableList<Field> fields) {
        super(constQualified, volatileQualified, fields, BlockType.STRUCTURE);
    }

    public StructureType(StructDeclaration structDeclaration) {
        this(false, false, structDeclaration);
    }

    public StructureType(ImmutableList<Field> fields) {
        this(false, false, fields);
    }

    @Override
    public final StructureType addQualifiers(boolean addConst, boolean addVolatile,
                                             boolean addRestrict) {
        switch (getVariant()) {
            case ONLY_DECLARATION:
                return new StructureType(addConstQualifier(addConst),
                        addVolatileQualifier(addVolatile), getDeclaration());
            case ONLY_FIELDS:
            case FULL:
                return new StructureType(addConstQualifier(addConst),
                        addVolatileQualifier(addVolatile), getFields());
            default:
                throw new RuntimeException("unexpected variant of a tag type object");
        }
    }

    @Override
    public final StructureType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                                boolean removeRestrict) {
        switch (getVariant()) {
            case ONLY_DECLARATION:
                return new StructureType(removeConstQualifier(removeConst),
                        removeVolatileQualifier(removeVolatile), getDeclaration());
            case ONLY_FIELDS:
            case FULL:
                return new StructureType(removeConstQualifier(removeConst),
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
