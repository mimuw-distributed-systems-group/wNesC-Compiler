package pl.edu.mimuw.nesc.astutil;

import com.google.common.base.Supplier;
import java.util.List;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;

/**
 * Interface for adjusting type elements that allows its preservation by
 * {@link TypeElementsPreserver}.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface TypeElementsAdjuster {
    /**
     * Adjust type elements contained in the given modifiers list for
     * a forward declaration of a function.
     *
     * @param specifiers Unmodifiable list of specifiers contained in the
     *                   DataDecl AST node to adjust. Contents of the list may
     *                   change after modification of the list returned by the
     *                   supplier.
     * @param substituteSupplier Supplier of list of specifiers that can be
     *                           safely modified. Initially, its contents are
     *                           exactly the same as <code>specifiers</code>. If
     *                           {@link Supplier#get} is called, then original
     *                           specifiers will be replaced.
     * @param uniqueName Unique name of the function to adjust.
     * @param declarationObj Declaration object that depicts the function whose
     *                       declaration is to be adjusted.
     */
    void adjustFunctionDeclaration(List<TypeElement> specifiers, Supplier<List<TypeElement>> substituteSupplier,
            String uniqueName, FunctionDeclaration declarationObj);

    /**
     * Adjust type elements contained in the given modifiers list for
     * a function definition.
     *
     * @param specifiers Unmodifiable list of modifiers contained in the
     *                   FunctionDecl AST node to adjust. Contents of the list
     *                   may change after modification of the list returned by
     *                   the supplier.
     * @param substituteSupplier Supplier of list of specifiers that can be
     *                           safely modified. Initially, its contents are
     *                           exactly the same as <code>specifiers</code>. If
     *                           {@link Supplier#get} is called, then original
     *                           specifiers will be replaced.
     * @param uniqueName Unique name of the function to adjust.
     * @param declarationObj Declaration object that depicts the function whose
     *                       declaration is to be adjusted.
     */
    void adjustFunctionDefinition(List<TypeElement> specifiers, Supplier<List<TypeElement>> substituteSupplier,
            String uniqueName, FunctionDeclaration declarationObj);
}
