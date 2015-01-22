package pl.edu.mimuw.nesc.abi.typedata;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * <p>Data about field tag types, e.g. a structure or union.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class FieldTagTypeData {
    /**
     * Value indicating if the type of a bit-field affects the alignment of the
     * whole tag type.
     */
    private final boolean bitFieldTypeMatters;

    /**
     * Minimum alignment of a zero-width bit-field (in bits).
     */
    private final int emptyBitFieldAlignmentInBits;

    /**
     * Minimum alignment of the whole structure (in bits).
     */
    private final int minimumAlignment;

    public FieldTagTypeData(boolean bitFieldTypeMatters, int emptyBitFieldAlignmentInBits,
            int minimumAlignment) {
        checkArgument(emptyBitFieldAlignmentInBits >= 1, "alignment of the zero-width bit-field cannot be non-positive");
        checkArgument(minimumAlignment >= 1, "minimum alignment of the whole tag type cannot be non-positive");

        this.bitFieldTypeMatters = bitFieldTypeMatters;
        this.emptyBitFieldAlignmentInBits = emptyBitFieldAlignmentInBits;
        this.minimumAlignment = minimumAlignment;
    }

    /**
     * Check if the type of a bit-field affects the alignment of the whole tag
     * type.
     *
     * @return <code>true</code> if and only if the type of a bit-field affects
     *         the alignment of the whole tag type.
     */
    public boolean bitFieldTypeMatters() {
        return bitFieldTypeMatters;
    }

    /**
     * Get the minimum alignment of a zero-width bit-field in bits.
     *
     * @return Alignment of a zero-width bit-field in bits.
     */
    public int getEmptyBitFieldAlignmentInBits() {
        return emptyBitFieldAlignmentInBits;
    }

    /**
     * Get the minimum alignment of the whole tag type in bytes.
     *
     * @return Minimum alignment of the whole tag type in bytes.
     */
    public int getMinimumAlignment() {
        return minimumAlignment;
    }
}
