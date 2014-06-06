package pl.edu.mimuw.nesc.preprocessor.directive;

/**
 * Base class for preprocessor directives that to evaluate condition value
 * must evaluate some preprocessor expression.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class ConditionalExpDirective extends ConditionalDirective {

    //TODO: expression - it may be complicated, do we need to parse it somehow?

    /**
     * Creates preprocessor directive from builder parameters.
     *
     * @param builder builder.
     */
    protected ConditionalExpDirective(Builder<? extends ConditionalExpDirective> builder) {
        super(builder);
    }

    /**
     * Builder.
     */
    public static abstract class Builder<T extends ConditionalExpDirective> extends ConditionalDirective.Builder<T> {

    }
}
