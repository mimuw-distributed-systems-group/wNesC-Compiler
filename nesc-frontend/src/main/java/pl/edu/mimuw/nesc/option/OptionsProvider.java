package pl.edu.mimuw.nesc.option;

/**
 * <p>Interface for providing options for the frontend. The provided options can
 * be richer than the frontend options (and this interface is intended to allow
 * this).</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface OptionsProvider extends OptionsHelpPrinter {
    /**
     * <p>Get the options provided by this object. The returned object should
     * contain options for the frontend. It can contain options that are not
     * related with the frontend.</p>
     *
     * @return The parsed options provided by this object.
     */
    OptionsHolder getOptions();

    /**
     * <p>Get the count of parameters specified in the command line that are
     * represented by options provided by this object (the count shall take into
     * account all parameters, including the invalid ones).</p>
     *
     * @return Count of command line parameters that are represented by
     *         options provided by this object.
     */
    int parametersCount();
}
