package pl.edu.mimuw.nesc.symboltable;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.declaration.label.LabelDeclaration;

/**
 * <p>Interface with additional operations for a table for labels. These are
 * to support local labels and nested functions GCC extensions.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface LabelSymbolTable<T extends LabelDeclaration> extends SymbolTable<T> {
    /**
     * Adds information about a normal label to the symbol table. The object is
     * added to the last parent symbol table that corresponds to the same
     * function as this symbol table, i.e. to the symbol table created for the
     * body of the function. It is added only if there is no object currently
     * associated with the given name in the symbol table.
     *
     * @param name Name of the label to add.
     * @param item Declaration object to be associated with the given name.
     * @return <code>true</code> if and only if a symbol table has been
     *         modified by this method.
     */
    boolean addFunctionScopeLabel(String name, T item);

    /**
     * Looks for a label declaration with given name. If the this symbol
     * table does not contain the label and the parent symbol table is present
     * and corresponds to the same function as this, it searches in the parent
     * scope.
     *
     * @param name Name of the label to look for.
     * @return Declaration object for a label with given name.
     */
    Optional<? extends T> getFromCurrentFunction(String name);

    /**
     * The first symbol table that is searched is the first parent symbol table
     * that corresponds to an outer function. Then, the lookup continues and
     * stops at the first local label that is found.
     *
     * @param name Name of the label to look for.
     * @return Declaration object for a label with given name.
     */
    Optional<? extends T> getLocalFromOuterFunctions(String name);
}
