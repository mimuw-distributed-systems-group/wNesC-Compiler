package pl.edu.mimuw.nesc.attribute;

/**
 * <p>Class that contains useful methods that are related to attributes.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class Attributes {
    /**
     * Names of attributes.
     */
    private static final String ATTRIBUTE_NAME_C = "C";
    private static final String ATTRIBUTE_NAME_SPONTANEOUS = "spontaneous";
    private static final String ATTRIBUTE_NAME_HWEVENT = "hwevent";
    private static final String ATTRIBUTE_NAME_ATOMIC_HWEVENT = "atomic_hwevent";

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

    /**
     * Private constructor to prevent this class from being instantiated.
     */
    private Attributes() {
    }
}
