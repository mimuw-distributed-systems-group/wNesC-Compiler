package pl.edu.mimuw.nesc.ast.type;

import pl.edu.mimuw.nesc.declaration.tag.StructDeclaration;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Reflects an external structure type, e.g.
 * <code>nx_struct { nx_int32_t n; nx_int8_t c; }</code>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
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
        super(constQualified, volatileQualified, structDecl);
        checkArgument(structDecl.isExternal(), "the structure declaration must be external");
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
