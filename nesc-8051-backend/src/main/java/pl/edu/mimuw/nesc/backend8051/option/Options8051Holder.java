package pl.edu.mimuw.nesc.backend8051.option;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import org.apache.commons.cli.CommandLine;
import pl.edu.mimuw.nesc.codesize.SDCCMemoryModel;

import static com.google.common.base.Preconditions.checkNotNull;
import static pl.edu.mimuw.nesc.backend8051.option.Options8051.*;

/**
 * Objects that are to easily extract information about the 8051 options.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class Options8051Holder {
    /**
     * Function for parsing integers in strings.
     */
    private static final Function<String, Integer> FUNCTION_PARSE_INT = new Function<String, Integer>() {
        @Override
        public Integer apply(String arg) {
            checkNotNull(arg, "argument cannot be null");
            return Integer.valueOf(arg);
        }
    };

    /**
     * The parsed 8051 options.
     */
    private final CommandLine cmdLine;

    Options8051Holder(CommandLine cmdLine) {
        checkNotNull(cmdLine, "the parsed options cannot be null");
        this.cmdLine = cmdLine;
    }

    /**
     * Get the SDCC memory model specified by options.
     *
     * @return Memory model indicated by options. The object is absent if the
     *         model has not been specified.
     */
    public Optional<SDCCMemoryModel> getMemoryModel() {
        if (cmdLine.hasOption(OPTION_LONG_MODEL_SMALL)) {
            return Optional.of(SDCCMemoryModel.SMALL);
        } else if (cmdLine.hasOption(OPTION_LONG_MODEL_MEDIUM)) {
            return Optional.of(SDCCMemoryModel.MEDIUM);
        } else if (cmdLine.hasOption(OPTION_LONG_MODEL_LARGE)) {
            return Optional.of(SDCCMemoryModel.LARGE);
        } else if (cmdLine.hasOption(OPTION_LONG_MODEL_HUGE)) {
            return Optional.of(SDCCMemoryModel.HUGE);
        } else {
            return Optional.absent();
        }
    }

    public Optional<Integer> getBankSize() {
        return getIntegerOptionValue(OPTION_LONG_BANK_SIZE);
    }

    public Optional<Integer> getBanksCount() {
        return getIntegerOptionValue(OPTION_LONG_BANKS_COUNT);
    }

    public Optional<Integer> getEstimateThreadsCount() {
        return getIntegerOptionValue(OPTION_LONG_THREADS_COUNT);
    }

    public Optional<String> getSDCCExecutable() {
        return Optional.fromNullable(cmdLine.getOptionValue(OPTION_LONG_SDCC_EXEC));
    }

    public Optional<String> getCallGraphFile() {
        return Optional.fromNullable(cmdLine.getOptionValue(OPTION_LONG_DUMP_CALL_GRAPH));
    }

    private Optional<Integer> getIntegerOptionValue(String optionName) {
        final Optional<String> optionValue = Optional.fromNullable(
                cmdLine.getOptionValue(optionName));
        return optionValue.transform(FUNCTION_PARSE_INT);
    }
}
