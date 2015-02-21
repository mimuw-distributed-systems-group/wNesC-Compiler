package pl.edu.mimuw.nesc.astutil.predicates;

import com.google.common.base.Predicate;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.GccAttribute;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>The predicate is fulfilled if and only if the attribute is GCC attribute
 * 'packed'.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class PackedAttributePredicate implements Predicate<Attribute> {
    /**
     * Name of the GCC attribute.
     */
    private static final String ATTRIBUTE_NAME = "packed";

    /**
     * Get the name of the GCC 'packed' attribute.
     *
     * @return String with the name of the attribute.
     */
    public static String getAttributeName() {
        return ATTRIBUTE_NAME;
    }

    @Override
    public boolean apply(Attribute attribute) {
        checkNotNull(attribute, "attribute cannot be null");

        if (!(attribute instanceof GccAttribute)) {
            return false;
        }

        final GccAttribute gccAttribute = (GccAttribute) attribute;
        return ATTRIBUTE_NAME.equals(gccAttribute.getName().getName())
                && !gccAttribute.getArguments().isPresent();
    }
}
