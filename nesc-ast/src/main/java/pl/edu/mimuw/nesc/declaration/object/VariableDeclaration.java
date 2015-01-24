package pl.edu.mimuw.nesc.declaration.object;

import pl.edu.mimuw.nesc.declaration.CopyController;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class VariableDeclaration extends ObjectDeclaration {
    /**
     * Value indicating if this object represents a generic parameter.
     */
    private final boolean isGenericParameter;

    /**
     * Globally unique name of the variable that this declaration object
     * represents.
     */
    private final String uniqueName;

    public static Builder builder() {
        return new Builder();
    }

    protected VariableDeclaration(Builder builder) {
        super(builder);

        this.isGenericParameter = builder.isGenericParameter;
        this.uniqueName = builder.uniqueName;
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

    /**
     * <p>Get the globally unique name of the variable that this declaration
     * object represents.</p>
     *
     * @return Unique name of the variable this declaration object represents.
     */
    public String getUniqueName() {
        return uniqueName;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    @Override
    public VariableDeclaration deepCopy(CopyController controller) {
        return VariableDeclaration.builder()
                .uniqueName(controller.mapUniqueName(this.uniqueName))
                .isGenericParameter(this.isGenericParameter)
                .linkage(this.linkage.orNull())
                .type(controller.mapType(this.type).orNull())
                .name(this.name)
                .startLocation(this.location)
                .build();

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
        private String uniqueName;

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

        /**
         * <p>Set the unique name of the variable.</p>
         *
         * @param name Unique name to set.
         * @return <code>this</code>
         */
        public Builder uniqueName(String name) {
            this.uniqueName = name;
            return this;
        }

        @Override
        protected void beforeBuild() {
            super.beforeBuild();
            setKind(ObjectKind.VARIABLE);
        }

        @Override
        protected void validate() {
            super.validate();

            checkNotNull(uniqueName, "the unique name cannot be null");
            checkState(!uniqueName.isEmpty(), "the unique name cannot be an empty string");
        }

        @Override
        protected VariableDeclaration create() {
            return new VariableDeclaration(this);
        }
    }
}
