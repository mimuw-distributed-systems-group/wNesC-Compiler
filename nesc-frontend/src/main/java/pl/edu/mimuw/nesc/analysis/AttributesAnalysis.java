package pl.edu.mimuw.nesc.analysis;

import com.google.common.base.Optional;
import java.util.List;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.AttributeRef;
import pl.edu.mimuw.nesc.ast.gen.EndPoint;
import pl.edu.mimuw.nesc.ast.gen.TagRef;
import pl.edu.mimuw.nesc.ast.util.CAttributePredicate;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.environment.ScopeType;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.problem.issue.ErroneousIssue;
import pl.edu.mimuw.nesc.problem.issue.InvalidCAttributeUsageError;

import static com.google.common.base.Preconditions.checkNotNull;
import static pl.edu.mimuw.nesc.analysis.SpecifiersAnalysis.SpecifiersSet;
import static pl.edu.mimuw.nesc.analysis.SpecifiersAnalysis.NonTypeSpecifier.*;

/**
 * <p>Class responsible for analysis of the attributes placed in the code.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class AttributesAnalysis {
    /**
     * <p>Check if the @C() attribute is applied to the given tag reference and
     * if so, if the usage is correct. If not, the given error helper is
     * informed about detected error.</p>
     *
     * @param tagRef Tag reference with attributes to check.
     * @param environment Environment of the given tag reference.
     * @param errorHelper Object that will be notified about detected errors.
     * @return <code>true</code> if and only if the given tag reference
     *         contains the '@C()' attribute and the usage is correct.
     */
    public static boolean checkCAttribute(TagRef tagRef, Environment environment,
            ErrorHelper errorHelper) {
        checkNotNull(tagRef, "tag reference cannot be null");
        checkNotNull(environment, "environment cannot be null");
        checkNotNull(errorHelper, "error helper cannot be null");

        if (!checkCAttribute(tagRef.getAttributes(), environment,
                new SpecifiersSet(errorHelper), errorHelper)) {
            return false;
        }
        
        // Extract the location of the attribute
        final CAttributePredicate predicate = new CAttributePredicate();
        predicate.apply(tagRef.getAttributes());

        final Optional<? extends ErroneousIssue> error;
        if (tagRef.getName() == null) {
            error = Optional.of(InvalidCAttributeUsageError.appliedToUnnamedTag());
        } else if (tagRef instanceof AttributeRef) {
            error = Optional.of(InvalidCAttributeUsageError.appliedToAttribute());
        } else {
            error = Optional.absent();
        }

        if (error.isPresent()) {
            errorHelper.error(predicate.getStartLocation().get(), predicate.getEndLocation().get(),
                    error.get());
            return false;
        }

        return true;
    }

    /**
     * <p>Check if the @C() attribute is contained on the list of attributes and
     * if so, if the usage is correct. If it is not, the given error helper is
     * notified about detected errors.</p>
     *
     * @param attributes List with attributes to check.
     * @param environment Environment of appearance of attributes.
     * @param specifiers Specifiers used for the declaration with given
     *                   attributes.
     * @param errorHelper Object that will be notified about detected errors.
     * @return <code>true</code> if and only if the given list contains the
     *         '@C()' attribute and its usage is correct.
     * @throws NullPointerException One of the arguments is <code>null</code>.
     */
    public static boolean checkCAttribute(List<? extends Attribute> attributes, Environment environment,
            SpecifiersSet specifiers, ErrorHelper errorHelper) {
        checkNotNull(attributes, "list of attributes cannot be null");
        checkNotNull(environment, "environment cannot be null");
        checkNotNull(errorHelper, "error helper cannot be null");

        final CAttributePredicate predicate = new CAttributePredicate();
        if (!predicate.apply(attributes)) {
            return false;
        }

        final Optional<? extends ErroneousIssue> error;
        if (environment.getScopeType() != ScopeType.MODULE_IMPLEMENTATION) {
            error = Optional.of(InvalidCAttributeUsageError.invalidScope());
        } else if (environment.isEnclosedInGenericNescEntity()) {
            error = Optional.of(InvalidCAttributeUsageError.usageInGenericComponent());
        } else if (specifiers.contains(COMMAND) || specifiers.contains(EVENT) || specifiers.contains(TASK)) {
            error = Optional.of(InvalidCAttributeUsageError.usageForNescObject());
        } else if (specifiers.contains(STATIC)) {
            error = Optional.of(InvalidCAttributeUsageError.internalLinkage());
        } else {
            error = Optional.absent();
        }

        if (error.isPresent()) {
            errorHelper.error(predicate.getStartLocation().get(), predicate.getEndLocation().get(),
                    error.get());
            return false;
        }

        return true;
    }

    /**
     * Private constructor to deny instantiation of this class.
     */
    private AttributesAnalysis() {
    }
}
