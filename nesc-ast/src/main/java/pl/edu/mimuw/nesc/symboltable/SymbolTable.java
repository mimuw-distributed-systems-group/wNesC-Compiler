package pl.edu.mimuw.nesc.symboltable;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.declaration.Declaration;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public interface SymbolTable<T extends Declaration> {

    /**
     * Puts declaration into table in current scope.
     *
     * @param name declaration's name
     * @param item declaration
     * @return <code>true</code> if given identifier is already defined in
     * current scope (given identifier will not be put into table),
     * <code>false</code> otherwise
     */
    boolean add(String name, T item);

    /**
     * Returns declarations of given name. If current scope does not
     * contain identifier it searches in parent scope.
     *
     * @param name              declaration's name
     * @return declaration if present, <code>Optional.absent()</code>
     * otherwise
     */
    Optional<? extends T> get(String name);

    /**
     * Checks if table contains given identifier. If current scope does not
     * contain identifier it searches in parent scope.
     *
     * @param name              declaration's name
     * @return <code>true</code> if table contains given identifier
     */
    boolean contains(String name);

}
