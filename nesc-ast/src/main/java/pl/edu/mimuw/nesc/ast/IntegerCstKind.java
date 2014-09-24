package pl.edu.mimuw.nesc.ast;

/**
 * Kind of an integer constant. It depends on its prefix.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public enum IntegerCstKind {
    /**
     * Kind of a decimal constant, e.g.: <code>1</code>, <code>997</code>.
     */
    DECIMAL(10),

    /**
     * Kind of an octal constant, e.g.: <code>0777</code>, <code>0537</code>.
     */
    OCTAL(8),

    /**
     * Kind of an hexadecimal constant, e.g.: <code>0Xffff</code>,
     * <code>0x782AF</code>.
     */
    HEXADECIMAL(16);

    private final int radix;

    private IntegerCstKind(int radix) {
        this.radix = radix;
    }

    /**
     * Get the radix associated with this integer constant kind.
     *
     * @return The radix associated with this integer constant kind.
     */
    public int getRadix() {
        return radix;
    }
}
