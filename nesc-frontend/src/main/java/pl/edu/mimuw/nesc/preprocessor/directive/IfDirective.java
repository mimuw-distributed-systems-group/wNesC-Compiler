package pl.edu.mimuw.nesc.preprocessor.directive;

/**
 * <p>If directive.</p>
 *
 * <p>
 * <pre>
 *     #if expression
 *     controlled text
 *     #endif
 * </pre>
 * </p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class IfDirective extends ConditionalExpDirective {

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates preprocessor directive from builder parameters.
     *
     * @param builder builder.
     */
    protected IfDirective(Builder builder) {
        super(builder);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * If builder.
     */
    public static class Builder extends ConditionalExpDirective.Builder<IfDirective> {

        @Override
        protected void verify() {
            super.verify();
        }

        @Override
        protected IfDirective getInstance() {
            return new IfDirective(this);
        }
    }
}
