package pl.edu.mimuw.nesc.astutil.predicates;

import com.google.common.base.Predicate;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.NescAttribute;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class that allows creating predicates for NesC attributes. The predicate is
 * fulfilled if and only if the attribute is a NesC attribute and its name is
 * the same as the one given at construction.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
class NescAttributePredicate implements Predicate<Attribute> {
    /**
     * Name of the NesC attribute that this predicate is fulfilled for.
     */
    private final String attributeName;

    NescAttributePredicate(String attributeName) {
        checkNotNull(attributeName, "attribute name cannot be null");
        checkArgument(!attributeName.isEmpty(), "attribute name cannot be an empty string");

        this.attributeName = attributeName;
    }

    @Override
    public boolean apply(Attribute attribute) {
        checkNotNull(attribute, "attribute cannot be null");
        return attribute instanceof NescAttribute
                && attributeName.equals(attribute.getName().getName());
    }
}
