package pl.edu.mimuw.nesc.common.util;

import com.google.common.collect.Range;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

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
     * Set the log4j logging level to the given one in all current loggers.
     *
     * @param level Logging level to set.
     */
    public static void setLoggingLevel(Level level) {
        checkNotNull(level, "logging level cannot be null");

        LogManager.getRootLogger().setLevel(level);

        final Enumeration<Logger> loggersEnum = LogManager.getCurrentLoggers();
        while (loggersEnum.hasMoreElements()) {
            loggersEnum.nextElement().setLevel(level);
        }
    }

    /**
     * Create an appender that directs all log messages to a file with
     * given name and replace all current appenders with it.
     *
     * @param fileName Name of the log file to use.
     * @param append Value indicating if messages will be appended to the
     *               given file or it will be truncated.
     */
    public static void logToFile(String fileName, boolean append) throws IOException {
        final Appender newAppender = new FileAppender(new PatternLayout(), fileName, append);

        final List<Logger> loggers = Collections.list(LogManager.getCurrentLoggers());
        loggers.add(LogManager.getRootLogger());

        for (Logger logger : loggers) {
            logger.removeAllAppenders();
            logger.addAppender(newAppender);
        }
    }

    /**
     * Wait for termination of the given process discarding any data from its
     * input stream first. This prevents a possible deadlock.
     *
     * @param process Process to wait for.
     * @return Exit code of the process.
     */
    public static int waitForProcessHandlingIO(Process process)
            throws InterruptedException, IOException {
        checkNotNull(process, "process cannot be null");

        // Read all data from the input stream
        final InputStream input = process.getInputStream();
        int b;
        do {
            b = input.read();
        } while (b != -1);

        // Wait for the process
        return process.waitFor();
    }

    /**
     * Private constructor to prevent this class from being instantiated.
     */
    private VariousUtils() {
    }
}
