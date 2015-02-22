package pl.edu.mimuw.nesc.astutil.predicates;

import com.google.common.base.Predicate;
import pl.edu.mimuw.nesc.ast.gen.Attribute;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Predicate that allows testing if an attribute is of particular subclass of
 * <code>Attribute</code> AST node and if it has a specific name. It is
 * fulfilled if and only if these two conditions are fulfilled.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
abstract class AttributePredicate<T extends Attribute> implements Predicate<Attribute> {
    /**
     * Name of the attribute that must match for the predicate to be fulfilled.
     */
    private final String attributeName;

    /**
     * Class of the attribute that is required for the predicate to be
     * fulfilled.
     */
    private final Class<T> classObject;

    protected AttributePredicate(Class<T> classObject, String attributeName) {
        checkNotNull(classObject, "the class object cannot be null");
        checkNotNull(attributeName, "attribute name cannot be null");
        checkArgument(!attributeName.isEmpty(), "attribute name cannot be an empty string");

        this.classObject = classObject;
        this.attributeName = attributeName;
    }

    @Override
    public boolean apply(Attribute attribute) {
        checkNotNull(attribute, "attribute cannot be null");
        return attributeName.equals(attribute.getName().getName())
                && classObject.isInstance(attribute);
    }
}
