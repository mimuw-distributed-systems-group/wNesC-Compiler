package pl.edu.mimuw.nesc.preprocessor.directive;

import static com.google.common.base.Preconditions.checkState;

/**
 * Define preprocessor directive.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class DefineDirective extends PreprocessorDirective {

    public static final String NAME = "define";
    private static final int NAME_LENGTH = NAME.length();
    protected final String name;
    protected final TokenLocation nameLocation;

    /**
     * Creates preprocessor directive from builder parameters.
     *
     * @param builder builder.
     */
    protected DefineDirective(Builder builder) {
        super(builder);
        this.name = builder.name;
        this.nameLocation = builder.nameLocation;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public TokenLocation getNameLocation() {
        return nameLocation;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * Define builder.
     */
    public static class Builder extends PreprocessorDirective.Builder<DefineDirective> {

        protected String name;
        protected TokenLocation nameLocation;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder nameLocation(int line, int column, int length) {
            this.nameLocation = new TokenLocation(line, column, length);
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            checkState(name != null, "name must be set");
            checkState(nameLocation != null, "nameLocation must be set");
        }

        @Override
        protected DefineDirective getInstance() {
            return new DefineDirective(this);
        }
    }
}
