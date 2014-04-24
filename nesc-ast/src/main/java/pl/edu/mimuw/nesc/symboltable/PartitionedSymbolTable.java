package pl.edu.mimuw.nesc.symboltable;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.declaration.Declaration;

import java.util.Map;
import java.util.Set;

/**
 * <p>Represent a symbol table which also divide keys into partitions.</p>
 * <p>PartitionedSymbolTable consists only of global scope.</p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public interface PartitionedSymbolTable<T extends Declaration> {

    /**
     * Puts declaration into table in current scope.
     *
     * @param partition partition's name
     * @param name      declaration's name
     * @param item      declaration
     * @return <code>true</code> if given identifier is already defined in
     * current scope (given identifier will not be put into table),
     * <code>false</code> otherwise
     */
    boolean add(Partition partition, String name, T item);

    /**
     * Returns declarations of given name.
     *
     * @param name declaration's name
     * @return declaration if present, <code>Optional.absent()</code>
     * otherwise
     */
    Optional<? extends T> get(String name);

    /**
     * Returns all declarations in current scope.
     *
     * @return declarations
     */
    Set<Map.Entry<String, T>> getAll();

    /**
     * Checks if table contains given identifier.
     *
     * @param name declaration's name
     * @return <code>true</code> if table contains given identifier
     */
    boolean contains(String name);

    /**
     * Creates new partition.
     *
     * @param partition partition
     */
    void createPartition(Partition partition);

    /**
     * Removes given partition and all its declarations from table.
     *
     * @param partition partition
     */
    void removePartition(Partition partition);

}
