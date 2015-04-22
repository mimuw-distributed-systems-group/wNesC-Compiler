package pl.edu.mimuw.nesc.analysis.attributes;

import com.google.common.base.Predicate;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.problem.issue.ErroneousIssue;

/**
 * <p>Interface that allows getting information about a set of call information
 * attributes.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
interface CallInfoAttributes {
    Predicate<Attribute> getPredicateSpontaneous();
    Predicate<Attribute> getPredicateHwevent();
    Predicate<Attribute> getPredicateAtomicHwevent();
    Predicate<Attribute> getPredicateNoParameters();
    ErroneousIssue newParametersPresentError(String attributeName);
    ErroneousIssue newAppliedNotToFunctionError(String attributeName);
}
