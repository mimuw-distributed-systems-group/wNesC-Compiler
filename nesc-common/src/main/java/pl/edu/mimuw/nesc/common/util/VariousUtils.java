package pl.edu.mimuw.nesc.common.util;

import com.google.common.collect.Range;
import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

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
     * Calculate the smallest number greater or equal to the given one that is
     * a multiple of the given alignment.
     *
     * @param n Number to align.
     * @param alignment The alignment of the number.
     * @return Number after alignment.
     * @throws IllegalArgumentException The number to align is negative or
     *                                  the alignment is not positive.
     */
    public static int alignNumber(int n, int alignment) {
        checkArgument(n >= 0, "cannot align a negative number");
        checkArgument(alignment >= 1, "cannot align a non-positive number");
        return alignment * ((n + alignment - 1) / alignment);
    }

    /**
     * Compute the greatest common divisor of the given numbers.
     *
     * @return The greatest common divisor of the given numbers.
     * @throws IllegalArgumentException <code>n</code> or <code>m</code> is not
     *                                  positive.
     */
    public static int gcd(int n, int m) {
        checkArgument(n >= 1, "computing GCD of a non-positive number");
        checkArgument(m >= 1, "computing GCD of a non-positive number");
        return BigInteger.valueOf(n).gcd(BigInteger.valueOf(m)).intValue();
    }

    /**
     * Compute the least common multiple of the given numbers.
     *
     * @return The least common multiple of the given numbers.
     * @throws IllegalArgumentException <code>n</code> or <code>m</code> is not
     *                                  positive.
     */
    public static int lcm(int n, int m) {
        return (n * m) / gcd(n, m);
    }

    /**
     * Check if the given range contains numbers from all the remaining
     * parameters.
     *
     * @param range Range of numbers.
     * @param firstNum First number to check if it is inside the given range.
     * @param numbers Other numbers to check if they are contained in the given
     *                range.
     * @return <code>true</code> if and only if all the given numbers are
     *         contained in the given range.
     */
    public static boolean rangeContainsAll(Range<BigInteger> range, BigInteger firstNum, BigInteger... numbers) {
        checkNotNull(range, "range cannot be null");
        checkNotNull(firstNum, "the first number cannot be null");

        if (!range.contains(firstNum)) {
            return false;
        }

        for (BigInteger number : numbers) {
            if (!range.contains(number)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Private constructor to prevent this class from being instantiated.
     */
    private VariousUtils() {
    }
}
