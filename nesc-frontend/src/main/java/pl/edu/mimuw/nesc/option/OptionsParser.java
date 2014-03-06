package pl.edu.mimuw.nesc.option;

import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Options parser.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class OptionsParser {

    private final CommandLineParser parser;
    private final Options options;
    private final HelpFormatter helpFormatter;

    /**
     * Creates options parser.
     *
     * @throws IOException
     */
    public OptionsParser() throws IOException {
        this.parser = new DefaultParser();
        this.options = new Options();
        this.helpFormatter = new HelpFormatter();
        buildOptions();
    }

    /**
     * Parses the program's arguments and converts them into compiler options.
     *
     * @param args program's arguments
     * @throws ParseException
     */
    public void parse(String[] args) throws ParseException {
        final CommandLine commandLine = parser.parse(this.options, args);
        OptionsHolder.instance().setOptions(commandLine);
    }

    /**
     * Prints proper options usage to standard output.
     */
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
