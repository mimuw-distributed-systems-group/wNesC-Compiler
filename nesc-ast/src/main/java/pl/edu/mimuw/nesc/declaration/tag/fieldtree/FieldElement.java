package pl.edu.mimuw.nesc.declaration.tag.fieldtree;

import pl.edu.mimuw.nesc.declaration.CopyController;
import pl.edu.mimuw.nesc.declaration.tag.FieldDeclaration;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class FieldElement extends TreeElement {
    /**
     * Field declaration that corresponds to this field element. Never null.
     */
    private final FieldDeclaration fieldDeclaration;

    /**
     * Initializes this element with given declaration.
     *
     * @throws NullPointerException Given argument is null.
     */
    public FieldElement(FieldDeclaration fieldDeclaration) {
        checkNotNull(fieldDeclaration, "field declaration cannot be null");
        this.fieldDeclaration = fieldDeclaration;
    }

    public FieldDeclaration getFieldDeclaration() {
        return fieldDeclaration;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    @Override
    public FieldElement deepCopy(CopyController controller) {
        return new FieldElement(controller.copy(this.fieldDeclaration));
    }
}
