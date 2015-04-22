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
    private static final NoParamsNescAttributePredicate PREDICATE_NO_PARAMS_NESC = new NoParamsNescAttributePredicate();

    /**
     * Instances of GCC attributes predicates.
     */
    private static final GccAttributePredicate PREDICATE_PACKED = new GccAttributePredicate(Attributes.getPackedAttributeName());
    private static final GccAttributePredicate PREDICATE_INTERRUPT = new GccAttributePredicate(Attributes.getInterruptAttributeName());
    private static final GccAttributePredicate PREDICATE_SIGNAL = new GccAttributePredicate(Attributes.getSignalAttributeName());
    private static final GccAttributePredicate PREDICATE_SPONTANEOUS_GCC = new GccAttributePredicate(Attributes.getSpontaneousAttributeName());
    private static final GccAttributePredicate PREDICATE_HWEVENT_GCC = new GccAttributePredicate(Attributes.getHweventAttributeName());
    private static final GccAttributePredicate PREDICATE_ATOMIC_HWEVENT_GCC = new GccAttributePredicate(Attributes.getAtomicHweventAttributeName());
    private static final NoParamsGccAttributePredicate PREDICATE_NO_PARAMS_GCC = new NoParamsGccAttributePredicate();

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

    public static Predicate<Attribute> getSpontaneousGccPredicate() {
        return PREDICATE_SPONTANEOUS_GCC;
    }

    public static Predicate<Attribute> getHweventGccPredicate() {
        return PREDICATE_HWEVENT_GCC;
    }

    public static Predicate<Attribute> getAtomicHweventGccPredicate() {
        return PREDICATE_ATOMIC_HWEVENT_GCC;
    }

    public static Predicate<Attribute> getNoParametersNescPredicate() {
        return PREDICATE_NO_PARAMS_NESC;
    }

    public static Predicate<Attribute> getNoParametersGccPredicate() {
        return PREDICATE_NO_PARAMS_GCC;
    }

    /**
     * Private constructor to prevent this class from being instantiated.
     */
    private AttributePredicates() {
    }
}
