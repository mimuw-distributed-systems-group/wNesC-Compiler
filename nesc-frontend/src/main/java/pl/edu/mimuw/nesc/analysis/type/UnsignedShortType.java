package pl.edu.mimuw.nesc.analysis.type;

/**
 * Reflects the <code>unsigned short int</code> type.
 */
public final class UnsignedShortType extends UnsignedIntegerType {
    public UnsignedShortType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }
}
