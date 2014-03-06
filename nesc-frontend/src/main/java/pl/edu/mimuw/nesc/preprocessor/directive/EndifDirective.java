package pl.edu.mimuw.nesc.preprocessor.directive;

/**
 * <p>Endif directive.</p>
 *
 * <p>
 * <pre>
 *     #if expression
 *     text-if-true
 *     #else
 *     text-if-false
 *     #endif
 * </pre>
 * </p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class EndifDirective extends ConditionalDirective {

    public static final String NAME = "endif";
    private static final int NAME_LENGTH = NAME.length();

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates preprocessor directive from builder parameters.
     *
     * @param builder builder.
     */
    protected EndifDirective(Builder builder) {
        super(builder);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * If builder.
     */
    public static class Builder extends ConditionalDirective.Builder<EndifDirective> {

        @Override
        protected void verify() {
            super.verify();
        }

        @Override
        protected EndifDirective getInstance() {
            return new EndifDirective(this);
        }
    }
}
