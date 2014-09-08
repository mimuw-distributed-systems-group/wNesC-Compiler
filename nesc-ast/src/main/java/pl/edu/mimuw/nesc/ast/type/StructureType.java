package pl.edu.mimuw.nesc.ast.type;

import pl.edu.mimuw.nesc.declaration.tag.StructDeclaration;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Reflects a non-external structure, e.g.
 * <code>struct S { int n; unsigned int p; }</code>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
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
        super(constQualified, volatileQualified, structDeclaration);
        checkArgument(!structDeclaration.isExternal(), "the structure must not be external");
    }

    public StructureType(StructDeclaration structDeclaration) {
        this(false, false, structDeclaration);
    }

    @Override
    public final StructureType addQualifiers(boolean addConst, boolean addVolatile,
                                             boolean addRestrict) {
        return new StructureType(addConstQualifier(addConst),
                addVolatileQualifier(addVolatile), getDeclaration());
    }

    @Override
    public final StructureType removeQualifiers(boolean removeConst, boolean removeVolatile,
                                                boolean removeRestrict) {
        return new StructureType(removeConstQualifier(removeConst),
                removeVolatileQualifier(removeVolatile), getDeclaration());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
