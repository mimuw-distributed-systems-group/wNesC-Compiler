package pl.edu.mimuw.nesc.compilation;

import java.io.UnsupportedEncodingException;
import pl.edu.mimuw.nesc.problem.NescError;
import pl.edu.mimuw.nesc.problem.NescIssuePrinter;
import pl.edu.mimuw.nesc.problem.NescWarning;

/**
 * <p>Listener that prints issues it receives to stderr using
 * {@link NescIssuePrinter}.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class DefaultCompilationListener implements CompilationListener {
    /**
     * Printer used for printing issues.
     */
    private final NescIssuePrinter issuePrinter;

    public DefaultCompilationListener() {
        try {
            this.issuePrinter = new NescIssuePrinter(System.err, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            /* This should never be executed because every Java platform must
               support UTF-8 encoding. */
            throw new RuntimeException("UTF-8 is unknown encoding");
        }
    }

    @Override
    public void error(NescError error) {
        issuePrinter.print(error);
    }

    @Override
    public void warning(NescWarning warning) {
        issuePrinter.print(warning);
    }
}
