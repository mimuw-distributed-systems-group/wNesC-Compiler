package pl.edu.mimuw.nesc.backend8051.option;

import com.google.common.base.Optional;
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
     * Object that represents parsed options.
     */
    private final CommandLine cmdLine;

    Options8051Validator(CommandLine cmdLine) {
        checkNotNull(cmdLine, "command line cannot be null");
        this.cmdLine = cmdLine;
    }

    /**
     * Check if the options given at construction are correct.
     *
     * @return The object is absent if the validation succeeds. Otherwise, it
     *         is present and it contains the error message.
     */
    public Optional<String> validate() {
        Optional<String> error = validateBankSize();
        if (error.isPresent()) {
            return error;
        }

        error = validateBanksCount();
        if (error.isPresent()) {
            return error;
        }

        error = validateEstimateThreadsCount();
        if (error.isPresent()) {
            return error;
        }

        error = validateSDCCExecutable();
        if (error.isPresent()) {
            return error;
        }

        return validateDumpCallGraph();
    }

    private Optional<String> validateBankSize() {
        return checkPositiveInteger(getOptionValue(Options8051.OPTION_LONG_BANK_SIZE),
                "the bank size");
    }

    private Optional<String> validateBanksCount() {
        return checkPositiveInteger(getOptionValue(Options8051.OPTION_LONG_BANKS_COUNT),
                "banks count");
    }

    private Optional<String> validateEstimateThreadsCount() {
        return checkPositiveInteger(getOptionValue(Options8051.OPTION_LONG_THREADS_COUNT),
                "count of estimate threads");
    }

    private Optional<String> validateSDCCExecutable() {
        return checkNonEmptyString(getOptionValue(Options8051.OPTION_LONG_SDCC_EXEC),
                "SDCC executable");
    }

    private Optional<String> validateDumpCallGraph() {
        return checkNonEmptyString(getOptionValue(Options8051.OPTION_LONG_DUMP_CALL_GRAPH),
                "name of file for the call graph");
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
}
