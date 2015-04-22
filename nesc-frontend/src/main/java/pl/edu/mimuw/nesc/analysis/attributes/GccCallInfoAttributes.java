package pl.edu.mimuw.nesc.analysis.attributes;

import com.google.common.base.Predicate;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.astutil.predicates.AttributePredicates;
import pl.edu.mimuw.nesc.problem.issue.ErroneousIssue;
import pl.edu.mimuw.nesc.problem.issue.InvalidCallInfoAttributeUsageError;

/**
 * <p>GCC call information attributes.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class GccCallInfoAttributes implements CallInfoAttributes {
    @Override
    public Predicate<Attribute> getPredicateSpontaneous() {
        return AttributePredicates.getSpontaneousGccPredicate();
    }

    @Override
    public Predicate<Attribute> getPredicateHwevent() {
        return AttributePredicates.getHweventGccPredicate();
    }

    @Override
    public Predicate<Attribute> getPredicateAtomicHwevent() {
        return AttributePredicates.getAtomicHweventGccPredicate();
    }

    @Override
    public Predicate<Attribute> getPredicateNoParameters() {
        return AttributePredicates.getNoParametersGccPredicate();
    }

    @Override
    public ErroneousIssue newParametersPresentError(String attributeName) {
        return InvalidCallInfoAttributeUsageError.gccParametersPresent(attributeName);
    }

    @Override
    public ErroneousIssue newAppliedNotToFunctionError(String attributeName) {
        return InvalidCallInfoAttributeUsageError.gccAppliedNotToFunction(attributeName);
    }
}
