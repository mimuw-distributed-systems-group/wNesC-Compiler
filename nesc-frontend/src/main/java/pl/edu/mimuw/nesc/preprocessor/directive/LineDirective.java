package pl.edu.mimuw.nesc.preprocessor.directive;

/**
 * Line directive.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class LineDirective extends PreprocessorDirective {

    public static Builder builder() {
        return new Builder();
    }

    // TODO: linenum filename?

    /**
     * Creates preprocessor directive from builder parameters.
     *
     * @param builder builder.
     */
    protected LineDirective(Builder builder) {
        super(builder);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * If builder.
     */
    public static class Builder extends PreprocessorDirective.Builder<LineDirective> {

        @Override
        protected void verify() {
            super.verify();
        }

        @Override
        protected LineDirective getInstance() {
            return new LineDirective(this);
        }
    }
}
