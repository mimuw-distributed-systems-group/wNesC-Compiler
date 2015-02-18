package pl.edu.mimuw.nesc.intermediate;

import com.google.common.base.Optional;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.astutil.TypeElementUtils;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class responsible for determining a combining function to use in an
 * intermediate function.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class CombiningFunctionResolver {
    /**
     * Map with unique names of type definitions as keys and names of
     * corresponding combining functions as values.
     */
    private final Map<String, String> combiningFunctions;

    CombiningFunctionResolver(Map<String, String> combiningFunctions) {
        checkNotNull(combiningFunctions, "combining function cannot be null");
        this.combiningFunctions = combiningFunctions;
    }

    /**
     * Get the name of the combining function for the return type of given
     * function.
     *
     * @param funDecl AST node of function.
     * @return Name of the combining function for the return type of given
     *         function. The object is absent if no combining function is
     *         associated with the return type.
     */
    Optional<String> resolve(FunctionDecl funDecl) {
        /* Check if the return type of the function is entirely defined with
           a type elements list. */

        if (AstUtils.declaratorAffectsReturnType(funDecl)) {
            return Optional.absent();
        }

        final Optional<String> typedefUniqueName = TypeElementUtils.getTypedefUniqueName(funDecl.getModifiers());
        return typedefUniqueName.isPresent()
                ? Optional.fromNullable(combiningFunctions.get(typedefUniqueName.get()))
                : Optional.<String>absent();
    }
}
