package pl.edu.mimuw.nesc.astutil.predicates;

import pl.edu.mimuw.nesc.ast.gen.GccAttribute;

/**
 * A GCC attribute predicate that is fulfilled if and only if the attribute is
 * a GCC attribute and it has the name given at construction.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
class GccAttributePredicate extends AttributePredicate<GccAttribute> {
    GccAttributePredicate(String attributeName) {
        super(GccAttribute.class, attributeName);
    }
}
