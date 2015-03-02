package pl.edu.mimuw.nesc.analysis.attributes;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.util.List;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.abi.AttributesAssumptions;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.astutil.predicates.AttributePredicates;
import pl.edu.mimuw.nesc.declaration.Declaration;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.environment.Environment;

import static com.google.common.base.Preconditions.checkNotNull;

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
     * Predicate of preferential attribute and value implied by it.
     */
    private final Predicate<Attribute> preferentialPredicate;
    private final FunctionDeclaration.CallAssumptions preferentialValue;

    /**
     * Predicate of the other attribute and the value implied by it.
     */
    private final Predicate<Attribute> otherPredicate;
    private final FunctionDeclaration.CallAssumptions otherValue;

    GccInterruptAttributesAnalyzer(ABI abi) {
        checkNotNull(abi, "ABI cannot be null");

        final FunctionDeclaration.CallAssumptions assumptionsSignal =
                getCallAssumptions(abi.getAttributesAssumptions().getSignalSemantics());
        final FunctionDeclaration.CallAssumptions assumptionsInterrupt =
                getCallAssumptions(abi.getAttributesAssumptions().getInterruptSemantics());
        final Predicate<Attribute> predicateSignal = AttributePredicates.getSignalPredicate();
        final Predicate<Attribute> predicateInterrupt = AttributePredicates.getInterruptPredicate();

        switch (abi.getAttributesAssumptions().getPreferentialAttribute()) {
            case SIGNAL:
                this.preferentialPredicate = predicateSignal;
                this.otherPredicate = predicateInterrupt;
                this.preferentialValue = assumptionsSignal;
                this.otherValue = assumptionsInterrupt;
                break;
            case INTERRUPT:
                this.preferentialPredicate = predicateInterrupt;
                this.otherPredicate = predicateSignal;
                this.preferentialValue = assumptionsInterrupt;
                this.otherValue = assumptionsSignal;
                break;
            default:
                throw new RuntimeException("unexpected preferential attribute "
                        + abi.getAttributesAssumptions().getPreferentialAttribute());
        }
    }

    private static FunctionDeclaration.CallAssumptions getCallAssumptions(AttributesAssumptions.InterruptSemantics interruptSemantics) {
        switch (interruptSemantics) {
            case ATOMIC:
                return FunctionDeclaration.CallAssumptions.ATOMIC_HWEVENT;
            case NORMAL:
                return FunctionDeclaration.CallAssumptions.HWEVENT;
            default:
                throw new RuntimeException("unexpected interrupt semantics "
                        + interruptSemantics);
        }
    }

    @Override
    public void analyzeAttribute(List<Attribute> attributes, Declaration declaration, Environment environment) {
        if (!(declaration instanceof FunctionDeclaration)) {
            return;
        }

        final Optional<FunctionDeclaration.CallAssumptions> newAssumptions;

        if (Iterables.any(attributes, preferentialPredicate)) {
            newAssumptions = Optional.of(preferentialValue);
        } else if (Iterables.any(attributes, otherPredicate)) {
            newAssumptions = Optional.of(otherValue);
        } else {
            newAssumptions = Optional.absent();
        }

        if (newAssumptions.isPresent()) {
            final FunctionDeclaration funDeclaration = (FunctionDeclaration) declaration;
            if (newAssumptions.get().compareTo(funDeclaration.getMinimalCallAssumptions()) >= 0) {
                funDeclaration.setCallAssumptions(newAssumptions.get());
            }
        }
    }
}
