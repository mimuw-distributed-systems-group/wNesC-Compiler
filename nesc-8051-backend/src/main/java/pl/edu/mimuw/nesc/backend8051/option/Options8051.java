package pl.edu.mimuw.nesc.backend8051.option;

/**
 * <p>Class with some data about the options for 8051 version of the
 * compiler.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class Options8051 {
    /**
     * Short names of options for the 8051 backend.
     */
    public static final String OPTION_SHORT_BANKS_COUNT = "n";
    public static final String OPTION_SHORT_THREADS_COUNT = "z";

    /**
     * Long names of options for the 8051 backend.
     */
    public static final String OPTION_LONG_BANK_SIZE = "bank-size";
    public static final String OPTION_LONG_BANKS_COUNT = "banks-count";
    public static final String OPTION_LONG_THREADS_COUNT = "estimate-threads";
    public static final String OPTION_LONG_SDCC_EXEC = "sdcc-exec";
    public static final String OPTION_LONG_MODEL_SMALL = "model-small";
    public static final String OPTION_LONG_MODEL_MEDIUM = "model-medium";
    public static final String OPTION_LONG_MODEL_LARGE = "model-large";
    public static final String OPTION_LONG_MODEL_HUGE = "model-huge";
    public static final String OPTION_LONG_DUMP_CALL_GRAPH = "dump-call-graph";

    /**
     * Private constructor to prevent instantiating this class.
     */
    private Options8051() {
    }
}
