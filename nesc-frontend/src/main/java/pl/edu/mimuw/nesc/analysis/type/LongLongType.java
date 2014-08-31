package pl.edu.mimuw.nesc.analysis.type;

/**
 * Reflects the <code>long long int</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class LongLongType extends SignedIntegerType {
    public LongLongType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }
}
