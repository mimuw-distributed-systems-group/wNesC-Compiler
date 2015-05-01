package pl.edu.mimuw.nesc.backend8051.option;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.cli.CommandLine;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Object that is responsible for checking if the options given to the
 * validator are correctly specified.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class Options8051Validator {
    /**
     * Regular expression whose language are valid identifiers in C.
     */
    private static final Pattern REGEXP_IDENTIFIER = Pattern.compile("[a-zA-Z_]\\w*");

    /**
     * Regular expression that defines the language for a single element in the
     * interrupts map.
     */
    private static final Pattern REGEXP_INTERRUPT_ASSIGNMENT =
            Pattern.compile("(?<functionName>" + REGEXP_IDENTIFIER + ")"
                    + Options8051.SEPARATOR_INTERRUPT_ASSIGNMENT_INNER
                    + "(?<interruptNumber>\\d+)");

    /**
     * Object that represents parsed options.
     */
    private final CommandLine cmdLine;

    /**
     * Instances of all single validators that will check the options.
     */
    private final ImmutableList<SingleValidator> validationChain;

    Options8051Validator(CommandLine cmdLine) {
        checkNotNull(cmdLine, "command line cannot be null");
        this.cmdLine = cmdLine;
        this.validationChain = ImmutableList.of(
                new BankSizeValidator(),
                new BanksCountValidator(),
                new EstimateThreadsCountValidator(),
                new SDCCExecutableValidator(),
                new DumpCallGraphValidator(),
                new InterruptsValidator()
        );
    }

    /**
     * Check if the options given at construction are correct.
     *
     * @return The object is absent if the validation succeeds. Otherwise, it
     *         is present and it contains the error message.
     */
    public Optional<String> validate() {
        for (SingleValidator featureValidator : validationChain) {
            final Optional<String> error = featureValidator.validate();
            if (error.isPresent()) {
                return error;
            }
        }
        return Optional.absent();
    }

    private Optional<String> getOptionValue(String optionName) {
        return Optional.fromNullable(cmdLine.getOptionValue(optionName));
    }

    private Optional<String> checkPositiveInteger(Optional<String> value, String optionText) {
        if (!value.isPresent()) {
            return Optional.absent();
        }

        final int intValue;
        try {
            intValue = Integer.parseInt(value.get());
        } catch (NumberFormatException e) {
            return Optional.of("'" + value.get() + "' is not a valid integer");
        }

        return intValue <= 0
                ? Optional.of(optionText + " must be positive")
                : Optional.<String>absent();
    }

    private Optional<String> checkNonEmptyString(Optional<String> value, String optionText) {
        return value.isPresent() && value.get().isEmpty()
                ? Optional.of(optionText + " cannot be empty")
                : Optional.<String>absent();
    }

    /**
     * Interface for validation of a single option.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private interface SingleValidator {
        /**
         * Validate the option of this validator. The value of the option should
         * be taken directly from {@link Options8051Validator#cmdLine} member.
         *
         * @return Error message if the option is invalid. Otherwise, the object
         *         should be absent.
         */
        Optional<String> validate();
    }

    private final class BankSizeValidator implements SingleValidator {
        @Override
        public Optional<String> validate() {
            return checkPositiveInteger(getOptionValue(Options8051.OPTION_LONG_BANK_SIZE),
                    "the bank size");
        }
    }

    private final class BanksCountValidator implements SingleValidator {
        @Override
        public Optional<String> validate() {
            return checkPositiveInteger(getOptionValue(Options8051.OPTION_LONG_BANKS_COUNT),
                    "banks count");
        }
    }

    private final class DumpCallGraphValidator implements SingleValidator {
        @Override
        public Optional<String> validate() {
            return checkNonEmptyString(getOptionValue(Options8051.OPTION_LONG_DUMP_CALL_GRAPH),
                    "name of file for the call graph");
        }
    }

    private final class EstimateThreadsCountValidator implements SingleValidator {
        @Override
        public Optional<String> validate() {
            return checkPositiveInteger(getOptionValue(Options8051.OPTION_LONG_THREADS_COUNT),
                    "count of estimate threads");
        }
    }

    private final class SDCCExecutableValidator implements SingleValidator {
        @Override
        public Optional<String> validate() {
            return checkNonEmptyString(getOptionValue(Options8051.OPTION_LONG_SDCC_EXEC),
                    "SDCC executable");
        }
    }

    private final class InterruptsValidator implements SingleValidator {
        @Override
        public Optional<String> validate() {
            final Optional<String> interrupts = getOptionValue(Options8051.OPTION_LONG_INTERRUPTS);
            if (!interrupts.isPresent()) {
                return Optional.absent();
            }

            final BigInteger maxInt = BigInteger.valueOf(Integer.MAX_VALUE);
            final Map<Integer, String> usedNumbers = new HashMap<>();
            final String[] assignments = interrupts.get()
                    .split(Options8051.SEPARATOR_INTERRUPT_ASSIGNMENT_OUTER, -1);

            for (String assignment : assignments) {
                final Matcher matcher = REGEXP_INTERRUPT_ASSIGNMENT.matcher(assignment);

                if (matcher.matches()) {
                    final BigInteger value = new BigInteger(matcher.group("interruptNumber"));
                    final String functionName = matcher.group("functionName");

                    if (value.compareTo(maxInt) > 0) {
                        return Optional.of("invalid value for option '--" + Options8051.OPTION_LONG_INTERRUPTS
                                + "': number " + matcher.group("interruptNumber") + " exceeds " + maxInt);
                    }

                    if (usedNumbers.containsKey(value.intValue())
                            && !usedNumbers.get(value.intValue()).equals(functionName)) {
                        return Optional.of("invalid value for option '--" + Options8051.OPTION_LONG_INTERRUPTS
                                + "': multiple functions ('" + usedNumbers.get(value.intValue())
                                + "', '" + functionName + "') assigned to interrupt "
                                + matcher.group("interruptNumber"));
                    }

                    usedNumbers.put(value.intValue(), functionName);
                } else {
                    return Optional.of("invalid value for option '--" + Options8051.OPTION_LONG_INTERRUPTS
                            + "': '" + assignment + "' is an invalid assignment of function to interrupt");
                }
            }

            return Optional.absent();
        }
    }
}
