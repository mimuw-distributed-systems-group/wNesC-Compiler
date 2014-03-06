package pl.edu.mimuw.nesc.preprocessor.directive;

import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Ifndef directive.</p>
 * <p/>
 * <p>
 * <pre>
 *     #ifndef MACRO
 *     controlled text
 *     #endif
 * </pre>
 * </p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class IfndefDirective extends ConditionalMacroDirective {

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates preprocessor directive from builder parameters.
     *
     * @param builder builder.
     */
    protected IfndefDirective(Builder builder) {
        super(builder);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * If builder.
     */
    public static class Builder extends ConditionalMacroDirective.Builder<IfndefDirective> {

        @Override
        protected void verify() {
            super.verify();
        }

        @Override
        protected IfndefDirective getInstance() {
            return new IfndefDirective(this);
        }
    }
}
