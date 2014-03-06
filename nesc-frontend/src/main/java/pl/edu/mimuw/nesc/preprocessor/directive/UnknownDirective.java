package pl.edu.mimuw.nesc.preprocessor.directive;


/**
 * Represents unknown preprocessor directive (possibly containing a typo in
 * directive name).
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class UnknownDirective extends PreprocessorDirective {

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates preprocessor directive from builder parameters.
     *
     * @param builder builder.
     */
    protected UnknownDirective(Builder builder) {
        super(builder);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * Unknown directive builder.
     */
    public static class Builder extends PreprocessorDirective.Builder<UnknownDirective> {

        public Builder() {
            super();
        }

        @Override
        protected void prepare() {
            super.prepare();
        }

        @Override
        protected void verify() {
            super.verify();
        }

        @Override
        protected UnknownDirective getInstance() {
            return new UnknownDirective(this);
        }
    }
}
