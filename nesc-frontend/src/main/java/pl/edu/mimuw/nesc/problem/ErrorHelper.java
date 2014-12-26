package pl.edu.mimuw.nesc.problem;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.problem.issue.CautionaryIssue;
import pl.edu.mimuw.nesc.problem.issue.ErroneousIssue;
import pl.edu.mimuw.nesc.problem.issue.Issue;

import static java.lang.String.format;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class ErrorHelper {

    private static final Logger LOG = Logger.getLogger(ErrorHelper.class);

    private final ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder;

    public ErrorHelper(ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder) {
        this.issuesMultimapBuilder = issuesMultimapBuilder;
    }

    private static String getCodeString(Optional<Issue.Code> code) {
        return   code.isPresent()
               ? " " + code.get().asString()
               : "";
    }

    private void _warning(Location startLocation, Optional<Location> endLocation,
                         Optional<Issue.Code> code, String message) {
        final NescWarning warning = new NescWarning(startLocation, endLocation, code, message);
        this.issuesMultimapBuilder.put(startLocation.getLine(), warning);
        LOG.info(format("warning%s: %s in %s at line: %d, column: %d.", getCodeString(code),
                message, startLocation.getFilePath(), startLocation.getLine(),
                startLocation.getColumn()));
    }

    public void warning(Location startLocation, Optional<Location> endLocation, String message) {
        _warning(startLocation, endLocation, Optional.<Issue.Code>absent(), message);
    }

    public void warning(Location startLocation, Location endLocation, String message) {
        _warning(startLocation, Optional.of(endLocation), Optional.<Issue.Code>absent(),
                 message);
    }

    public void warning(Location startLocation, Location endLocation, CautionaryIssue warning) {
        _warning(startLocation, Optional.of(endLocation), Optional.of(warning.getCode()),
                 warning.generateDescription());
    }

    private void _error(Location startLocation, Optional<Location> endLocation,
                        Optional<Issue.Code> code, String message) {
        final NescError error = new NescError(startLocation, endLocation, code, message);
        this.issuesMultimapBuilder.put(startLocation.getLine(), error);
        LOG.info(format("error%s: %s in %s at line: %d, column: %d.", getCodeString(code),
                message, startLocation.getFilePath(), startLocation.getLine(),
                startLocation.getColumn()));
    }

    public void error(Location startLocation, Optional<Location> endLocation, String message) {
        _error(startLocation, endLocation, Optional.<Issue.Code>absent(), message);
    }

    public void error(Location startLocation, Location endLocation, String message) {
        _error(startLocation, Optional.of(endLocation), Optional.<Issue.Code>absent(),
               message);
    }

    public void error(Location startLocation, Location endLocation, ErroneousIssue error) {
        _error(startLocation, Optional.of(endLocation), Optional.of(error.getCode()),
               error.generateDescription());
    }

    public void error(Location startLocation, Optional<Location> endLocation, ErroneousIssue error) {
        _error(startLocation, endLocation, Optional.of(error.getCode()),
                error.generateDescription());
    }
}
