package pl.edu.mimuw.nesc.attribute;

/**
 * <p>Class that contains useful methods that are related to attributes.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class Attributes {
    /**
     * Names of NesC attributes.
     */
    private static final String ATTRIBUTE_NAME_C = "C";
    private static final String ATTRIBUTE_NAME_SPONTANEOUS = "spontaneous";
    private static final String ATTRIBUTE_NAME_HWEVENT = "hwevent";
    private static final String ATTRIBUTE_NAME_ATOMIC_HWEVENT = "atomic_hwevent";

    /**
     * Names of GCC attributes.
     */
    private static final String ATTRIBUTE_NAME_PACKED = "packed";
    private static final String ATTRIBUTE_NAME_INTERRUPT = "interrupt";
    private static final String ATTRIBUTE_NAME_SIGNAL = "signal";

    public static String getCAttributeName() {
        return ATTRIBUTE_NAME_C;
    }

    public static String getSpontaneousAttributeName() {
        return ATTRIBUTE_NAME_SPONTANEOUS;
    }

    public static String getHweventAttributeName() {
        return ATTRIBUTE_NAME_HWEVENT;
    }

    public static String getAtomicHweventAttributeName() {
        return ATTRIBUTE_NAME_ATOMIC_HWEVENT;
    }

    public static String getPackedAttributeName() {
        return ATTRIBUTE_NAME_PACKED;
    }

    public static String getInterruptAttributeName() {
        return ATTRIBUTE_NAME_INTERRUPT;
    }

    public static String getSignalAttributeName() {
        return ATTRIBUTE_NAME_SIGNAL;
    }

    /**
     * Private constructor to prevent this class from being instantiated.
     */
    private Attributes() {
    }
}
