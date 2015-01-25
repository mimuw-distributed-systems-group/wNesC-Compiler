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

    /**
     * Endianness of the external type.
     */
    private final Endianness endianness;

    /**
     * Suffix of functions that handle the external type.
     */
    private final String suffix;

    public ExternalScheme(Endianness endianness, String suffix) {
        checkNotNull(endianness, "endianness cannot be null");
        checkNotNull(suffix, "suffix cannot be null");
        checkArgument(!suffix.isEmpty(), "suffix cannot be an empty string");

        this.endianness = endianness;
        this.suffix = suffix;
    }

    /**
     * Get the endianness of values of the external type.
     *
     * @return Endianness for the external type.
     */
    public Endianness getEndianness() {
        return endianness;
    }

    /**
     * Get the suffix of functions that handle the external type.
     *
     * @return The suffix for the external type.
     */
    public String getSuffix() {
        return suffix;
    }

    /**
     * Get the name of the function that is responsible for writing the value
     * to an entity that is not bit-field.
     *
     * @return Name of the function.
     */
    public String getWriteFunctionName() {
        return PREFIX_ASSIGNMENT + suffix;
    }

    /**
     * Get the name of the function responsible for reading the value of the
     * external type to the host representation.
     *
     * @return Name of the function.
     */
    public String getReadFunctionName() {
        return PREFIX_READ + suffix;
    }

    /**
     * Get the name of the function that is responsible for writing the value
     * to a bit-field.
     *
     * @return Name of the function.
     */
    public String getWriteBitFieldFunctionName() {
        return PREFIX_ASSIGNMENT_BITFIELD + suffix;
    }

    /**
     * Get the name of the function that is responsible for reading the value
     * of the external type stored as a bit-field.
     *
     * @return Name of the function.
     */
    public String getReadBitFieldFunctionName() {
        return PREFIX_READ_BITFIELD + suffix;
    }
}
