package pl.edu.mimuw.nesc.typelayout;

/**
 * <p>Operations of a layout calculator. This interface is created rather for
 * semantic and documentation purposes instead of usage with polymorphism.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface TypeLayoutCalculator {
    /**
     * Calculates the layout of the type specified at the construction of the
     * object and makes all side effects that are assumed by the calculation.
     * These depend on the implementing class. If this method succeeds without
     * throwing an exception, it always returns the same instance.
     *
     * @return Layout of the type that has been calculated.
     */
    TypeLayout calculate();
}
