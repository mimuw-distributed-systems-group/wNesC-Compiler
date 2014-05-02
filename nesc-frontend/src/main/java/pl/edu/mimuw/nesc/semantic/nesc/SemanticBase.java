package pl.edu.mimuw.nesc.semantic.nesc;

import com.google.common.collect.ImmutableListMultimap;
import pl.edu.mimuw.nesc.environment.NescEntityEnvironment;
import pl.edu.mimuw.nesc.issue.ErrorHelper;
import pl.edu.mimuw.nesc.issue.NescIssue;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class SemanticBase {

    protected final NescEntityEnvironment nescEnvironment;
    protected final ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder;
    protected final ErrorHelper errorHelper;

    protected SemanticBase(NescEntityEnvironment nescEnvironment,
                           ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder) {
        this.nescEnvironment = nescEnvironment;
        this.issuesMultimapBuilder = issuesMultimapBuilder;
        this.errorHelper = new ErrorHelper(issuesMultimapBuilder);
    }
}
