package pl.edu.mimuw.nesc.backend8051.option;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import org.apache.commons.cli.CommandLine;
import pl.edu.mimuw.nesc.codepartition.BankSchema;
import pl.edu.mimuw.nesc.codesize.SDCCMemoryModel;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
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

    public Optional<BankSchema> getBankSchema() {
        final Optional<String> bankSchemaOpt = Optional.fromNullable(
                cmdLine.getOptionValue(OPTION_LONG_BANKS));
        if (!bankSchemaOpt.isPresent()) {
            return Optional.absent();
        }

        final String[] elements = bankSchemaOpt.get().split(SEPARATOR_BANKS_SCHEMA_OUTER, -1);
        Optional<Integer> commonBankSize = Optional.absent();
        final String commonBankPrefix = elements[0] + SEPARATOR_BANKS_SCHEMA_INNER;

        // Get the size of the common bank
        for (int i = 1; i < elements.length; ++i) {
            if (elements[i].startsWith(commonBankPrefix)) {
                checkState(!commonBankSize.isPresent(), "size of the common bank occurs more than once");
                commonBankSize = Optional.of(Integer.parseInt(elements[i].substring(
                        commonBankPrefix.length())));
            }
        }

        checkState(commonBankSize.isPresent(), "size of the common bank is not specified");
        final BankSchema.Builder bankSchemaBuilder = BankSchema.builder(
                elements[0], commonBankSize.get());

        // Add remaining banks
        for (int i = 1; i < elements.length; ++i) {
            if (!elements[i].startsWith(commonBankPrefix)) {
                final int indexOfSep = elements[i].indexOf(SEPARATOR_BANKS_SCHEMA_INNER);
                checkState(indexOfSep != -1, "invalid entry of bank schema");
                final String bankName = elements[i].substring(0, indexOfSep);
                final String bankCapacity = elements[i].substring(indexOfSep + SEPARATOR_BANKS_SCHEMA_INNER.length());
                bankSchemaBuilder.addBank(bankName, Integer.parseInt(bankCapacity));
            }
        }

        return Optional.of(bankSchemaBuilder.build());
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

    /**
     * Get mapping from unique names of functions to numbers of interrupts they
     * handle.
     *
     * @return Multimap that specifies interrupts handled by functions.
     */
    public ImmutableSetMultimap<String, Integer> getInterrupts() {
        final Optional<String> interruptsMap = Optional.fromNullable(
                cmdLine.getOptionValue(OPTION_LONG_INTERRUPTS));
        if (!interruptsMap.isPresent()) {
            return ImmutableSetMultimap.of();
        }

        final ImmutableSetMultimap.Builder<String, Integer> interruptsBuilder =
                ImmutableSetMultimap.builder();
        final String[] assignments = interruptsMap.get().split(SEPARATOR_INTERRUPT_ASSIGNMENT_OUTER);

        for (String assignment : assignments) {
            final int equalitySignIndex = assignment.indexOf(SEPARATOR_INTERRUPT_ASSIGNMENT_INNER);
            if (equalitySignIndex == -1) {
                throw new IllegalStateException("invalid assignment of a function to an interrupt");
            }

            interruptsBuilder.put(assignment.substring(0, equalitySignIndex),
                    Integer.valueOf(assignment.substring(equalitySignIndex
                            + SEPARATOR_INTERRUPT_ASSIGNMENT_INNER.length())));
        }

        return interruptsBuilder.build();
    }

    /**
     * Check if the option that allows changing banked, defined and not
     * spontaneous functions to not banked is specified.
     *
     * @return <code>true</code> if and only if the option that allows
     *         relaxation of banked characteristics is given.
     */
    public boolean getRelaxBanked() {
        return cmdLine.hasOption(OPTION_LONG_RELAX_BANKED);
    }

    /**
     * Get parameters for SDCC specified by the option. The object is absent if
     * the option has not been used by the user.
     *
     * @return List with parameters for SDCC as specified by the user.
     */
    public Optional<ImmutableList<String>> getSDCCParameters() {
        final Optional<String> sdccParams = Optional.fromNullable(
                cmdLine.getOptionValue(OPTION_LONG_SDCC_PARAMS));

        try {
            return sdccParams.isPresent()
                    ? Optional.of(new SDCCParametersParser().parse(sdccParams.get()))
                    : Optional.<ImmutableList<String>>absent();
        } catch (SDCCParametersParser.InvalidParametersException e) {
            throw new RuntimeException("invalid SDCC parameters", e);
        }
    }

    public Optional<String> getSDASExecutable() {
        return Optional.fromNullable(cmdLine.getOptionValue(OPTION_LONG_SDAS_EXEC));
    }

    public Optional<Integer> getMaximumInlineSize() {
        return getIntegerOptionValue(OPTION_LONG_MAXIMUM_INLINE_SIZE);
    }

    public boolean getRelaxInline() {
        return cmdLine.hasOption(OPTION_LONG_RELAX_INLINE);
    }

    private Optional<Integer> getIntegerOptionValue(String optionName) {
        final Optional<String> optionValue = Optional.fromNullable(
                cmdLine.getOptionValue(optionName));
        return optionValue.transform(FUNCTION_PARSE_INT);
    }
}
