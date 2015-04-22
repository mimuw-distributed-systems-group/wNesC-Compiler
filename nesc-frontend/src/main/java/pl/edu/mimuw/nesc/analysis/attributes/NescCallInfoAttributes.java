package pl.edu.mimuw.nesc.analysis.attributes;

import com.google.common.base.Predicate;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.astutil.predicates.AttributePredicates;
import pl.edu.mimuw.nesc.problem.issue.ErroneousIssue;
import pl.edu.mimuw.nesc.problem.issue.InvalidCallInfoAttributeUsageError;

/**
 * <p>NesC call information attributes.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class NescCallInfoAttributes implements CallInfoAttributes {
    @Override
    public Predicate<Attribute> getPredicateSpontaneous() {
        return AttributePredicates.getSpontaneousPredicate();
    }

    @Override
    public Predicate<Attribute> getPredicateHwevent() {
        return AttributePredicates.getHweventPredicate();
    }

    @Override
    public Predicate<Attribute> getPredicateAtomicHwevent() {
        return AttributePredicates.getAtomicHweventPredicate();
    }

    @Override
    public Predicate<Attribute> getPredicateNoParameters() {
        return AttributePredicates.getNoParametersNescPredicate();
    }

    @Override
    public ErroneousIssue newParametersPresentError(String attributeName) {
        return InvalidCallInfoAttributeUsageError.nescParametersPresent(attributeName);
    }

    @Override
    public ErroneousIssue newAppliedNotToFunctionError(String attributeName) {
        return InvalidCallInfoAttributeUsageError.nescAppliedNotToFunction(attributeName);
    }
}
