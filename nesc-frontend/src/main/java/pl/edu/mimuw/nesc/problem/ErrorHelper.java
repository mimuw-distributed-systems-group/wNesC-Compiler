package pl.edu.mimuw.nesc.problem;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.ast.Location;

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

    public void warning(Location startLocation, Optional<Location> endLocation, String message) {
        final NescWarning warning = new NescWarning(startLocation, endLocation, message);
        this.issuesMultimapBuilder.put(startLocation.getLine(), warning);
        LOG.info(format("warning: %s in %s at line: %d, column: %d.", message, startLocation.getFilePath(),
                startLocation.getLine(), startLocation.getColumn()));
    }

    public void error(Location startLocation, Optional<Location> endLocation, String message) {
        final NescError error = new NescError(startLocation, endLocation, message);
        this.issuesMultimapBuilder.put(startLocation.getLine(), error);
        LOG.info(format("error: %s in %s at line: %d, column: %d.", message, startLocation.getFilePath(),
                startLocation.getLine(), startLocation.getColumn()));
    }
}
