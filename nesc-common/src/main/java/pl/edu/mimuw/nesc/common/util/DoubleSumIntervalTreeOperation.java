package pl.edu.mimuw.nesc.common.util;

/**
 * Operation for an interval tree that is the arithmetic sum of floating-point
 * numbers.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class DoubleSumIntervalTreeOperation implements IntervalTreeOperation<Double> {
    @Override
    public Double perform(Double arg1, Double arg2) {
        return arg1 + arg2;
    }

    @Override
    public Double getNeutralElement() {
        return 0.;
    }
}
