package pl.edu.mimuw.nesc.codepartition;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import java.util.List;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;

/**
 * <p>Interface with operations for dividing functions into multiple code
 * banks.</p>
 *
 * <p>The model of banks is as follows. There are N banks. Bank <code>i</code>
 * is capable of holding functions with the total size not greater than
 * <code>s<sub>i</sub></code> bytes where <code>s<sub>i</sub></code> is
 * a positive natural number. A partition is an assignment of each function to
 * a single bank such that the size of all functions in any single bank does not
 * exceed the capacity of the bank.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface CodePartitioner {
    /**
     * Index of the common bank, if the target architecture supports it.
     */
    int INDEX_COMMON_BANK = 0;

    /**
     * Partition given functions into memory banks. The bank with index 0 is
     * considered the common bank, if the target architecture defines it. All
     * information necessary for the partition operation, e.g. the number of
     * code banks, sizes of banks, shall be given to the partitioner in some
     * other way.
     *
     * @param functions List with functions to partition.
     * @param functionsSizes Map with estimations of functions sizes.
     * @return List with sets of functions that are assigned to subsequent
     *         banks. The set in each index specifies functions assigned to
     *         a bank. If the target architecture defines a common bank,
     *         functions in set with index 0 are considered functions for it.
     * @throws PartitionImpossibleException It is not possible to partition the
     *                                      given functions, e.g. sizes of all
     *                                      functions exceed capacity of all
     *                                      banks.
     */
    ImmutableList<ImmutableSet<FunctionDecl>> partition(List<FunctionDecl> functions,
            Map<String, Range<Integer>> functionsSizes) throws PartitionImpossibleException;
}
