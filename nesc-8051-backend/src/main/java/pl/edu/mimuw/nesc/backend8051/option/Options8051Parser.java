package pl.edu.mimuw.nesc.backend8051.option;

import com.google.common.base.Optional;
import java.io.IOException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import pl.edu.mimuw.nesc.option.OptionsHolder;
import pl.edu.mimuw.nesc.option.OptionsProvider;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Object that parses options for the 8051 version of the compiler. It also
 * provides options for further usage by the frontend - the provided options are
 * the parsed ones.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class Options8051Parser implements OptionsProvider {
    /**
     * Object with the definition of options for the 8051 version of the
     * compiler.
     */
    private final Options optionsDefinition;

    /**
     * Object that will print help about the usage of options for the 8051
     * version of the compiler.
     */
    private final HelpFormatter helpFormatter;

    /**
     * Parameters for the 8051 compiler that will be parsed.
     */
    private final String[] parameters;

    /**
     * Object that contains parsed parameters if it already happen.
     */
    private Optional<CommandLine> parsedParameters;

    /**
     * Initializes this parser to parse the given parameters.
     *
     * @param args Parameters that will be parsed.
     * @throws IOException Cannot read the definition of frontend options.
     */
    public Options8051Parser(String[] args) throws IOException {
        checkNotNull(args, "arguments cannot be null");

        // Set all fields
        this.optionsDefinition = new Options8051Factory().newOptions();
        this.helpFormatter = new HelpFormatter();
        this.parameters = args;
        this.parsedParameters = Optional.absent();

        // Configure the help formatter
        this.helpFormatter.setWidth(100);
        this.helpFormatter.setLeftPadding(3);
    }

    /**
     * Parses the parameters given at construction of the parser.
     *
     * @return Newly created object that allows extracting information about the
     *         parsed options.
     * @throws ParseException The options specified by the user are invalid.
     */
    public Options8051Holder parse() throws ParseException {
        if (!parsedParameters.isPresent()) {
            final DefaultParser parser = new DefaultParser();
            final CommandLine afterParsing = parser.parse(optionsDefinition, parameters, false);
            this.parsedParameters = Optional.of(afterParsing);
        }

        return new Options8051Holder(parsedParameters.get());
    }

    /**
     * Get the validator for the parsed options created from parameters given
     * at construction of the parser.
     *
     * @return Newly created validator for the parsed options.
     * @throws IllegalStateException Options have not been parsed yet or the
     *                               parsing process has failed.
     */
    public Options8051Validator getValidator() {
        checkState(parsedParameters.isPresent(), "options have not been parsed yet");
        return new Options8051Validator(parsedParameters.get());
    }

    @Override
    public OptionsHolder getOptions() {
        checkState(parsedParameters.isPresent(), "options have not been parsed yet");
        return new OptionsHolder(parsedParameters.get());
    }

    @Override
    public int parametersCount() {
        return parameters.length;
    }

    @Override
    public void printHelp() {
        helpFormatter.printHelp("nesc-8051 -m <main-configuration> -p <project-root-dir> [supplementary-options]",
                "wNesC compiler [8051 microcontrollers version]", optionsDefinition, null);
    }

    @Override
    public void printError(String errorMessage) {
        checkNotNull(errorMessage, "error message cannot be null");
        checkArgument(!errorMessage.isEmpty(), "error message cannot be an empty string");
        System.out.println("error: " + errorMessage);
    }

    @Override
    public void printHelpWithError(String errorMessage) {
        printError(errorMessage);
        printHelp();
    }
}
