package pl.edu.mimuw.nesc.preprocessor.directive;

import static com.google.common.base.Preconditions.checkState;

/**
 * Include preprocessor directive.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class IncludeDirective extends PreprocessorDirective {

    protected final String fileName;
    protected final boolean isSystem;
    protected final String filePath;
    protected final TokenLocation argumentLocation;

    /**
     * Creates preprocessor directive from builder parameters.
     *
     * @param builder builder.
     */
    protected IncludeDirective(Builder builder) {
        super(builder);
        this.fileName = builder.fileName;
        this.isSystem = builder.isSystem;
        this.filePath = builder.filePath;
        this.argumentLocation = builder.argumentLocation;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public String getFilePath() {
        return filePath;
    }

    public TokenLocation getArgumentLocation() {
        return argumentLocation;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * Include builder.
     */
    public static class Builder extends PreprocessorDirective.Builder<IncludeDirective> {

        protected String fileName;
        protected boolean isSystem;
        protected String filePath;
        protected TokenLocation argumentLocation;

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder isSystem(boolean isSystem) {
            this.isSystem = isSystem;
            return this;
        }

        public Builder argumentLocation(int line, int column, int length) {
            this.argumentLocation = new TokenLocation(line, column, length);
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            checkState(fileName != null, "filePath must be set");
            // checkState(filePath != null, "filePath must be set"); // FIXME
            checkState(argumentLocation != null, "argument location must be set");
        }

        @Override
        protected IncludeDirective getInstance() {
            return new IncludeDirective(this);
        }
    }
}
