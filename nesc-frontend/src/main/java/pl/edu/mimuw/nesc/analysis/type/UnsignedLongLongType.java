package pl.edu.mimuw.nesc.analysis.type;

/**
 * Reflects the <code>unsigned long long</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class UnsignedLongLongType extends UnsignedIntegerType {
    public UnsignedLongLongType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }
}
