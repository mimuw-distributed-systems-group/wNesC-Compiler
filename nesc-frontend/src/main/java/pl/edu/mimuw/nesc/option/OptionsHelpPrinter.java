package pl.edu.mimuw.nesc.option;

/**
 * <p>Interface that allows printing help for options.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface OptionsHelpPrinter {
    /**
     * Print help about usage of the options to stdout.
     */
    void printHelp();

    /**
     * Print an error message related to the use of options along with help
     * about their proper usage to stdout.
     *
     * @param errorMessage Error message to include in the text that will be
     *                     printed to stdout.
     * @throws NullPointerException Given message is <code>null</code>.
     * @throws IllegalArgumentException Given message is an empty string.
     */
    void printHelpWithError(String errorMessage);
}
