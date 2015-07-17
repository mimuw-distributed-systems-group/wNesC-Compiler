package pl.edu.mimuw.nesc.common.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of the disjoint-set data structure.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class FindUnionSet<T> {
    /**
     * All elements of the find-union data structure.
     */
    private final ImmutableMap<T, Node<T>> elements;

    /**
     * Set that contains current representatives of all sets.
     */
    private final Set<T> allRepresentatives;

    /**
     * Unmodifiable view of the set of representatives.
     */
    private final Set<T> unmodifiableAllRepresentatives;

    /**
     * Create a new find-union set. It will contain the given elements in
     * separate sets each.
     *
     * @param elements Elements that will be located in single sets directly
     *                 after creation of this find-union set.
     */
    public FindUnionSet(Set<T> elements) {
        checkNotNull(elements, "elements cannot be null");

        // Elements
        final ImmutableMap.Builder<T, Node<T>> elementsBuilder =
                ImmutableMap.builder();
        for (T element : elements) {
            elementsBuilder.put(element, new Node<>(element));
        }
        this.elements = elementsBuilder.build();

        // Representatives
        this.allRepresentatives = new HashSet<>(elements);
        this.unmodifiableAllRepresentatives = Collections.unmodifiableSet(this.allRepresentatives);
    }

    /**
     * Find the representative of the set that contains the given element.
     *
     * @param element Element to find the representative for.
     * @return The representative of the set with given element.
     */
    public T find(T element) {
        checkNotNull(element, "element cannot be null");
        checkArgument(elements.containsKey(element), "this find-union set does not contain given element");

        // Find the representative
        Node<T> currentNode = elements.get(element);
        while (currentNode != currentNode.getParent()) {
            currentNode = currentNode.getParent();
        }

        // Perform the path compression optimization
        Node<T> updatedNode = elements.get(element);
        while (updatedNode != updatedNode.getParent()) {
            final Node<T> nextNode = updatedNode.getParent();
            updatedNode.setParent(currentNode);
            updatedNode = nextNode;
        }

        return currentNode.getElement();
    }

    /**
     * Merge the set that contains the first element with the one that contains
     * the second element. Total count of sets is reduced by at most one. It is
     * not reduced if the two elements are from the same set.
     *
     * @param element1 Element that indicates the first set to merge.
     * @param element2 Element that indicates the second set to merge.
     */
    public void union(T element1, T element2) {
        checkNotNull(element1, "the first element cannot be null");
        checkNotNull(element2, "the second element cannot be null");
        checkArgument(elements.containsKey(element1), "the first element is not contained in this find-union set");
        checkArgument(elements.containsKey(element2), "the second element is not contained in this find-union set");

        final Node<T> root1 = elements.get(find(element1));
        final Node<T> root2 = elements.get(find(element2));

        if (root1 == root2) {
            return;
        }

        if (root1.getRank() > root2.getRank()) {
            root2.setParent(root1);
            allRepresentatives.remove(root2.getElement());
        } else if (root2.getRank() > root1.getRank()) {
            root1.setParent(root2);
            allRepresentatives.remove(root1.getElement());
        } else {
            root1.setParent(root2);
            root2.incrementRank();
            allRepresentatives.remove(root1.getElement());
        }
    }

    /**
     * Get an unmodifiable view of the set with current representatives of all
     * sets in this find-union set.
     *
     * @return Unmodifiable set with all representatives.
     */
    public Set<T> getAllRepresentatives() {
        return unmodifiableAllRepresentatives;
    }

    /**
     * Get set with all elements stored in this find-union set.
     *
     * @return Immutable set with all elements of this find-union set.
     */
    public ImmutableSet<T> getAllElements() {
        return elements.keySet();
    }

    /**
     * Get the size of this find-union set, i.e. the count of sets it contains.
     *
     * @return Count of sets contained in this find-union set.
     */
    public int size() {
        return getAllRepresentatives().size();
    }

    /**
     * Node of a find-union tree.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class Node<T> {
        private final T element;
        private int rank;
        private Node<T> parent;

        private Node(T element) {
            checkNotNull(element, "element cannot be null");
            this.element = element;
            this.rank = 0;
            this.parent = this;
        }

        private T getElement() {
            return element;
        }

        private int getRank() {
            return rank;
        }

        private void incrementRank() {
            ++rank;
        }

        private Node<T> getParent() {
            return parent;
        }

        private void setParent(Node<T> parent) {
            checkNotNull(parent, "parent cannot be null");
            this.parent = parent;
        }
    }
}
