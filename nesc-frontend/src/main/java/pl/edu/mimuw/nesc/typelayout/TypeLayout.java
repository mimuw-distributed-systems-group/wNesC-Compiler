package pl.edu.mimuw.nesc.typelayout;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Base class that represents a layout of data types.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class TypeLayout {
    private final int size;
    private final int alignment;

    TypeLayout(int size, int alignment) {
        checkArgument(size > 0, "size cannot be non-positive");
        checkArgument(alignment > 0, "the alignment cannot be non-positive");
        this.size = size;
        this.alignment = alignment;
    }

    /**
     * Get the size of the type.
     *
     * @return Size of the type (in bytes).
     */
    public int getSize() {
        return size;
    }

    /**
     * Get the alignment of the type.
     *
     * @return Alignment of the type (in bytes).
     */
    public int getAlignment() {
        return alignment;
    }
}
