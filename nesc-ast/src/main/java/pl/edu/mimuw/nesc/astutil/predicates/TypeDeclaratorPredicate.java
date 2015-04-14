package pl.edu.mimuw.nesc.astutil.predicates;

import com.google.common.base.Predicate;
import pl.edu.mimuw.nesc.ast.gen.ArrayDeclarator;
import pl.edu.mimuw.nesc.ast.gen.Declarator;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.gen.PointerDeclarator;

/**
 * <p>A predicate that is fulfilled if and only if the declarator introduces a new
 * type, e.g. it is of one of the following classes:</p>
 * <ul>
 *     <li><code>ArrayDeclarator</code></li>
 *     <li><code>FunctionDeclarator</code></li>
 *     <li><code>PointerDeclarator</code></li>
 * </ul>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class TypeDeclaratorPredicate implements Predicate<Declarator> {
    @Override
    public boolean apply(Declarator declarator) {
        return declarator instanceof ArrayDeclarator
                || declarator instanceof FunctionDeclarator
                || declarator instanceof PointerDeclarator;
    }
}
