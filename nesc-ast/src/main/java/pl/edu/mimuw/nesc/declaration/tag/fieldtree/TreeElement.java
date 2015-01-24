package pl.edu.mimuw.nesc.declaration.tag.fieldtree;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.declaration.CopyController;
import pl.edu.mimuw.nesc.declaration.tag.FieldDeclaration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;

/**
 * A class that represents a single element of the fields tree. Consider the
 * example:
 *
 * <pre>
 *     struct S
 *     {
 *         int a;
 *
 *         union
 *         {
 *             float f;
 *             unsigned long long l;
 *         };
 *
 *         char c;
 *         int flex[];
 *     };
 * </pre>
 *
 * <code>a</code>, <code>c</code> and <code>flex</code> will be represented by
 * <code>FieldElement</code> objects because they are not enclosed by any block
 * in the structure definition. However, the nested anonymous union will be
 * represented by a <code>BlockElement</code> object that will contain fields
 * <code>f</code> and <code>l</code>.
 *
 * <p>This class implements <code>Iterable</code> interface. The elements are
 * returned by the iterator in the order they were declared in a program.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class TreeElement implements Iterable<FieldDeclaration> {
    @Override
    public Iterator<FieldDeclaration> iterator() {
        return new FieldDeclarationIterator();
    }

    /**
     * Calls <code>visitor.visit(this, arg);</code> as in the Visitor design
     * pattern.
     *
     * @return Value returned by the call <code>visitor.visit(this, arg)</code>.
     */
    public abstract <R, A> R accept(Visitor<R, A> visitor, A arg);

    /**
     * Copies this node and all its children and the field declarations using
     * the given copy controller.
     *
     * @param controller Controller to make the copy with.
     * @return Newly created instance of a proper tree element (of the same
     *         class as this).
     */
    public abstract TreeElement deepCopy(CopyController controller);

    public interface Visitor<R, A> {
        R visit(FieldElement fieldElement, A arg);
        R visit(BlockElement blockElement, A arg);
    }

    /**
     * An iterator class that allows traversing the fields from this element.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private class FieldDeclarationIterator implements Iterator<FieldDeclaration> {
        /**
         * The value is absent if it has not been checked yet if the next
         * element is present. Otherwise, it contains such information.
         */
        private Optional<Optional<FieldDeclaration>> next = Optional.absent();

        /**
         * Stack that contains the current path in the tree of the blocks.
         */
        private final Stack<Iterator<TreeElement>> stack = new Stack<>();

        /**
         * <code>true</code> if and only if the iteration has been already
         * started.
         */
        private boolean started = false;

        /**
         * Visitor that is used to traverse the structure.
         */
        private final Visitor<Void, Void> traverseVisitor = new Visitor<Void, Void>() {
            @Override
            public Void visit(FieldElement fieldElement, Void arg) {
                next = Optional.of(Optional.of(fieldElement.getFieldDeclaration()));
                return null;
            }

            @Override
            public Void visit(BlockElement blockElement, Void arg) {
                stack.push(blockElement.getChildren().iterator());
                return null;
            }
        };

        @Override
        public final boolean hasNext() {
            traverse();
            return next.get().isPresent();
        }

        @Override
        public final FieldDeclaration next() {
            traverse();

            if (!next.get().isPresent()) {
                throw new NoSuchElementException("there are no elements to iterate over");
            }
            final FieldDeclaration result = next.get().get();
            next = Optional.absent();

            return result;
        }

        @Override
        public final void remove() {
            throw new UnsupportedOperationException("a field declaration iterator cannot remove any declaration");
        }

        private void traverse() {
            if (!started) {
                TreeElement.this.accept(traverseVisitor, null);
                started = true;
            }

            if (next.isPresent()) {
                return;
            }

            while (!stack.isEmpty() && !next.isPresent()) {
                final Iterator<TreeElement> top = stack.peek();
                if (top.hasNext()) {
                    top.next().accept(traverseVisitor, null);
                } else {
                    stack.pop();
                }
            }

            if (!next.isPresent()) {
                next = Optional.of(Optional.<FieldDeclaration>absent());
            }
        }
    }
}
