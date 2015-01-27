package pl.edu.mimuw.nesc.declaration.tag.fieldtree;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import pl.edu.mimuw.nesc.declaration.CopyController;
import pl.edu.mimuw.nesc.declaration.tag.FieldTagDeclaration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class BlockElement extends TreeElement {
    /**
     * Elements that are contained in this block element. Never null.
     */
    private final List<TreeElement> children;

    /**
     * Type of this block. Never null.
     */
    private final BlockType type;

    /**
     * Tag declaration object created for this block element.
     */
    private final FieldTagDeclaration<?> declaration;

    /**
     * Initializes this object with given children elements. The list is not
     * copied.
     *
     * @throws NullPointerException One of the arguments is null.
     * @throws IllegalArgumentException One of the elements of the given list
     *                                  is null.
     */
    public BlockElement(List<TreeElement> children, BlockType type, FieldTagDeclaration<?> declaration) {
        checkNotNull(children, "children elements cannot be null");
        checkNotNull(type, "type of the block element cannot be null");
        checkNotNull(declaration, "declaration cannot be null");
        for (TreeElement element : children) {
            checkArgument(element != null, "a child tree element cannot be null");
        }

        this.children = children;
        this.type = type;
        this.declaration = declaration;
    }

    public List<TreeElement> getChildren() {
        return children;
    }

    public BlockType getType() {
        return type;
    }

    public FieldTagDeclaration<?> getDeclaration() {
        return declaration;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    @Override
    public BlockElement deepCopy(CopyController controller) {
        final ImmutableList.Builder<TreeElement> childrenBuilder = ImmutableList.builder();

        for (TreeElement child : this.children) {
            childrenBuilder.add(child.deepCopy(controller));
        }

        return new BlockElement(childrenBuilder.build(), this.type,
                (FieldTagDeclaration<?>) controller.copy(this.declaration));
    }

    public enum BlockType {
        STRUCTURE,
        UNION,
        EXTERNAL_STRUCTURE,
        EXTERNAL_UNION,
    }
}
