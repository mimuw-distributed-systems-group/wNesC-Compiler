package pl.edu.mimuw.nesc.backend8051;

import com.google.common.base.Optional;
import java.io.IOException;
import org.apache.commons.cli.ParseException;
import pl.edu.mimuw.nesc.backend8051.option.Options8051Holder;
import pl.edu.mimuw.nesc.backend8051.option.Options8051Parser;

/**
 * <p>Compilation for 8051 microcontrollers.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class Main {
    /**
     * Code returned by the compiler to the system when the compilation fails.
     */
    private static final int STATUS_ERROR = 1;

    /**
     * Code returned by the compiler to the system when the compilation
     * succeeds.
     */
    private static final int STATUS_SUCCESS = 0;

    /**
     * Options for the 8051 version of the compiler.
     */
    private final Options8051Holder options;

    private static Options8051Holder parseParameters(String[] args) throws IOException {
        final Options8051Parser optionsParser = new Options8051Parser(args);
        final Options8051Holder options;

        // Parse options
        try {
            options = optionsParser.parse();
        } catch (ParseException e) {
            if (args.length != 0) {
                optionsParser.printError(e.getMessage());
            } else {
                optionsParser.printHelpWithError(e.getMessage());
            }
            System.exit(STATUS_ERROR);
            throw new RuntimeException("this should never be thrown");
        }

        // Validate options
        final Optional<String> error = optionsParser.getValidator().validate();
        if (error.isPresent()) {
            optionsParser.printError(error.get());
            System.exit(STATUS_ERROR);
        }

        return options;
    }

    public static void main(String[] args) {
        try {
            new Main(parseParameters(args)).compile();
            System.exit(STATUS_SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(STATUS_ERROR);
        }
    }

    private Main(Options8051Holder options) {
        this.options = options;
    }

    /**
     * Performs the whole compilation process for 8051 microcontrollers.
     */
    private void compile() {
    }
}
