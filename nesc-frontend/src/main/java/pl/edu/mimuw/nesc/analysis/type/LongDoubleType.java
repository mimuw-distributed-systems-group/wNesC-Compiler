package pl.edu.mimuw.nesc.analysis.type;

/**
 * Reflects the <code>long double</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class LongDoubleType extends FloatingType {
    public LongDoubleType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }
}
