package pl.edu.mimuw.nesc.analysis.type;

import pl.edu.mimuw.nesc.declaration.tag.UnionDeclaration;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Reflects a non-external union type, e.g.
 *  <code>union U { signed i; float f; };</code>.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UnionType extends MaybeExternalTagType<UnionDeclaration> {
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
        super(constQualified, volatileQualified, unionDecl);
        checkArgument(!unionDecl.isExternal(), "the union type cannot be external");
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
