package pl.edu.mimuw.nesc.codepartition;

/**
 * The algorithm that will be used for allocation to the common bank
 * performed as the first step by the heuristic.
 *
 * @author Michał Ciszewski <mc305195@students.mimuw.edu.pl>
 */
public enum CommonBankAllocationAlgorithm {
    /**
     * A greedy algorithm that does not guarantee any approximation factor.
     * Functions are sequenced in the descending order of their frequency
     * estimation and assigned in this order. A function is assigned if and
     * only if it fits the common bank.
     */
    GREEDY_DESCENDING_ESTIMATIONS,

    /**
     * A 2-approximation algorithm for the knapsack problem: it guarantees
     * that the sum of frequency estimations of functions allocated to the
     * common bank by the algorithm is at most two times less than in the
     * optimal allocation.
     */
    TWO_APPROXIMATION,

    /**
     * An algorithm that does not allocate any functions to the common
     * bank: it does nothing.
     */
    NO_OPERATION,
}
