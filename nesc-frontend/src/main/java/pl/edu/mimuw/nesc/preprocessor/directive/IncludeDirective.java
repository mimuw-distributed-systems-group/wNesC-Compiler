package pl.edu.mimuw.nesc.preprocessor.directive;

import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkState;

/**
 * Include preprocessor directive.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class IncludeDirective extends PreprocessorDirective {

    public static Builder builder() {
        return new Builder();
    }

    protected final String fileName;
    protected final boolean isSystem;
    protected final Optional<String> filePath;
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

    public String getFileName() {
        return fileName;
    }

    public Optional<String> getFilePath() {
        return filePath;
    }

    public boolean isSystem() {
        return isSystem;
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
        protected Optional<String> filePath;
        protected TokenLocation argumentLocation;

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = Optional.fromNullable(filePath);
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
            checkState(fileName != null, "file name must be set");
            checkState(filePath != null, "file path must be set");
            checkState(argumentLocation != null, "argument location must be set");
        }

        @Override
        protected IncludeDirective getInstance() {
            return new IncludeDirective(this);
        }
    }
}
