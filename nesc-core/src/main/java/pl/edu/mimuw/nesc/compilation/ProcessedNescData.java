package pl.edu.mimuw.nesc.compilation;

/**
 * <p>Class that represents a NesC file that has been processed by the compiler.
 * It is intended to allow representing a original generic component (along with
 * its external declarations) and an instantiated component that has not got
 * associated <code>FileData object</code>.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 * @see pl.edu.mimuw.nesc.FileData FileData
 */
abstract class ProcessedNescData {
    /**
     * Value indicating if declarations from this data object have been
     * (or currently are) emitted.
     */
    private boolean outputted = false;

    /**
     * Method that allows usage of Visitor pattern.
     *
     * @return Value returned by the given visitor after visiting appropriate
     *         node.
     */
    abstract <R, A> R accept(Visitor<R, A> visitor, A arg);

    /**
     * Get the current value of the 'outputted' flag.
     *
     * @return <code>true</code> if and only if this data object has been
     *         already marked as outputted.
     */
    boolean isOutputted() {
        return outputted;
    }

    /**
     * Set the 'outputted' flag to <code>true</code>.
     */
    void outputted() {
        outputted = true;
    }

    interface Visitor<R, A> {
        R visit(InstantiatedData data, A arg);
        R visit(PreservedData data, A arg);
    }
}
