package pl.edu.mimuw.nesc.symboltable;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.declaration.Declaration;

import java.util.Map;
import java.util.Set;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public interface SymbolTable<T extends Declaration> {

    /**
     * Puts the declaration into the table in the current scope. It is assumed
     * that declaration comes from current file.
     *
     * @param name declaration's name
     * @param item declaration
     * @return <code>false</code> if the given identifier is already defined in
     * the current scope (the given identifier will not be put into the table),
     * <code>true</code> otherwise
     */
    boolean add(String name, T item);

    /**
     * Puts the declaration into the table in the current scope.
     *
     * @param name            declaration's name
     * @param item            declaration
     * @param fromCurrentFile indicates whether the declaration was defined in
     *                        current file or comes from e.g. included file
     * @return <code>false</code> if the given identifier is already defined in
     * the current scope (the given identifier will not be put into the table),
     * <code>true</code> otherwise
     */
    boolean add(String name, T item, boolean fromCurrentFile);

    /**
     * Returns the declaration of the given name. If the current scope does not
     * contain the identifier, it searches in the parent scope.
     *
     * @param name declaration's name
     * @return declaration if present, <code>Optional.absent()</code>
     * otherwise
     */
    Optional<? extends T> get(String name);

    /**
     * Returns the declaration of the given name. If the current scope does not
     * contain the identifier, it searches in the parent scope, if
     * <code>onlyCurrentScope</code> is <code>true</code>.
     *
     * @param name             declaration's name
     * @param onlyCurrentScope indicates whether only current scope should be
     *                         searched
     * @return declaration if present, <code>Optional.absent()</code>
     * otherwise
     */
    Optional<? extends T> get(String name, boolean onlyCurrentScope);

    /**
     * Returns all declarations in the symbol table.
     *
     * @return declarations
     */
    Set<Map.Entry<String, T>> getAll();

    /**
     * Returns all declarations but only those defined in the current file.
     *
     * @return declarations
     */
    Set<Map.Entry<String, T>> getAllFromFile();

    /**
     * Checks if the table contains the given identifier. If the current scope
     * does not contain the identifier, it searches in the parent scope.
     *
     * @param name declaration's name
     * @return <code>true</code> if the table contains the given identifier
     */
    boolean contains(String name);

    /**
     * Checks if the table contains the given identifier. If the current scope
     * does not contain the identifier, it searches in the parent scope, if
     * <code>onlyCurrentScope</code> is <code>true</code>.
     *
     * @param name             declaration's name
     * @param onlyCurrentScope indicates whether only current scope should be
     *                         searched
     * @return <code>true</code> if the table contains the given identifier
     */
    boolean contains(String name, boolean onlyCurrentScope);

}
