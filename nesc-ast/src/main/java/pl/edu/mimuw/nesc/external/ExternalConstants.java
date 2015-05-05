package pl.edu.mimuw.nesc.external;

/**
 * Class with constants related to external types.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ExternalConstants {
    /**
     * Macros that handle external tags.
     */
    private static final String EXTERNAL_DEFINES = "#define nx_struct struct\n#define nx_union union\n\n";

    /**
     * Get text that contains definitions of macros that translate keywords
     * <code>nx_struct</code> and <code>nx_union</code> to <code>struct</code>
     * and <code>union</code> respectively.
     *
     * @return Text with the macros definitions.
     */
    public static String getExternalDefines() {
        return EXTERNAL_DEFINES;
    }

    /**
     * Private constructor to prevent this class from being instantiated.
     */
    private ExternalConstants() {
    }
}
