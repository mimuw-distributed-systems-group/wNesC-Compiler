package pl.edu.mimuw.nesc.analysis.type;

/**
 * Reflects the <code>int</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class IntType extends SignedIntegerType {
    public IntType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }
}
