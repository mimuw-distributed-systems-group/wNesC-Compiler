package pl.edu.mimuw.nesc.option;

import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Options parser.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class OptionsParser implements OptionsHelpPrinter {

    private final CommandLineParser parser;
    private final Options options;
    private final HelpFormatter helpFormatter;

    /**
     * Creates options parser.
     */
    public OptionsParser() {
        this.parser = new DefaultParser();
        this.options = new Options();
        this.helpFormatter = new HelpFormatter();
        this.helpFormatter.setWidth(100);
    }

    /**
     * Parses the program's arguments and converts them into compiler options.
     *
     * @param args program's arguments
     * @return options holder
     * @throws ParseException
     * @throws IOException
     */
    public OptionsHolder parse(String[] args) throws ParseException, IOException {
        buildOptions();
        final CommandLine commandLine = parser.parse(this.options, args, false);
        return new OptionsHolder(commandLine);
    }

    @Override
    public void printHelpWithError(String errorMessage) {
        printError(errorMessage);
        printHelp();
    }

    @Override
    public void printError(String errorMessage) {
        checkNotNull(errorMessage, "error message cannot be null");
        checkArgument(!errorMessage.isEmpty(), "error message cannot be an empty string");
        System.out.println("error: " + errorMessage);
    }

    /**
     * Prints proper options usage to standard output.
     */
    @Override
    public void printHelp() {
        helpFormatter.printHelp("nesc", this.options);
    }

    private void buildOptions() throws IOException {
        final OptionsLoader loader = new OptionsLoader();
        final List<Option> options = loader.load();
        for (Option option : options) {
            this.options.addOption(option);
        }
    }

}
