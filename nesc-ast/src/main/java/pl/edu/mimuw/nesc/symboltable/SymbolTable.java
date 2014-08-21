package pl.edu.mimuw.nesc.symboltable;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import pl.edu.mimuw.nesc.declaration.Declaration;

import java.util.Map;
import java.util.Set;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
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
     * If the given name does not exist in the current scope, unconditionally
     * adds it to it. However, if it exists, the given item is associated with
     * the given name if and only if the given predicate is satisfied by the
     * current object in the table.
     *
     * @param name      Name of the declaration.
     * @param item      Object to be associated with the given name.
     * @param predicate Predicate that must be satisfied to overwrite the
     *                  current object associated with the given name if it
     *                  exists.
     * @return <code>true</code> if and only if a change in this table has been
     * made.
     */
    boolean addOrOverwriteIf(String name, T item, Predicate<T> predicate);

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

    /**
     * Checks if the object associated with the given identifier in the table
     * satisfies the given predicate. If the current scope does not contain the
     * identifier, it searches in the parent scope.
     *
     * @param name      Name associated with the object to test.
     * @param predicate Predicate to test on the object associated with the
     *                  given name.
     * @return <code>Optional.absent()</code> if the identifier is absent in the
     * table. If it is present, a logical value wrapped in
     * <code>Optional</code> that specifies if it satisfies the given
     * predicate.
     */
    Optional<Boolean> test(String name, Predicate<T> predicate);

    /**
     * Checks if the object associated with given name satisfies the given
     * predicate. Firstly, it looks for the object in the current scope. Then,
     * if the object is not found and <code>onlyCurrentScope</code> is
     * <code>false</code>, it searches in the parent scope.
     *
     * @param name      Name associated with the object to test.
     * @param predicate Predicate to test on the object associated with the
     *                  given name.
     * @return If no object with the given name is found,
     * <code>Optional.absent()</code>. Otherwise, a boolean value
     * wrapped by <code>Optional</code> that is <code>true</code> if and
     * only if the object satisfies the given predicate.
     */
    Optional<Boolean> test(String name, Predicate<T> predicate, boolean onlyCurrentScope);
}
