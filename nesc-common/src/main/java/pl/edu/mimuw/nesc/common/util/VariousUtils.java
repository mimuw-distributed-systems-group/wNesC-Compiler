package pl.edu.mimuw.nesc.common.util;

/**
 * <p>Class with various utility methods that cannot be currently located in
 * a dedicated package.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class VariousUtils {
    /**
     * Get the value of a <code>Boolean</code> object treating its
     * <code>null</code> value as <code>false</code>.
     *
     * @param booleanValue Boolean object with value to retrieve.
     * @return <code>true</code> if and only if the given object is not
     *         <code>null</code> and represents <code>true</code> value.
     */
    public static boolean getBooleanValue(Boolean booleanValue) {
        return Boolean.TRUE.equals(booleanValue);
    }

    /**
     * Private constructor to prevent this class from being instantiated.
     */
    private VariousUtils() {
    }
}
