package pl.edu.mimuw.nesc.option;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.Option;

import com.google.common.base.Preconditions;

/**
 * Loads compiler's options definitions from file.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 *
 */
final class OptionsLoader {

	private static final String OPTION_PROPERTIES_FILE = "option/options.properties";

	private static final String PREFIX = "nesc.option";
	private static final String OPTIONS = "nesc.options";
	private static final String SHORT_NAME = "shortName";
	private static final String LONG_NAME = "longName";
	private static final String REQUIRED = "required";
	private static final String ARGS = "args";
	private static final String DESCRIPTION = "desc";

	public List<Option> load(String filePath) throws IOException {
		Preconditions.checkNotNull(filePath);
		final Properties prop = new Properties();
		prop.load(new FileInputStream(filePath));
		return load(prop);
	}

	public List<Option> load() throws IOException {
		final Properties prop = new Properties();
		prop.load(getClass().getClassLoader().getResourceAsStream(OPTION_PROPERTIES_FILE));
		return load(prop);
	}

	private List<Option> load(Properties prop) {
		final List<String> optionNames = loadStringList(prop, OPTIONS);
		final List<Option> result = new ArrayList<>();

		for (String optionName : optionNames) {
			final Option option = loadOption(prop, optionName);
			result.add(option);
		}
		return result;
	}

	private Option loadOption(Properties prop, String name) {
		final String shortName = loadString(prop, chain(PREFIX, name, SHORT_NAME));
		final Option.Builder builder = Option.builder(shortName);
		loadLongName(prop, name, builder);
		loadIsRequired(prop, name, builder);
		loadDescription(prop, name, builder);
		loadArgs(prop, name, builder);
		return builder.build();
	}

	private void loadLongName(Properties prop, String name, Option.Builder builder) {
		final String longName = loadString(prop, chain(PREFIX, name, LONG_NAME));
		if (longName != null) {
			builder.longOpt(longName);
		}
	}

	private void loadIsRequired(Properties prop, String name, Option.Builder builder) {
		final String key = chain(PREFIX, name, REQUIRED);
		final Boolean isRequired = loadBoolean(prop, key);
		if (isRequired != null && isRequired) {
			builder.required();
		}
	}

	private void loadDescription(Properties prop, String name, Option.Builder builder) {
		final String key = chain(PREFIX, name, DESCRIPTION);
		final String desc = loadString(prop, key);
		builder.desc(desc);
	}

	private void loadArgs(Properties prop, String name, Option.Builder builder) {
		final String key = chain(PREFIX, name, ARGS);
		final List<String> argNames = loadStringList(prop, key);
		if (!argNames.isEmpty()) {
            // FIXME: number of option's values
			builder.hasArgs();
			for (String argName : argNames) {
				loadArg(prop, name, argName, builder);
			}
		}
	}

	private void loadArg(Properties prop, String name, String argName, Option.Builder builder) {
		builder.argName(argName);
	}

	private Boolean loadBoolean(Properties prop, String key) {
		final String value = prop.getProperty(key);
		if (value == null) {
			return null;
		}
		return Boolean.parseBoolean(value);
	}

	@SuppressWarnings("unused")
	private int loadInt(Properties prop, String key) {
		return Integer.parseInt(prop.getProperty(key));
	}

	private String loadString(Properties prop, String key) {
		return prop.getProperty(key);
	}

	private List<String> loadStringList(Properties prop, String key) {
		final String property = prop.getProperty(key);
		if (property == null) {
			return new ArrayList<>();
		}
		final String[] parts = property.split(",");
		final List<String> result = new ArrayList<String>(parts.length);
		for (String part : parts) {
			result.add(part.trim());
		}
		return result;
	}

	private String chain(String... parts) {
		final StringBuilder builder = new StringBuilder();
		final int len = parts.length;

		builder.append(parts[0]);
		for (int i = 1; i < len; ++i) {
			builder.append('.');
			builder.append(parts[i]);
		}
		return builder.toString();
	}

}
