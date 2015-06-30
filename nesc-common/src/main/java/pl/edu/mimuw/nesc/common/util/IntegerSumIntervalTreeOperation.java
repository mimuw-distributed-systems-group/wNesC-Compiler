package pl.edu.mimuw.nesc.common.util;

/**
 * Operation for an interval tree that is simply the arithmetic sum of integers.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class IntegerSumIntervalTreeOperation implements IntervalTreeOperation<Integer> {
    @Override
    public Integer perform(Integer arg1, Integer arg2) {
        return arg1 + arg2;
    }

    @Override
    public Integer getNeutralElement() {
        return 0;
    }
}
