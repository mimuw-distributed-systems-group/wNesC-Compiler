package pl.edu.mimuw.nesc.preprocessor.directive;

import static com.google.common.base.Preconditions.checkState;

/**
 * Base class for preprocessor directives that to evaluate condition value
 * must check if specified macro is defined or not.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class ConditionalMacroDirective extends ConditionalDirective {

    protected final String macro;
    protected final TokenLocation macroLocation;

    /**
     * Creates preprocessor directive from builder parameters.
     *
     * @param builder builder.
     */
    protected ConditionalMacroDirective(Builder builder) {
        super(builder);
        this.macro = builder.macro;
        this.macroLocation = builder.macroLocation;
    }

    public String getMacro() {
        return macro;
    }

    public TokenLocation getMacroLocation() {
        return macroLocation;
    }

    /**
     * Builder.
     */
    public static abstract class Builder<T extends ConditionalMacroDirective> extends ConditionalDirective.Builder<T> {

        protected String macro;
        protected TokenLocation macroLocation;

        public Builder macro(String macro) {
            this.macro = macro;
            return this;
        }

        public Builder macroLocation(int line, int column, int length) {
            this.macroLocation = new TokenLocation(line, column, length);
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            checkState(macro != null, "macro must be set");
            checkState(macroLocation != null, "macroLocation must be set");
        }
    }
}
