package pl.edu.mimuw.nesc.abi.typedata;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Structure with basic information about a type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class TypeData {
    private final int size;
    private final int alignment;

    public TypeData(int size, int alignment) {
        checkArgument(size >= 1, "size must be positive");
        checkArgument(alignment >= 1, "alignment must be positive");

        this.size = size;
        this.alignment = alignment;
    }

    /**
     * Get the size of the type in bytes.
     *
     * @return Size of the type in bytes.
     */
    public int getSize() {
        return size;
    }

    /**
     * Get the alignment of the type in bytes.
     *
     * @return Alignment of the type in bytes.
     */
    public int getAlignment() {
        return alignment;
    }
}
