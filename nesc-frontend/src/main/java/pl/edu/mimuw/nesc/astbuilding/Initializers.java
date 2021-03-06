package pl.edu.mimuw.nesc.astbuilding;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import java.util.LinkedList;
import java.util.List;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.analysis.attributes.AttributeAnalyzer;
import pl.edu.mimuw.nesc.analysis.SemanticListener;
import pl.edu.mimuw.nesc.analysis.expressions.FullExpressionsAnalysis;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.astutil.predicates.IsInitializerPredicate;
import pl.edu.mimuw.nesc.common.util.list.Lists;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.environment.NescEntityEnvironment;
import pl.edu.mimuw.nesc.problem.NescIssue;
import pl.edu.mimuw.nesc.token.Token;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class Initializers extends AstBuildingBase {

    public Initializers(NescEntityEnvironment nescEnvironment,
            ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder,
            ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder,
            SemanticListener semanticListener, AttributeAnalyzer attributeAnalyzer,
            ABI abi) {
        super(nescEnvironment, issuesMultimapBuilder, tokensMultimapBuilder,
                semanticListener, attributeAnalyzer, abi);
    }

    public Designator setInitIndex(Environment environment, Location startLocation,
            Location endLocation, Expression first,  Optional<Expression> last) {

        FullExpressionsAnalysis.analyze(first, environment, abi, errorHelper);
        if (last.isPresent()) {
            FullExpressionsAnalysis.analyze(last.get(), environment, abi, errorHelper);
        }

        final Designator designator = new DesignateIndex(startLocation, first, last);
        designator.setEndLocation(endLocation);
        return designator;
    }

    public Designator setInitLabel(Location startLocation, Location endLocation, String fieldName) {
        final DesignateField designator = new DesignateField(startLocation, fieldName);
        designator.setEndLocation(endLocation);
        return designator;
    }

    public Expression makeInitSpecific(Environment environment, Location startLocation,
            Location endLocation, LinkedList<Designator> designators, Expression initialValue) {

        analyzeInitExpressions(initialValue, environment);
        final InitSpecific result = new InitSpecific(startLocation, designators, initialValue);
        result.setEndLocation(endLocation);
        return result;
    }

    public InitSpecific makeInitSpecific(Environment environment, Location startLocation,
            Location endLocation, Designator designator, Expression initialValue) {

        analyzeInitExpressions(initialValue, environment);
        final InitSpecific result = new InitSpecific(startLocation, Lists.newList(designator), initialValue);
        result.setEndLocation(endLocation);
        return result;
    }

    public InitList makeInitList(Environment environment, Location startLocation, Location endLocation,
            LinkedList<Expression> expressions) {

        analyzeInitExpressions(expressions, environment);
        final InitList initList = new InitList(startLocation, expressions);
        initList.setEndLocation(endLocation);
        return initList;
    }

    private void analyzeInitExpressions(Expression expr, Environment environment) {
        if (!IsInitializerPredicate.PREDICATE.apply(expr)) {
            FullExpressionsAnalysis.analyze(expr, environment, abi, errorHelper);
        }
    }

    private void analyzeInitExpressions(List<? extends Expression> expressions, Environment environment) {
        for (Expression expr : expressions) {
            if (!IsInitializerPredicate.PREDICATE.apply(expr)) {
                FullExpressionsAnalysis.analyze(expr, environment, abi, errorHelper);
            }
        }
    }
}
