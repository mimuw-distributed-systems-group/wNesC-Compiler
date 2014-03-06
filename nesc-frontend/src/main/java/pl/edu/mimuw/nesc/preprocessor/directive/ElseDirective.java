package pl.edu.mimuw.nesc.preprocessor.directive;

/**
 * <p>Else directive.</p>
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
public class ElseDirective extends ConditionalDirective {

    public static final String NAME = "else";
    private static final int NAME_LENGTH = NAME.length();

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates preprocessor directive from builder parameters.
     *
     * @param builder builder.
     */
    protected ElseDirective(Builder builder) {
        super(builder);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * If builder.
     */
    public static class Builder extends ConditionalDirective.Builder<ElseDirective> {

        @Override
        protected void verify() {
            super.verify();
        }

        @Override
        protected ElseDirective getInstance() {
            return new ElseDirective(this);
        }
    }
}
