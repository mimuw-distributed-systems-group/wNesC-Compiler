package pl.edu.mimuw.nesc.astutil.predicates;

import com.google.common.base.Predicate;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.InitList;
import pl.edu.mimuw.nesc.ast.gen.InitSpecific;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Predicate of an expression that is fulfilled if and only if the
 * expression is an initializer list or an initialization designation inside
 * an initializer list. It does not accept <code>null</code> values.</p>
 *
 * <p>The predicate follows the singleton design pattern.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class IsInitializerPredicate implements Predicate<Expression> {
    /**
     * The only instance of this predicate.
     */
    public static final IsInitializerPredicate PREDICATE = new IsInitializerPredicate();

    /**
     * Private constructor to limit its accessibility.
     */
    private IsInitializerPredicate() {
    }

    @Override
    public boolean apply(Expression expr) {
        checkNotNull(expr, "the expression cannot be null");
        return expr instanceof InitList || expr instanceof InitSpecific;
    }
}
