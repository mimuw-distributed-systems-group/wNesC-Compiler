package pl.edu.mimuw.nesc.declaration.object;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class VariableDeclaration extends ObjectDeclaration {

    private final boolean isGenericParameter;

    public static Builder builder() {
        return new Builder();
    }

    protected VariableDeclaration(Builder builder) {
        super(builder);
        this.isGenericParameter = builder.isGenericParameter;
    }

    /**
     * <p>Check if this variable declaration object represents a generic
     * non-type parameter of a generic component.</p>
     *
     * @return Value indicating if this declaration represents a generic
     *         parameter non-type parameter of a component.
     */
    public boolean isGenericParameter() {
        return isGenericParameter;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * Builder for the variable declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static class Builder extends ExtendedBuilder<VariableDeclaration> {
        /**
         * Data needed to build a variable declaration.
         */
        private boolean isGenericParameter = false;

        /**
         * Private constructor to limit its accessibility.
         */
        protected Builder() {
        }

        /**
         * Set the value indicating if the variable declaration represents
         * a generic parameter.
         *
         * @param isGenericParameter Value to set.
         * @return <code>this</code>
         */
        public Builder isGenericParameter(boolean isGenericParameter) {
            this.isGenericParameter = isGenericParameter;
            return this;
        }

        @Override
        protected void beforeBuild() {
            super.beforeBuild();
            setKind(ObjectKind.VARIABLE);
        }

        @Override
        protected VariableDeclaration create() {
            return new VariableDeclaration(this);
        }
    }
}
