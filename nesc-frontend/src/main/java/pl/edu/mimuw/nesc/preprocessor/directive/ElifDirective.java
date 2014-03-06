package pl.edu.mimuw.nesc.preprocessor.directive;

/**
 * <p>Elif directive.</p>
 * <p/>
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
public class ElifDirective extends ConditionalExpDirective {

    public static final String NAME = "elif";
    public static final int NAME_LENGTH = NAME.length();

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates preprocessor directive from builder parameters.
     *
     * @param builder builder.
     */
    protected ElifDirective(Builder builder) {
        super(builder);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * If builder.
     */
    public static class Builder extends ConditionalExpDirective.Builder<ElifDirective> {

        @Override
        protected void verify() {
            super.verify();
        }

        @Override
        protected ElifDirective getInstance() {
            return new ElifDirective(this);
        }
    }
}
