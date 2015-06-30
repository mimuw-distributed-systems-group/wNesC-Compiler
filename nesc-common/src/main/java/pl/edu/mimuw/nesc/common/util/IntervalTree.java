package pl.edu.mimuw.nesc.common.util;

import java.lang.reflect.Array;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of an interval tree that allows finding the sum of elements
 * enclosed in a given interval in logarithmic time.
 *
 * @param <T> Type of elements contained in the tree.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class IntervalTree<T> {
    /**
     * All nodes of the whole tree.
     */
    private final T[] tree;

    /**
     * Operation performed by this tree.
     */
    private final IntervalTreeOperation<T> operation;

    /**
     * Count of valid keys in the tree.
     */
    private final int size;

    /**
     * Index of the element in the array associated with key 0.
     */
    private final int leavesOffset;

    /**
     * Create an interval tree that contains at least given count of elements.
     * All elements are initialized to zero.
     *
     * @param classObj Object for the class of elements that this interval tree
     *                 will contain.
     * @param operation Operation that will be performed by this tree.
     * @param elementsCount Count of elements that the constructed tree will be
     *                      able to hold - the greatest key in the tree will be
     *                      equal to <code>elementsCount - 1</code>.
     */
    @SuppressWarnings("unchecked")
    public IntervalTree(Class<T> classObj, IntervalTreeOperation<T> operation,
            int elementsCount) {
        checkNotNull(classObj, "the class object cannot be null");
        checkNotNull(operation, "operation cannot be null");
        checkArgument(elementsCount >= 1, "count of elements must be positive");

        // Compute the count of all leaves in the tree
        long leavesCount = elementsCount;
        --leavesCount;
        leavesCount |= leavesCount >>> 1;
        leavesCount |= leavesCount >>> 2;
        leavesCount |= leavesCount >>> 4;
        leavesCount |= leavesCount >>> 8;
        leavesCount |= leavesCount >>> 16;
        ++leavesCount;

        if (2 * leavesCount - 1 > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("count of elements of the tree is too big");
        }

        this.size = (int) leavesCount;
        this.leavesOffset = (int) leavesCount - 1;
        this.tree = (T[]) Array.newInstance(classObj, this.leavesOffset + this.size);
        this.operation = operation;
        Arrays.fill(this.tree, operation.getNeutralElement());
    }

    /**
     * Get the count of elements in the tree. It never changes during the
     * lifetime of an interval tree.
     *
     * @return Count of elements in this tree.
     */
    public int size() {
        return size;
    }

    /**
     * Associate the element with given index with given value. This method runs
     * in O(log(<code>size()</code>)) time.
     *
     * @param index Index of the element whose value will be set.
     * @param value Value that will be set for the element with given index.
     */
    public void set(int index, T value) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("given index " + index
                    + " is out of range [0, " + (size - 1) + "]");
        }

        index = this.leavesOffset + index + 1;
        int parentIndex = index / 2;
        this.tree[index - 1] = value;

        // Update the tree
        while (index != 1) {
            index = index - index % 2;
            this.tree[parentIndex - 1] = operation.perform(this.tree[index - 1],
                    this.tree[index]);
            index = parentIndex;
            parentIndex = index / 2;
        }
    }

    /**
     * Compute the result of the operation for elements associated with indices
     * from <code>startIndex</code> (inclusive) and <code>endIndex</code>
     * (exclusive). This method runs in O(log(<code>size()</code>)) time.
     *
     * @param startIndex Index of the first element for the operation.
     * @param endIndex Index after the last element that will be given to the
     *                 operation.
     * @return Result of the operation of this tree for elements with indices
     *         from <code>startIndex</code> to <code>endIndex - 1</code>.
     */
    public T compute(int startIndex, int endIndex) {
        if (startIndex >= endIndex || startIndex < 0 || endIndex > size()) {
            throw new IndexOutOfBoundsException("interval [" + startIndex + ", "
                    + endIndex + "] is invalid");
        }

        int leftIndex = this.leavesOffset + startIndex + 1;
        int rightIndex = this.leavesOffset + endIndex;

        if (leftIndex == rightIndex) {
            return this.tree[leftIndex - 1];
        }

        int leftParentIndex = leftIndex / 2;
        int rightParentIndex = rightIndex / 2;
        T result = operation.perform(this.tree[leftIndex - 1], this.tree[rightIndex - 1]);

        while (leftParentIndex != rightParentIndex) {
            if (leftIndex % 2 == 0) {
                result = operation.perform(result, this.tree[leftIndex]);
            }
            if (rightIndex % 2 == 1) {
                result = operation.perform(result, this.tree[rightIndex - 2]);
            }
            leftIndex = leftParentIndex;
            rightIndex = rightParentIndex;
            leftParentIndex = leftParentIndex / 2;
            rightParentIndex = rightParentIndex / 2;
        }

        return result;
    }
}
