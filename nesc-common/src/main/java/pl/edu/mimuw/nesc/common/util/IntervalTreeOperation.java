package pl.edu.mimuw.nesc.common.util;

/**
 * Interface that models operation performed by an interval tree. It should be
 * associative and commutative.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface IntervalTreeOperation<T> {
    /**
     * Perform the operation on given arguments.
     *
     * @param arg1 The first argument for the operation.
     * @param arg2 The second argument for the operation.
     * @return Result of the operation on given arguments.
     */
    T perform(T arg1, T arg2);

    /**
     * Get the neutral element of the operation.
     *
     * @return Neutral element of the operation.
     */
    T getNeutralElement();
}
