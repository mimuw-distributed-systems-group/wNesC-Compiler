package pl.edu.mimuw.nesc.preprocessor.directive;

/**
 * <p>Ifdef directive.</p>
 * <p/>
 * <p>
 * <pre>
 *     #ifdef MACRO
 *     controlled text
 *     #endif
 * </pre>
 * </p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class IfdefDirective extends ConditionalMacroDirective {

    public static final String NAME = "ifdef";

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates preprocessor directive from builder parameters.
     *
     * @param builder builder.
     */
    protected IfdefDirective(Builder builder) {
        super(builder);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * If builder.
     */
    public static class Builder extends ConditionalMacroDirective.Builder<IfdefDirective> {

        @Override
        protected void verify() {
            super.verify();
        }

        @Override
        protected IfdefDirective getInstance() {
            return new IfdefDirective(this);
        }
    }
}
