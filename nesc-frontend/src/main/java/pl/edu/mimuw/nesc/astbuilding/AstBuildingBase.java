package pl.edu.mimuw.nesc.astbuilding;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.issue.NescError;
import pl.edu.mimuw.nesc.issue.NescIssue;
import pl.edu.mimuw.nesc.issue.NescWarning;
import pl.edu.mimuw.nesc.token.Token;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class AstBuildingBase {

    protected static final Logger LOG = Logger.getLogger(AstBuildingBase.class);

    protected final ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder;
    protected final ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder;

    protected AstBuildingBase(ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder,
                              ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder) {
        this.issuesMultimapBuilder = issuesMultimapBuilder;
        this.tokensMultimapBuilder = tokensMultimapBuilder;
    }

    protected void warning(Location startLocation, Optional<Location> endLocation, String message) {
        final NescWarning warning = new NescWarning(startLocation, endLocation, message);
        this.issuesMultimapBuilder.put(startLocation.getLine(), warning);
        LOG.info("Warning at (" + startLocation.getLine() + ", " + startLocation.getColumn() + "), " +
                startLocation.getFilePath() + " : " + message);
    }

    protected void error(Location startLocation, Optional<Location> endLocation, String message) {
        final NescError error = new NescError(startLocation, endLocation, message);
        this.issuesMultimapBuilder.put(startLocation.getLine(), error);
        LOG.info("Error at (" + startLocation.getLine() + ", " + startLocation.getColumn() + "), " +
                startLocation.getFilePath() + " : " + message);
    }

}
