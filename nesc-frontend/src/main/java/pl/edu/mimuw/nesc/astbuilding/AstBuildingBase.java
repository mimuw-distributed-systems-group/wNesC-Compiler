package pl.edu.mimuw.nesc.astbuilding;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableListMultimap;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.analysis.attributes.AttributeAnalyzer;
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
    protected final ABI abi;

    /**
     * The semantic listener that will be used for generating events that occur
     * during the analysis.
     */
    protected final SemanticListener semanticListener;

    /**
     * Function that mangles names when applied to them. It actually generates
     * a semantic event with mangling request.
     */
    protected final Function<String, String> manglingFunction;

    /**
     * Object responsible for analysis of attributes.
     */
    protected final AttributeAnalyzer attributeAnalyzer;

    protected AstBuildingBase(NescEntityEnvironment nescEnvironment,
                              ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder,
                              ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder,
                              SemanticListener semanticListener, AttributeAnalyzer attributeAnalyzer,
                              ABI abi) {
        this.nescEnvironment = nescEnvironment;
        this.errorHelper = new ErrorHelper(issuesMultimapBuilder);
        this.tokensMultimapBuilder = tokensMultimapBuilder;
        this.semanticListener = semanticListener;
        this.manglingFunction = new Function<String, String>() {
            @Override
            public String apply(String unmangledName) {
                return AstBuildingBase.this.semanticListener.nameManglingRequired(unmangledName);
            }
        };
        this.attributeAnalyzer = attributeAnalyzer;
        this.abi = abi;
    }

}
