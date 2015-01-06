package pl.edu.mimuw.nesc.astutil;

import com.google.common.base.Optional;
import java.util.List;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.Identifier;
import pl.edu.mimuw.nesc.ast.gen.IdentityVisitor;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ExpressionUtils {
    /**
     * Visitor that sets the unique name in visited identifiers.
     */
    private static final UniqueNameSetVisitor UNIQUE_NAME_SET_VISITOR = new UniqueNameSetVisitor();

    /**
     * Sets the unique name of all identifiers contained in the given expression
     * to the given unique name.
     *
     * @param expr Expression with potential identifiers.
     * @param uniqueName Unique name to set.
     */
    public static void setUniqueNameDeep(Expression expr, Optional<String> uniqueName) {
        checkNotNull(expr, "expression cannot be null");
        checkNotNull(uniqueName, "unique name cannot be null");
        expr.traverse(UNIQUE_NAME_SET_VISITOR, uniqueName);
    }

    /**
     * Set the unique name of all identifiers contained in all expressions from
     * the given list to the given unique name.
     *
     * @param exprs List with expressions to modify identifiers in.
     * @param uniqueName Unique name to set.
     */
    public static void setUniqueNameDeep(List<? extends Expression> exprs, Optional<String> uniqueName) {
        checkNotNull(exprs, "list of expressions cannot be null");

        for (Expression expr : exprs) {
            setUniqueNameDeep(expr, uniqueName);
        }
    }

    private ExpressionUtils() {
    }

    private static final class UniqueNameSetVisitor extends IdentityVisitor<Optional<String>> {
        @Override
        public Optional<String> visitIdentifier(Identifier identifier, Optional<String> uniqueName) {
            identifier.setUniqueName(uniqueName);
            return uniqueName;
        }
    }
}
