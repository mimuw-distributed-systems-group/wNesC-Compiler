package pl.edu.mimuw.nesc.backend8051.option;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.cli.CommandLine;
import pl.edu.mimuw.nesc.codesize.SDCCMemoryModel;

import static com.google.common.base.Preconditions.checkArgument;
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
     * Regular expression for an entry in the bank schema.
     */
    private static final Pattern REGEXP_BANK_SCHEMA_ENTRY =
            Pattern.compile("(?<bankName>" + REGEXP_IDENTIFIER + ")"
                    + Options8051.SEPARATOR_BANKS_SCHEMA_INNER
                    + "(?<bankCapacity>\\d+)");

    private static final Pattern REGEXP_PARTITION_HEURISTIC =
            Pattern.compile("simple|greedy-(?<greedyPreValue>[1-9]\\d*)|bcomponents|tmsearch-(?<tmsearchMaxIterCount>\\d+)-(?<tmsearchMaxFruitlessIterCount>\\d+)");

    /**
     * Set with SDCC parameters that cannot be specified by the option.
     */
    private static final ImmutableSet<String> FORBIDDEN_SDCC_PARAMS;
    static {
        final ImmutableSet.Builder<String> forbiddenParamsBuilder = ImmutableSet.builder();
        // basic options
        forbiddenParamsBuilder.add("-c", "--compile-only", "-S", "-o");
        // processor target
        forbiddenParamsBuilder.add(
                "-mmcs51",
                "-mds390",
                "-mds400",
                "-mhc08",
                "-ms08",
                "-mz80",
                "-mz180",
                "-mr2k",
                "-mr3ka",
                "-mgbz80",
                "-mstm8",
                "-mpic14",
                "-mpic16"
        );
        // memory model options
        for (SDCCMemoryModel memoryModel : SDCCMemoryModel.values()) {
            forbiddenParamsBuilder.add(memoryModel.getOption());
        }
        FORBIDDEN_SDCC_PARAMS = forbiddenParamsBuilder.build();
    }

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
                new BankSchemaValidator(),
                new EstimateThreadsCountValidator(),
                new SDCCExecutableValidator(),
                new DumpCallGraphValidator(),
                new InterruptsValidator(),
                new SDCCParametersValidator(),
                new SDASExecutableValidator(),
                new MaximumInlineSizeValidator(),
                new DumpInlineFunctionsValidator(),
                new PartitionHeuristicValidator(),
                new EnumOptionValidator(Options8051.OPTION_LONG_SPANNING_FOREST,
                        Options8051.MAP_SPANNING_FOREST.keySet(),
                        "spanning forest kind"),
                new EnumOptionValidator(Options8051.OPTION_LONG_ARBITRARY_SUBTREE_PARTITIONING,
                        Options8051.MAP_ARBITRARY_SUBTREE_PARTITIONING.keySet(),
                        "arbitrary subtree partitioning mode"),
                new LoopFactorValidator(),
                new ConditionalFactorValidator()
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

    private Optional<String> checkGreaterOrEqual(Optional<String> value, String optionText,
                int lowerBound) {
        if (!value.isPresent()) {
            return Optional.absent();
        }

        final int intValue;
        try {
            intValue = Integer.parseInt(value.get());
        } catch (NumberFormatException e) {
            return Optional.of("'" + value.get() + "' is not a valid integer");
        }

        final Optional<String> message;
        if (intValue < lowerBound) {
            if (lowerBound == 1) {
                message = Optional.of(optionText + " must be positive");
            } else if (lowerBound == 0) {
                message = Optional.of(optionText + " cannot be negative");
            } else {
                message = Optional.of(optionText + " must be greater than or equal to "
                        + lowerBound);
            }
        } else {
            message = Optional.absent();
        }

        return message;
    }

    private Optional<String> checkNonEmptyString(Optional<String> value, String optionText) {
        return value.isPresent() && value.get().isEmpty()
                ? Optional.of(optionText + " cannot be empty")
                : Optional.<String>absent();
    }

    private boolean checkNotGreaterThanMaxInt(String value) {
        final BigInteger valueBigInt = new BigInteger(value);
        return valueBigInt.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0;
    }

    private Optional<Double> checkParsesAsDouble(String value) {
        try {
            return Optional.of(Double.parseDouble(value));
        } catch (NumberFormatException e) {
            return Optional.absent();
        }
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

    private final class BankSchemaValidator implements SingleValidator {
        @Override
        public Optional<String> validate() {
            final Optional<String> bankSchemaOpt = getOptionValue(Options8051.OPTION_LONG_BANKS);
            if (!bankSchemaOpt.isPresent()) {
                return Optional.absent();
            }

            final String msgPrefix = "invalid value for option '--"
                    + Options8051.OPTION_LONG_BANKS + "': ";
            final String[] elements = bankSchemaOpt.get()
                    .split(Options8051.SEPARATOR_BANKS_SCHEMA_OUTER, -1);
            final Set<String> definedBanks = new HashSet<>();

            // Name of the common bank
            if (!REGEXP_IDENTIFIER.matcher(elements[0]).matches()) {
                return Optional.of(msgPrefix + "name of common bank '" + elements[0] + "' is invalid");
            }

            // Other entries
            for (int i = 1; i < elements.length; ++i) {
                final Matcher entryMatcher = REGEXP_BANK_SCHEMA_ENTRY.matcher(elements[i]);
                if (entryMatcher.matches()) {
                    final String bankName = entryMatcher.group("bankName");
                    if (!definedBanks.add(bankName)) {
                        return Optional.of(msgPrefix + "bank '" + bankName + "' specified more than once");
                    }
                    final BigInteger capacity = new BigInteger(entryMatcher.group("bankCapacity"));
                    if (capacity.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                        return Optional.of(msgPrefix + "size of area available in bank '" + bankName
                                + "' exceeds " + Integer.MAX_VALUE);
                    }
                } else {
                    return Optional.of(msgPrefix + "'" + elements[i] + "' is invalid specification of a bank");
                }
            }

            // Check if the common bank has been specified
            if (!definedBanks.contains(elements[0])) {
                return Optional.of(msgPrefix + "size of area available in common bank '"
                        + elements[0] + "' is not specified");
            }

            return Optional.absent();
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
            return checkGreaterOrEqual(getOptionValue(Options8051.OPTION_LONG_THREADS_COUNT),
                    "count of estimate threads", 1);
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

    private final class SDCCParametersValidator implements SingleValidator {
        @Override
        public Optional<String> validate() {
            final Optional<String> sdccParams = getOptionValue(Options8051.OPTION_LONG_SDCC_PARAMS);
            if (!sdccParams.isPresent()) {
                return Optional.absent();
            }

            final String msgPrefix = "invalid value for option '--"
                    + Options8051.OPTION_LONG_SDCC_PARAMS + "': ";
            final ImmutableList<String> parsedParams;

            try {
                parsedParams = new SDCCParametersParser().parse(sdccParams.get());
            } catch (SDCCParametersParser.InvalidParametersException e) {
                return Optional.of(msgPrefix + e.getMessage());
            }

            final StringBuilder forbiddenMsgBuilder = new StringBuilder();
            for (String param : parsedParams) {
                if (FORBIDDEN_SDCC_PARAMS.contains(param)) {
                    if (forbiddenMsgBuilder.length() != 0) {
                        forbiddenMsgBuilder.append(", ");
                    }
                    forbiddenMsgBuilder.append('\'');
                    forbiddenMsgBuilder.append(param);
                    forbiddenMsgBuilder.append('\'');
                }
            }

            return forbiddenMsgBuilder.length() != 0
                    ? Optional.of(msgPrefix + "forbidden parameters specified: " + forbiddenMsgBuilder.toString())
                    : Optional.<String>absent();
        }
    }

    private final class SDASExecutableValidator implements SingleValidator {
        @Override
        public Optional<String> validate() {
            return checkNonEmptyString(getOptionValue(Options8051.OPTION_LONG_SDAS_EXEC),
                    "8051 assembler executable");
        }
    }

    private final class MaximumInlineSizeValidator implements SingleValidator {
        @Override
        public Optional<String> validate() {
            return checkGreaterOrEqual(getOptionValue(Options8051.OPTION_LONG_MAXIMUM_INLINE_SIZE),
                    "maximum size of an inline function", 0);
        }
    }

    private final class DumpInlineFunctionsValidator implements SingleValidator {
        @Override
        public Optional<String> validate() {
            return checkNonEmptyString(getOptionValue(Options8051.OPTION_LONG_DUMP_INLINE_FUNCTIONS),
                    "name of the file for names of inline functions");
        }
    }

    private final class PartitionHeuristicValidator implements SingleValidator {
        @Override
        public Optional<String> validate() {
            final Optional<String> value = getOptionValue(Options8051.OPTION_LONG_PARTITION_HEURISTIC);
            if (!value.isPresent()) {
                return Optional.absent();
            }

            final String msgPrefix = "invalid value for option '--"
                    + Options8051.OPTION_LONG_PARTITION_HEURISTIC + "': ";
            final Matcher matcher = REGEXP_PARTITION_HEURISTIC.matcher(value.get());

            if (!matcher.matches()) {
                return Optional.of(msgPrefix + "invalid kind of heuristic '" + value.get() + "'");
            } else if (value.get().startsWith("greedy-")) {
                return checkNotGreaterThanMaxInt(matcher.group("greedyPreValue"))
                        ? Optional.<String>absent()
                        : Optional.of(msgPrefix + "value of the parameter for the greedy heuristic exceeds "
                            + Integer.MAX_VALUE);
            } else if (value.get().startsWith("tmsearch-")) {
                if (!checkNotGreaterThanMaxInt(matcher.group("tmsearchMaxIterCount"))) {
                    return Optional.of(msgPrefix + "maximum count of iterations exceeds "
                            + Integer.MAX_VALUE);
                } else if (!checkNotGreaterThanMaxInt(matcher.group("tmsearchMaxFruitlessIterCount"))) {
                    return Optional.of(msgPrefix + "maximum count of consecutive fruitless iterations exceeds "
                            + Integer.MAX_VALUE);
                } else {
                    return Optional.absent();
                }
            } else {
                return Optional.absent();
            }
        }
    }

    private final class EnumOptionValidator implements SingleValidator {
        private final String optionName;
        private final ImmutableSet<String> correctValues;
        private final String enumDescription;

        private EnumOptionValidator(String optionName, ImmutableSet<String> correctValues,
                    String enumDescription) {
            checkNotNull(optionName, "option name cannot be null");
            checkNotNull(correctValues, "correct values cannot be null");
            checkNotNull(enumDescription, "enum description cannot be null");
            checkArgument(!optionName.isEmpty(), "option name cannot be an empty string");
            checkArgument(!enumDescription.isEmpty(), "enum description cannot be an empty string");
            this.optionName = optionName;
            this.correctValues = correctValues;
            this.enumDescription = enumDescription;
        }

        @Override
        public Optional<String> validate() {
            final Optional<String> value = getOptionValue(optionName);
            if (!value.isPresent()) {
                return Optional.absent();
            }

            return correctValues.contains(value.get())
                    ? Optional.<String>absent()
                    : Optional.of("invalid value for option '--" + optionName
                        + "': invalid " + enumDescription + " '" + value.get()
                        + "'");
        }
    }

    private final class LoopFactorValidator implements SingleValidator {
        @Override
        public Optional<String> validate() {
            final Optional<String> value = getOptionValue(Options8051.OPTION_LONG_LOOP_FACTOR);
            if (!value.isPresent()) {
                return Optional.absent();
            }

            final String msgPrefix = "invalid value for option '--"
                    + Options8051.OPTION_LONG_LOOP_FACTOR + "': ";
            final Optional<Double> parsedValue = checkParsesAsDouble(value.get());

            if (!parsedValue.isPresent()) {
                return Optional.of(msgPrefix + "'" + value.get() + "' is an invalid floating-point number");
            } else if (parsedValue.get() < 1.) {
                return Optional.of(msgPrefix + "number '" + value.get() + "' is not greater than or equal to 1");
            } else {
                return Optional.absent();
            }
        }
    }

    private final class ConditionalFactorValidator implements SingleValidator {
        @Override
        public Optional<String> validate() {
            final Optional<String> value = getOptionValue(Options8051.OPTION_LONG_CONDITIONAL_FACTOR);
            if (!value.isPresent()) {
                return Optional.absent();
            }

            final String msgPrefix = "invalid value for option '--"
                    + Options8051.OPTION_LONG_CONDITIONAL_FACTOR + "': ";
            final Optional<Double> parsedValue = checkParsesAsDouble(value.get());

            if (!parsedValue.isPresent()) {
                return Optional.of(msgPrefix + "'" + value.get() + "' is an invalid floating-point number");
            } else if (parsedValue.get() < 0. || parsedValue.get() > 1.) {
                return Optional.of(msgPrefix + "number '" + value.get() + "' is not in range [0, 1]");
            } else {
                return Optional.absent();
            }
        }
    }
}
