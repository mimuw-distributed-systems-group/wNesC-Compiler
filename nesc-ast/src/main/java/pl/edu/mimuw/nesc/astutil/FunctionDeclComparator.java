package pl.edu.mimuw.nesc.astutil;

import java.util.Comparator;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Comparator that compares AST nodes of functions in ascending order by their
 * unique names.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class FunctionDeclComparator implements Comparator<FunctionDecl> {
    @Override
    public int compare(FunctionDecl function1, FunctionDecl function2) {
        checkNotNull(function1, "the first function cannot be null");
        checkNotNull(function2, "the second function cannot be null");

        final String funUniqueName1 = DeclaratorUtils.getUniqueName(function1.getDeclarator()).get(),
                funUniqueName2 = DeclaratorUtils.getUniqueName(function2.getDeclarator()).get();
        return funUniqueName1.compareTo(funUniqueName2);
    }
}
