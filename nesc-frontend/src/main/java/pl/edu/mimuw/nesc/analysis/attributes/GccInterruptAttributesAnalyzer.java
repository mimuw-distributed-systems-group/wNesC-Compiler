package pl.edu.mimuw.nesc.analysis.attributes;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.List;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.astutil.predicates.AttributePredicates;
import pl.edu.mimuw.nesc.declaration.Declaration;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.environment.Environment;

/**
 * <p>Attribute analyzer that analyses the GCC attributes 'interrupt' and
 * 'signal'. Correctness checking and errors reporting is left in the case of
 * these attributes to GCC. This analyzer updates information about call
 * assumptions in {@link FunctionDeclaration function declaration objects} by
 * strengthening them to {@link FunctionDeclaration.CallAssumptions#HWEVENT} if
 * the attribute is present.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class GccInterruptAttributesAnalyzer implements AttributeSmallAnalyzer {
    /**
     * Predicate used to test if an attribute is a GCC attribute that marks an
     * interrupt handler.
     */
    private final Predicate<Attribute> predicate = Predicates.or(ImmutableList.of(
            AttributePredicates.getInterruptPredicate(),
            AttributePredicates.getSignalPredicate()
    ));

    @Override
    public void analyzeAttribute(List<Attribute> attributes, Declaration declaration, Environment environment) {
        if (declaration instanceof FunctionDeclaration && Iterables.any(attributes, predicate)) {
            final FunctionDeclaration funDeclaration = (FunctionDeclaration) declaration;
            if (FunctionDeclaration.CallAssumptions.HWEVENT.compareTo(funDeclaration.getCallAssumptions()) > 0) {
                funDeclaration.setCallAssumptions(FunctionDeclaration.CallAssumptions.HWEVENT);
            }
        }
    }
}
