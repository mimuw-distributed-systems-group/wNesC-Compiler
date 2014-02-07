package pl.edu.mimuw.nesc.option;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.cli.CommandLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Compiler's options holder.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class OptionsHolder {

    public static final String NESC_ENTRY_NAME = "m";
    public static final String NESC_PROJECT_DIRECTORY_NAME = "p";
    public static final String NESC_INCLUDE_NAME = "include";
    public static final String NESC_SEARCH_PATH_NAME = "I";
    public static final String NESC_IQUOTE = "iquote";
    public static final String NESC_DEFINE = "D";

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
     * Returns file path to the main configuration definition.
     *
     * @return main configuration's file path
     */
    public String getEntryEntity() {
        return getValue(NESC_ENTRY_NAME);
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
