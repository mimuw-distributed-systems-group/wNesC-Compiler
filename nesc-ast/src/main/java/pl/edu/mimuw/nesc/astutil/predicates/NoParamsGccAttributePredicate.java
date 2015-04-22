package pl.edu.mimuw.nesc.astutil.predicates;

import com.google.common.base.Predicate;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.GccAttribute;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Predicate that is fulfilled if and only if the attribute is a GCC
 * attribute and it contains no parameters.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
class NoParamsGccAttributePredicate implements Predicate<Attribute> {
    @Override
    public boolean apply(Attribute attribute) {
        checkNotNull(attribute, "attribute cannot be null");
        return attribute instanceof GccAttribute
            && !((GccAttribute) attribute).getArguments().isPresent();
    }
}
