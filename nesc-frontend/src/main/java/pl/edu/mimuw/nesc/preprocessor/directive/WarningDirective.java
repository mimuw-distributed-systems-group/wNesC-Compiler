package pl.edu.mimuw.nesc.preprocessor.directive;

import static com.google.common.base.Preconditions.checkState;

/**
 * Warning preprocessor directive.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class WarningDirective extends PreprocessorDirective {

    public static Builder builder() {
        return new Builder();
    }

    protected final String message;

    /**
     * Creates preprocessor directive from builder parameters.
     *
     * @param builder builder.
     */
    protected WarningDirective(Builder builder) {
        super(builder);
        this.message = builder.message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * Warning builder.
     */
    public static class Builder extends PreprocessorDirective.Builder<WarningDirective> {

        protected String message;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            checkState(message != null, "message must be set");
        }

        @Override
        protected WarningDirective getInstance() {
            return new WarningDirective(this);
        }
    }
}
