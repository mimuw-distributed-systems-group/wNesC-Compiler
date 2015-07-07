package pl.edu.mimuw.nesc.compilation;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.problem.NescError;
import pl.edu.mimuw.nesc.problem.NescWarning;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Compilation listener that counts types of issues reported to it.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class CountingCompilationListener implements CompilationListener {
    /**
     * Listener that will receive all issues reported to this listener.
     */
    private final Optional<CompilationListener> targetListener;

    /**
     * Count of errors reported to this listener.
     */
    private int errorsCount;

    /**
     * Count of warnings reported to this listener.
     */
    private int warningsCount;

    public CountingCompilationListener(Optional<CompilationListener> targetListener) {
        checkNotNull(targetListener, "target listener cannot be null");
        this.targetListener = targetListener;
        this.warningsCount = this.errorsCount = 0;
    }

    /**
     * Get the count of errors reported to this listener.
     *
     * @return Count of errors.
     */
    public int getErrorsCount() {
        return errorsCount;
    }

    /**
     * Get the count of warnings reported to this listener.
     *
     * @return Count of warnings.
     */
    public int getWarningsCount() {
        return warningsCount;
    }

    @Override
    public void error(NescError error) {
        ++errorsCount;
        if (targetListener.isPresent()) {
            targetListener.get().error(error);
        }
    }

    @Override
    public void warning(NescWarning warning) {
        ++warningsCount;
        if (targetListener.isPresent()) {
            targetListener.get().warning(warning);
        }
    }
}
