package pl.edu.mimuw.nesc.analysis.attributes;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import java.util.List;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.declaration.Declaration;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.problem.issue.ErroneousIssue;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Analyzer responsible for analysis of call assumptions attributes.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class CallInfoAttributesAnalyzer implements AttributeSmallAnalyzer {
    /**
     * Object that will be notified about detected errors.
     */
    private final ErrorHelper errorHelper;

    /**
     * Predicate for testing if an attribute is a call information attribute.
     */
    private final Predicate<Attribute> callInfoPredicate;

    /**
     * Information about the set of call info attributes.
     */
    private final CallInfoAttributes callInfoAttributes;

    protected CallInfoAttributesAnalyzer(CallInfoAttributes callInfoAttributes,
            ErrorHelper errorHelper) {
        checkNotNull(callInfoAttributes, "call information attributes cannot be null");
        checkNotNull(errorHelper, "error helper cannot be null");

        this.callInfoAttributes = callInfoAttributes;
        this.errorHelper = errorHelper;
        this.callInfoPredicate = Predicates.or(ImmutableList.of(
                callInfoAttributes.getPredicateSpontaneous(),
                callInfoAttributes.getPredicateHwevent(),
                callInfoAttributes.getPredicateAtomicHwevent()
        ));
    }

    @Override
    public void analyzeAttribute(List<Attribute> attributes, Declaration declaration, Environment environment) {
        FunctionDeclaration.CallAssumptions strongestAssumptions = FunctionDeclaration.CallAssumptions.NONE;

        // Determine the strongest assumptions specified by attributes

        for (Attribute attribute : attributes) {
            if (callInfoPredicate.apply(attribute)) {
                final FunctionDeclaration.CallAssumptions assumptions =
                        analyzeAttribute(attribute, declaration);

                if (assumptions.compareTo(strongestAssumptions) > 0) {
                    strongestAssumptions = assumptions;
                }
            }
        }

        // Update the function declaration object

        if (!(declaration instanceof FunctionDeclaration)) {
            /* Just return, errors have been already reported during analysis of
               individual attributes. */
            return;
        }

        final FunctionDeclaration funDeclaration = (FunctionDeclaration) declaration;

        if (strongestAssumptions.compareTo(funDeclaration.getMinimalCallAssumptions()) > 0) {
            funDeclaration.setMinimalCallAssumptions(strongestAssumptions);
        }
    }

    private FunctionDeclaration.CallAssumptions analyzeAttribute(Attribute attribute, Declaration declaration) {
        final String attributeName = attribute.getName().getName();
        final FunctionDeclaration.CallAssumptions assumptions;

        if (callInfoAttributes.getPredicateSpontaneous().apply(attribute)) {
            assumptions = FunctionDeclaration.CallAssumptions.SPONTANEOUS;
        } else if (callInfoAttributes.getPredicateHwevent().apply(attribute)) {
            assumptions = FunctionDeclaration.CallAssumptions.HWEVENT;
        } else if (callInfoAttributes.getPredicateAtomicHwevent().apply(attribute)) {
            assumptions = FunctionDeclaration.CallAssumptions.ATOMIC_HWEVENT;
        } else {
            throw new RuntimeException("unexpectedly no call information predicate is fulfilled for attribute '"
                    + attributeName + "'");
        }

        final Optional<? extends ErroneousIssue> error;

        if (!callInfoAttributes.getPredicateNoParameters().apply(attribute)) {
            error = Optional.of(callInfoAttributes.newParametersPresentError(attributeName));
        } else if (!(declaration instanceof FunctionDeclaration)) {
            error = Optional.of(callInfoAttributes.newAppliedNotToFunctionError(attributeName));
        } else {
            error = Optional.absent();
        }

        if (error.isPresent()) {
            errorHelper.error(attribute.getLocation(), attribute.getEndLocation(), error.get());
        }

        return assumptions;
    }
}
