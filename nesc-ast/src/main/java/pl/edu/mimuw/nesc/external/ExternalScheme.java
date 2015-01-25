package pl.edu.mimuw.nesc.external;

import pl.edu.mimuw.nesc.abi.Endianness;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class that contains information about an external scheme. An external
 * scheme is always related to an external base type and depicts the way of
 * decoding and encoding values.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ExternalScheme {
    /**
     * Constants for generation of function used to implement external types.
     */
    private static final String PREFIX_ASSIGNMENT = "__nesc_hton_";
    private static final String PREFIX_READ = "__nesc_ntoh_";
    private static final String PREFIX_ASSIGNMENT_BITFIELD = "__nesc_htonbf_";
    private static final String PREFIX_READ_BITFIELD = "__nesc_ntohbf_";
    private static final String MARK_LITTLE_ENDIAN = "le";
    private static final String MARK_BIG_ENDIAN = "";
    private static final String MARK_SIGNED = "int";
    private static final String MARK_UNSIGNED = "uint";

    /**
     * Endianness of the external type.
     */
    private final Endianness endianness;

    /**
     * Number of bits of the representation of the external base type.
     */
    private final int bitsCount;

    /**
     * Value indicating if the external base type is unsigned.
     */
    private final boolean isUnsigned;

    public ExternalScheme(Endianness endianness, int bytesCount, boolean isUnsigned) {
        checkNotNull(endianness, "endianness cannot be null");
        checkArgument(bytesCount >= 1, "count of bytes cannot be non-positive");

        this.endianness = endianness;
        this.bitsCount = bytesCount * 8;
        this.isUnsigned = isUnsigned;
    }

    public Endianness getEndianness() {
        return endianness;
    }

    public int getBitsCount() {
        return bitsCount;
    }

    public boolean isUnsigned() {
        return isUnsigned;
    }

    /**
     * Get the name of the function that is responsible for writing the value
     * to an entity that is not bit-field.
     *
     * @return Name of the function.
     */
    public String getWriteFunctionName() {
        return buildFunctionName(PREFIX_ASSIGNMENT);
    }

    /**
     * Get the name of the function responsible for reading the value of the
     * external type to the host representation.
     *
     * @return Name of the function.
     */
    public String getReadFunctionName() {
        return buildFunctionName(PREFIX_READ);
    }

    /**
     * Get the name of the function that is responsible for writing the value
     * to a bit-field.
     *
     * @return Name of the function.
     */
    public String getWriteBitFieldFunctionName() {
        return buildFunctionName(PREFIX_ASSIGNMENT_BITFIELD);
    }

    /**
     * Get the name of the function that is responsible for reading the value
     * of the external type stored as a bit-field.
     *
     * @return Name of the function.
     */
    public String getReadBitFieldFunctionName() {
        return buildFunctionName(PREFIX_READ_BITFIELD);
    }

    private String buildFunctionName(String prefix) {
        final StringBuilder builder = new StringBuilder();
        builder.append(prefix);

        // Endianness
        final String endiannessStr;
        switch (endianness) {
            case LITTLE_ENDIAN:
                endiannessStr = MARK_LITTLE_ENDIAN;
                break;
            case BIG_ENDIAN:
                endiannessStr = MARK_BIG_ENDIAN;
                break;
            default:
                throw new RuntimeException("unexpected endianness '" + endianness + "'");
        }
        builder.append(endiannessStr);

        // Integer type
        final String typeStr = isUnsigned
                ? MARK_UNSIGNED
                : MARK_SIGNED;
        builder.append(typeStr);

        // Bits count
        builder.append(bitsCount);

        return builder.toString();
    }
}
