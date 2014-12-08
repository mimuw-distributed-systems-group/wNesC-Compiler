package pl.edu.mimuw.nesc.astbuilding;

import com.google.common.collect.ImmutableListMultimap;
import pl.edu.mimuw.nesc.analysis.SemanticListener;
import pl.edu.mimuw.nesc.environment.NescEntityEnvironment;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.problem.NescIssue;
import pl.edu.mimuw.nesc.token.Token;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class AstBuildingBase {

    protected final ErrorHelper errorHelper;
    protected final NescEntityEnvironment nescEnvironment;
    protected final ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder;

    /**
     * The semantic listener that will be used for generating events that occur
     * during the analysis.
     */
    protected final SemanticListener semanticListener;

    protected AstBuildingBase(NescEntityEnvironment nescEnvironment,
                              ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder,
                              ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder,
                              SemanticListener semanticListener) {
        this.nescEnvironment = nescEnvironment;
        this.errorHelper = new ErrorHelper(issuesMultimapBuilder);
        this.tokensMultimapBuilder = tokensMultimapBuilder;
        this.semanticListener = semanticListener;
    }

}
