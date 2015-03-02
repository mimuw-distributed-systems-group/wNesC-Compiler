package pl.edu.mimuw.nesc.abi;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Assumptions about semantics of GCC attributes <code>signal</code> and
 * <code>interrupt</code>.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class AttributesAssumptions {
    private final InterruptSemantics semanticsInterrupt;
    private final InterruptSemantics semanticsSignal;
    private final PreferentialAttribute preferentialAttribute;

    AttributesAssumptions(InterruptSemantics semanticsInterrupt,
            InterruptSemantics semanticsSignal,
            PreferentialAttribute preferentialAttribute) {
        checkNotNull(semanticsInterrupt, "interrupt attribute semantics cannot be null");
        checkNotNull(semanticsSignal, "signal attribute semantics cannot be null");
        checkNotNull(preferentialAttribute, "preferential attribute cannot be null");
        this.semanticsInterrupt = semanticsInterrupt;
        this.semanticsSignal = semanticsSignal;
        this.preferentialAttribute = preferentialAttribute;
    }

    /**
     * Get the atomicity semantics of GCC attribute 'interrupt'.
     *
     * @return Semantics of 'interrupt' attribute.
     */
    public InterruptSemantics getInterruptSemantics() {
        return semanticsInterrupt;
    }

    /**
     * Get the atomicity semantics of GCC attribute 'signal'.
     *
     * @return Semantics of 'signal' attribute.
     */
    public InterruptSemantics getSignalSemantics() {
        return semanticsSignal;
    }

    /**
     * Get the value indicating which attribute indicates the semantics if both
     * are applied to an interrupt handler.
     *
     * @return Preferential attribute that indicates the semantics if both
     *         both attributes are used.
     */
    public PreferentialAttribute getPreferentialAttribute() {
        return preferentialAttribute;
    }

    public enum InterruptSemantics {
        NORMAL, // corresponds to @hwevent() attribute
        ATOMIC, // corresponds to @atomic_hwevent() attribute
    }

    public enum PreferentialAttribute {
        SIGNAL,
        INTERRUPT,
    }
}
