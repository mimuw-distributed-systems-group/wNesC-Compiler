package pl.edu.mimuw.nesc.astbuilding.nesc;

import com.google.common.collect.ImmutableListMultimap;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.analysis.attributes.AttributeAnalyzer;
import pl.edu.mimuw.nesc.analysis.SemanticListener;
import pl.edu.mimuw.nesc.analysis.expressions.FullExpressionsAnalysis;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.InitList;
import pl.edu.mimuw.nesc.ast.gen.NescAttribute;
import pl.edu.mimuw.nesc.ast.gen.Word;

import java.util.LinkedList;
import pl.edu.mimuw.nesc.astbuilding.AstBuildingBase;
import pl.edu.mimuw.nesc.astutil.predicates.IsInitializerPredicate;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.environment.NescEntityEnvironment;
import pl.edu.mimuw.nesc.problem.NescIssue;
import pl.edu.mimuw.nesc.token.Token;

import static pl.edu.mimuw.nesc.astutil.AstUtils.getEndLocation;
import static pl.edu.mimuw.nesc.astutil.AstUtils.getStartLocation;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class NescAttributes extends AstBuildingBase {

    public NescAttributes(NescEntityEnvironment nescEnvironment,
            ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder,
            ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder,
            SemanticListener semanticListener, AttributeAnalyzer attributeAnalyzer,
            ABI abi) {
        super(nescEnvironment, issuesMultimapBuilder, tokensMultimapBuilder,
                semanticListener, attributeAnalyzer, abi);
    }

    public NescAttribute startAttributeUse(Word name) {
        final NescAttribute attr = new NescAttribute(null, name, null);

        /*
         * TODO: there is a nice trick to handle error recovery
         * see: nesc-attributes.c#start_attribute_use
         *
         * Create new environment so that we can track whether this is a
         * deputy scope or not. Using an environment makes it easy to
         * recover parsing errors: we just call poplevel in the appropriate
         * error production (see nattrib rules in c-parse.y).
         */

        return attr;
    }

    public NescAttribute finishAttributeUse(Environment environment,
            Location startLocation, Location endLocation, NescAttribute attribute,
            LinkedList<Expression> initializer) {

        final Expression initList;
        if (initializer.isEmpty()) {
            initList = new InitList(Location.getDummyLocation(), initializer);
        } else {
            final Location initStartLocation = getStartLocation(initializer).get();
            final Location initEndLocation = getEndLocation(initializer).get();

            initList = new InitList(initStartLocation, initializer);
            initList.setEndLocation(initEndLocation);
        }

        for (Expression expr : initializer) {
            if (!IsInitializerPredicate.PREDICATE.apply(expr)) {
                FullExpressionsAnalysis.analyze(expr, environment, abi, errorHelper);
            }
        }

        attribute.setValue(initList);
        attribute.setLocation(startLocation);
        attribute.setEndLocation(endLocation);
        return attribute;
    }
}
