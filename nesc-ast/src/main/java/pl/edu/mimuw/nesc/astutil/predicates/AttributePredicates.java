package pl.edu.mimuw.nesc.astutil.predicates;

import com.google.common.base.Predicate;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.attribute.Attributes;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class AttributePredicates {
    /**
     * The instances of predicates.
     */
    private static final Predicate<Attribute> PREDICATE_C = new NescAttributePredicate(Attributes.getCAttributeName());
    private static final Predicate<Attribute> PREDICATE_SPONTANEOUS = new NescAttributePredicate(Attributes.getSpontaneousAttributeName());
    private static final Predicate<Attribute> PREDICATE_HWEVENT = new NescAttributePredicate(Attributes.getHweventAttributeName());
    private static final Predicate<Attribute> PREDICATE_ATOMIC_HWEVENT = new NescAttributePredicate(Attributes.getAtomicHweventAttributeName());

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

    /**
     * Private constructor to prevent this class from being instantiated.
     */
    private AttributePredicates() {
    }
}
