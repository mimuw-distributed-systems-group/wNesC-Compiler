package pl.edu.mimuw.nesc.option;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.cli.CommandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import pl.edu.mimuw.nesc.common.SchedulerSpecification;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Compiler's options holder.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class OptionsHolder {

    public static final String NESC_ENTRY_PATH = "m";
    public static final String NESC_PROJECT_DIRECTORY_NAME = "p";
    public static final String NESC_INCLUDE_NAME = "include";
    public static final String NESC_SEARCH_PATH_NAME = "I";
    public static final String NESC_IQUOTE = "iquote";
    public static final String NESC_DEFINE = "D";
    public static final String NESC_SCHEDULER = "scheduler";
    public static final String NESC_ABI_PLATFORM = "abi-platform";
    public static final String NESC_ABI_FILE = "abi-file";
    public static final String NESC_OUTPUT_FILE = "o";
    public static final String NESC_EXTERNAL_VARIABLES = "e";

    private final CommandLine cmd;

    public OptionsHolder(CommandLine cmd) {
        checkNotNull(cmd);
        this.cmd = cmd;
    }

    /**
     * Gets option's values as a list.
     *
     * @param optionName option's name
     * @return values list, empty list when option's value was not specified
     */
    public List<String> getValuesList(String optionName) {
        final String[] values = cmd.getOptionValues(optionName);
        if (values == null) {
            return new ArrayList<>(1);
        }
        final List<String> result = new ArrayList<>(values.length);
        Collections.addAll(result, values);
        return result;
    }

    /**
     * Gets option's value as a single value or the first element of values
     * list.
     *
     * @param optionName option's name
     * @return value or <code>null</code> when option's value was not specified
     */
    public String getValue(String optionName) {
        return cmd.getOptionValue(optionName);
    }

    /**
     * Checks if specified option's is set true or false. Can be called only for
     * options of boolean type.
     *
     * @param optionName option name
     * @return <code>false</code> when option is not set or is set to
     * <code>false</code>
     */
    public Boolean isSet(String optionName) {
        final String value = cmd.getOptionValue(optionName);
        if (value == null) {
            return Boolean.FALSE;
        }
        // TODO: throw error when value is unknown
        return Boolean.valueOf(value);
    }

    /**
     * Project root directory path.
     *
     * @return root directory path
     */
    public String getProjectPath() {
        return getValue(NESC_PROJECT_DIRECTORY_NAME);
    }

    /**
     * Returns a relative path to the top-level configuration definition
     * (without .nc extension).
     *
     * @return path to definition of top-level configuration
     */
    public String getEntryEntityPath() {
        return getValue(NESC_ENTRY_PATH);
    }

    /**
     * Returns directories paths to be searched for source and header files.
     *
     * @return directories paths
     */
    public List<String> getSourcePaths() {
        return getValuesList(NESC_SEARCH_PATH_NAME);
    }

    /**
     * Returns files that should be included into the project entry entity.
     *
     * @return files paths
     */
    public List<String> getDefaultIncludeFiles() {
        return getValuesList(NESC_INCLUDE_NAME);
    }

    /**
     * Returns directories paths to be searched for header files in user space.
     *
     * @return directories path
     */
    public List<String> getUserSourcePaths() {
        return getValuesList(NESC_IQUOTE);
    }

    /**
     * Returns a map of predefined macros.
     *
     * @return predefined macros
     */
    public Map<String, String> getPredefinedMacros() {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        final List<String> values = getValuesList(NESC_DEFINE);
        for (String value : values) {
            final String[] parsed = parseArgWithValue(value);
            /* Set default macro value 1 */
            if (parsed.length == 1) {
                builder.put(parsed[0], "1");
            } else {
                builder.put(parsed[0], parsed[1]);
            }
        }

        return builder.build();
    }

    public Optional<SchedulerSpecification> getSchedulerSpecification() {
        final String specification = getValue(NESC_SCHEDULER);
        return specification != null
                ? Optional.of(new SchedulerSpecification(specification))
                : Optional.<SchedulerSpecification>absent();
    }

    public String getABIPlatformName() {
        return getValue(NESC_ABI_PLATFORM);
    }

    public String getABIFilename() {
        return getValue(NESC_ABI_FILE);
    }

    public Optional<String> getOutputFile() {
        return Optional.fromNullable(getValue(NESC_OUTPUT_FILE));
    }

    public ImmutableSet<String> getExternalVariables() {
        final String externalVariables = getValue(NESC_EXTERNAL_VARIABLES);
        return externalVariables == null
                ? ImmutableSet.<String>of()
                : ImmutableSet.copyOf(Arrays.asList(externalVariables.split(",")));
    }

    /**
     * Check the correctness of the options that are present in this holder.
     *
     * @param isStandalone Value indicating if the options are passed to
     *                     a standalone instance of the frontend (it has
     *                     impact on checking their correctness).
     * @return Description of an error of the usage of options. If the
     *         validation succeeds, the object is absent.
     */
    public Optional<String> validate(boolean isStandalone) {
        Optional<String> error;

        error = validateSchedulerSpec(isStandalone);
        if (error.isPresent()) {
            return error;
        }

        error = validateABIOptions();
        if (error.isPresent()) {
            return error;
        }

        error = validateOutputFileOption(isStandalone);
        if (error.isPresent()) {
            return error;
        }

        error = validateExternalVariablesOption();

        return error;
    }

    private Optional<String> validateSchedulerSpec(boolean isStandalone) {
        final String specification = getValue(NESC_SCHEDULER);
        if (specification == null) {
            return isStandalone
                    ? Optional.of("missing scheduler specification, use '--scheduler' parameter")
                    : Optional.<String>absent();
        }

        final String[] values = specification.split(",", -1);

        if (values.length != 6) {
            return Optional.of("expecting six comma-separated values in the scheduler specification but "
                    + values.length + " given");
        } else {
            // Check if all values aren't empty
            for (int i = 0; i < values.length; ++i) {
                if (values[i].isEmpty()) {
                    return Optional.of("the " + getOrdinalForm(i + 1)
                            + " value in the scheduler specification is empty");
                }
            }
        }

        return Optional.absent();
    }

    private Optional<String> validateABIOptions() {
        if (getValue(NESC_ABI_FILE) != null && getValue(NESC_ABI_PLATFORM) != null) {
            return Optional.of("cannot combine option '--abi-platform' (or equivalently '-a') with '--abi-file' (equivalently '-A')");
        }
        return Optional.absent();
    }

    private Optional<String> validateOutputFileOption(boolean isStandalone) {
        if (!isStandalone && getValue(NESC_OUTPUT_FILE) != null) {
            return Optional.of("cannot use option '-o' (or equivalently '--output-file') in the plug-in mode");
        }
        return Optional.absent();
    }

    private Optional<String> validateExternalVariablesOption() {
        final String externalVariables = getValue(NESC_EXTERNAL_VARIABLES);
        if (externalVariables == null) {
            return Optional.absent();
        }

        // Check if all names are not empty

        final String[] names = externalVariables.split(",", -1);

        for (int i = 0; i < names.length; ++i) {
            if (names[i].isEmpty()) {
                return Optional.of("the " + getOrdinalForm(i + 1)
                        + " name in external variables list is empty");
            }
        }

        return Optional.absent();
    }

    private String getOrdinalForm(int n) {
        switch (n) {
            case 1:  return "1st";
            case 2:  return "2nd";
            case 3:  return "3rd";
            default: return n + "th";
        }
    }

    private String[] parseArgWithValue(String str) {
        final String[] parts = str.split("=");
        if (parts.length == 1) {
            return new String[]{parts[0]};
        } else if (parts.length == 2) {
            return new String[]{parts[0], parts[1]};
        } else {
            // TODO: throw error
            throw new RuntimeException();
        }
    }

}
