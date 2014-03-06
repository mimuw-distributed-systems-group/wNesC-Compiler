package pl.edu.mimuw.nesc.preprocessor.directive;

/**
 * Pragma directive.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class PragmaDirective extends PreprocessorDirective {

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates preprocessor directive from builder parameters.
     *
     * @param builder builder.
     */
    protected PragmaDirective(Builder builder) {
        super(builder);
    }

    // TODO: parse body?

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * If builder.
     */
    public static class Builder extends PreprocessorDirective.Builder<PragmaDirective> {

        @Override
        protected void verify() {
            super.verify();
        }

        @Override
        protected PragmaDirective getInstance() {
            return new PragmaDirective(this);
        }
    }
}
