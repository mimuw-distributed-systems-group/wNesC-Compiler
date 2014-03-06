package pl.edu.mimuw.nesc.preprocessor.directive;

import static com.google.common.base.Preconditions.checkState;

/**
 * Error preprocessor directive.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class ErrorDirective extends PreprocessorDirective {

    public static final String NAME = "error";
    public static final int NAME_LENGTH = NAME.length();

    public static Builder builder() {
        return new Builder();
    }

    protected final String message;

    /**
     * Creates preprocessor directive from builder parameters.
     *
     * @param builder builder.
     */
    protected ErrorDirective(Builder builder) {
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
     * Error builder.
     */
    public static class Builder extends PreprocessorDirective.Builder<ErrorDirective> {

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
        protected ErrorDirective getInstance() {
            return new ErrorDirective(this);
        }
    }
}
