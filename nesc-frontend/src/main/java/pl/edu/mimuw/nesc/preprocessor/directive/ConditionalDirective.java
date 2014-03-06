package pl.edu.mimuw.nesc.preprocessor.directive;

/**
 * Base class for conditional preprocessor directives.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class ConditionalDirective extends PreprocessorDirective {
    /**
     * Creates preprocessor directive from builder parameters.
     *
     * @param builder builder.
     */
    protected ConditionalDirective(Builder builder) {
        super(builder);
    }

    public static abstract class Builder<T extends ConditionalDirective> extends PreprocessorDirective.Builder<T> {

    }
}
