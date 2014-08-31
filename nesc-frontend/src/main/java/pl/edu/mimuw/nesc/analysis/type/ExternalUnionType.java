package pl.edu.mimuw.nesc.analysis.type;

import pl.edu.mimuw.nesc.declaration.tag.UnionDeclaration;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Reflects an external union type, e.g.
 * <code>nx_union { nx_int32_t u[10]; };</code>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ExternalUnionType extends MaybeExternalTagType<UnionDeclaration> {
    public ExternalUnionType(boolean constQualified, boolean volatileQualified,
                             UnionDeclaration unionDecl) {
        super(constQualified, volatileQualified, unionDecl);
        checkArgument(unionDecl.isExternal(), "the union must be external");
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
