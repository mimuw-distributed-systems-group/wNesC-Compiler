package pl.edu.mimuw.nesc.astutil.predicates;

import com.google.common.base.Predicate;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.ErrorExpr;
import pl.edu.mimuw.nesc.ast.gen.InitList;
import pl.edu.mimuw.nesc.ast.gen.NescAttribute;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Predicate that is fulfilled if and only if the attribute is a NesC
 * attribute that does not contain any parameters.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
class NoParamsNescAttributePredicate implements Predicate<Attribute> {
    @Override
    public boolean apply(Attribute attribute) {
        checkNotNull(attribute, "attribute cannot be null");
        if (!(attribute instanceof NescAttribute)) {
            return false;
        }

        final NescAttribute nescAttribute = (NescAttribute) attribute;

        /* Some parameters are potentially given if an error expression is
           present. */
        return !(nescAttribute.getValue() instanceof ErrorExpr)
                && ((InitList) nescAttribute.getValue()).getArguments().isEmpty();
    }
}
