package pl.edu.mimuw.nesc.analysis.attributes;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import java.util.List;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.ErrorExpr;
import pl.edu.mimuw.nesc.ast.gen.InitList;
import pl.edu.mimuw.nesc.ast.gen.NescAttribute;
import pl.edu.mimuw.nesc.astutil.predicates.AttributePredicates;
import pl.edu.mimuw.nesc.declaration.Declaration;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.problem.issue.ErroneousIssue;
import pl.edu.mimuw.nesc.problem.issue.InvalidCallInfoAttributeUsageError;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Analyzer responsible for analysis of call assumptions NesC attributes:
 * {@literal @spontaneous()}, {@literal @hwevent()}
 * and {@literal @atomic_hwevent()}.
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

    CallInfoAttributesAnalyzer(ErrorHelper errorHelper) {
        checkNotNull(errorHelper, "error helper cannot be null");
        this.errorHelper = errorHelper;
        this.callInfoPredicate = Predicates.or(ImmutableList.of(
                AttributePredicates.getSpontaneousPredicate(),
                AttributePredicates.getHweventPredicate(),
                AttributePredicates.getAtomicHweventPredicate())
        );
    }

    @Override
    public void analyzeAttribute(List<Attribute> attributes, Declaration declaration, Environment environment) {
        FunctionDeclaration.CallAssumptions strongestAssumptions = FunctionDeclaration.CallAssumptions.NONE;

        // Determine the strongest assumptions specified by attributes

        for (Attribute attribute : attributes) {
            if (callInfoPredicate.apply(attribute)) {
                final FunctionDeclaration.CallAssumptions assumptions =
                        analyzeAttribute((NescAttribute) attribute, declaration);

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

        if (strongestAssumptions.compareTo(funDeclaration.getCallAssumptions()) > 0) {
            funDeclaration.setCallAssumptions(strongestAssumptions);
        }
    }

    private FunctionDeclaration.CallAssumptions analyzeAttribute(NescAttribute attribute, Declaration declaration) {
        final String attributeName = attribute.getName().getName();
        final FunctionDeclaration.CallAssumptions assumptions;

        if (AttributePredicates.getSpontaneousPredicate().apply(attribute)) {
            assumptions = FunctionDeclaration.CallAssumptions.SPONTANEOUS;
        } else if (AttributePredicates.getHweventPredicate().apply(attribute)) {
            assumptions = FunctionDeclaration.CallAssumptions.HWEVENT;
        } else if (AttributePredicates.getAtomicHweventPredicate().apply(attribute)) {
            assumptions = FunctionDeclaration.CallAssumptions.ATOMIC_HWEVENT;
        } else {
            throw new RuntimeException("unexpectedly no call information predicate is fulfilled for attribute '"
                    + attributeName + "'");
        }

        Optional<? extends ErroneousIssue> error = Optional.absent();

        if (!(attribute.getValue() instanceof ErrorExpr)) {
            final InitList initList = (InitList) attribute.getValue();
            if (!initList.getArguments().isEmpty()) {
                error = Optional.of(InvalidCallInfoAttributeUsageError.parametersPresent(
                        attributeName, initList.getArguments().size()));
            }
        }

        if (!error.isPresent() && !(declaration instanceof FunctionDeclaration)) {
            error = Optional.of(InvalidCallInfoAttributeUsageError.appliedNotToFunction(attributeName));
        }

        if (error.isPresent()) {
            errorHelper.error(attribute.getLocation(), attribute.getEndLocation(), error.get());
        }

        return assumptions;
    }
}
