package pl.edu.mimuw.nesc.astutil.predicates;

import com.google.common.base.Predicate;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.attribute.Attributes;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class AttributePredicates {
    /**
     * The instances of NesC attributes predicates.
     */
    private static final NescAttributePredicate PREDICATE_C = new NescAttributePredicate(Attributes.getCAttributeName());
    private static final NescAttributePredicate PREDICATE_SPONTANEOUS = new NescAttributePredicate(Attributes.getSpontaneousAttributeName());
    private static final NescAttributePredicate PREDICATE_HWEVENT = new NescAttributePredicate(Attributes.getHweventAttributeName());
    private static final NescAttributePredicate PREDICATE_ATOMIC_HWEVENT = new NescAttributePredicate(Attributes.getAtomicHweventAttributeName());

    /**
     * Instances of GCC attributes predicates.
     */
    private static final GccAttributePredicate PREDICATE_PACKED = new GccAttributePredicate(Attributes.getPackedAttributeName());
    private static final GccAttributePredicate PREDICATE_INTERRUPT = new GccAttributePredicate(Attributes.getInterruptAttributeName());
    private static final GccAttributePredicate PREDICATE_SIGNAL = new GccAttributePredicate(Attributes.getSignalAttributeName());

    public static Predicate<Attribute> getCPredicate() {
        return PREDICATE_C;
    }

    public static Predicate<Attribute> getSpontaneousPredicate() {
        return PREDICATE_SPONTANEOUS;
    }

    public static Predicate<Attribute> getHweventPredicate() {
        return PREDICATE_HWEVENT;
    }

    public static Predicate<Attribute> getAtomicHweventPredicate() {
        return PREDICATE_ATOMIC_HWEVENT;
    }

    public static Predicate<Attribute> getPackedPredicate() {
        return PREDICATE_PACKED;
    }

    public static Predicate<Attribute> getInterruptPredicate() {
        return PREDICATE_INTERRUPT;
    }

    public static Predicate<Attribute> getSignalPredicate() {
        return PREDICATE_SIGNAL;
    }

    /**
     * Private constructor to prevent this class from being instantiated.
     */
    private AttributePredicates() {
    }
}
