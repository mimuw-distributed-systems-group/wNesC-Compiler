package pl.edu.mimuw.nesc.astutil.predicates;

import pl.edu.mimuw.nesc.ast.gen.NescAttribute;

/**
 * Class that allows creating predicates for NesC attributes. The predicate is
 * fulfilled if and only if the attribute is a NesC attribute and its name is
 * the same as the one given at construction.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
class NescAttributePredicate extends AttributePredicate<NescAttribute> {
    NescAttributePredicate(String attributeName) {
        super(NescAttribute.class, attributeName);
    }
}
