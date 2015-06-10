package pl.edu.mimuw.nesc.codepartition;

import com.google.common.collect.Range;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.codesize.CodeSizeEstimation;
import pl.edu.mimuw.nesc.refsgraph.ReferencesGraph;

/**
 * <p>Interface with operations for dividing functions into multiple code
 * banks.</p>
 *
 * <p>The model of banks is as follows. There are N banks. Each bank has a name.
 * There is exactly one common bank, i.e. a bank that is always available during
 * execution of the program. Each bank has capacity measured in bytes that is
 * specified by a positive integer or zero. A partition of functions into banks
 * is an allocation of each function into exactly one bank such that all
 * functions allocated to the same bank do not exceed its capacity.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface CodePartitioner {
    /**
     * Get the bank schema used by this code partitioner.
     *
     * @return Bank schema assumed by this code partitioner.
     */
    BankSchema getBankSchema();

    /**
     * Partition given functions into memory banks according to the bank schema
     * of this partitioner. The bank schema should be specified at its
     * construction.
     *
     * @param functions Iterable with functions to partition.
     * @param sizesEstimation Object with estimation of functions sizes.
     * @param refsGraph References graph between entities in the program.
     * @return Bank table that contains all function from the given list assigned
     * @throws PartitionImpossibleException It is not possible to partition the
     *                                      given functions, e.g. sizes of all
     *                                      functions exceed capacity of all
     *                                      banks.
     */
    BankTable partition(Iterable<FunctionDecl> functions, CodeSizeEstimation sizesEstimation,
            ReferencesGraph refsGraph) throws PartitionImpossibleException;
}
