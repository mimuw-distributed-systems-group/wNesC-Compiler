package pl.edu.mimuw.nesc.declaration.object;

import com.google.common.base.Optional;
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
     * The object is present if and only if this variable is external. It
     * specifies the name in the external variables compiler option that
     * corresponds to this variable.
     */
    private final Optional<String> originalExternalName;

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
        this.originalExternalName = builder.originalExternalName;
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

    /**
     * <p>Check if this variable is an external variable. An external variable
     * is a variable whose linkage is guaranteed not to change from external to
     * internal in the output C file.</p>
     *
     * @return <code>true</code> if and only if this variable is external.
     */
    public boolean isExternalVariable() {
        return this.originalExternalName.isPresent();
    }

    /**
     * <p>Get the original external name of this variable if it is an external
     * variable. It it the name given in the external variables compiler option
     * that corresponds to this variable.</p>
     *
     * @return The original external name of this variable if it is such.
     * @throws IllegalStateException This variable is not an external variable.
     */
    public String getOriginalExternalName() {
        checkState(this.originalExternalName.isPresent(), "this variable is not an external variable");
        return this.originalExternalName.get();
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    @Override
    public VariableDeclaration deepCopy(CopyController controller) {
        final VariableDeclaration.Builder builder = VariableDeclaration.builder();

        builder.uniqueName(controller.mapUniqueName(this.uniqueName))
                .isGenericParameter(this.isGenericParameter)
                .linkage(this.linkage.orNull())
                .type(controller.mapType(this.type).orNull())
                .name(this.name)
                .startLocation(this.location);

        if (isExternalVariable()) {
            builder.external(getOriginalExternalName());
        }

        return builder.build();

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
        private Optional<String> originalExternalName = Optional.absent();
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

        /**
         * <p>Marks the variable as external and associates with it the given
         * original external name. It shall be the name from the external
         * variables compiler option that corresponds to this variable.</p>
         *
         * @param originalExternalName Name from the external variables compiler
         *                             option that corresponds to this variable.
         * @return <code>this</code>
         */
        public Builder external(String originalExternalName) {
            this.originalExternalName = Optional.of(originalExternalName);
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
            checkNotNull(originalExternalName, "original external name cannot be null");
            checkState(!uniqueName.isEmpty(), "the unique name cannot be an empty string");
            checkState(!originalExternalName.isPresent() || !originalExternalName.get().isEmpty(),
                    "the original external name cannot be empty");
        }

        @Override
        protected VariableDeclaration create() {
            return new VariableDeclaration(this);
        }
    }
}
