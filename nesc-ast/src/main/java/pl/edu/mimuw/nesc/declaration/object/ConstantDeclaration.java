package pl.edu.mimuw.nesc.declaration.object;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.type.IntType;
import pl.edu.mimuw.nesc.ast.type.Type;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Enumeration constant declaration.</p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ConstantDeclaration extends ObjectDeclaration {
    /**
     * The name of the constant that is globally unique.
     */
    private final String uniqueName;

    public static Builder builder() {
        return new Builder();
    }

    protected ConstantDeclaration(Builder builder) {
        super(builder);
        this.uniqueName = builder.uniqueName;
    }

    /**
     * Get the globally unique name of this constant declaration. It is the
     * mangled name.
     *
     * @return The globally unique name.
     */
    public String getUniqueName() {
        return uniqueName;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * Builder for the constant declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static class Builder extends ObjectDeclaration.Builder<ConstantDeclaration> {
        /**
         * Data needed to build a constant declaration.
         */
        private String uniqueName;

        /**
         * Set the globally unique name of the constant declaration.
         *
         * @param name Name to set.
         * @return <code>this</code>
         */
        public Builder uniqueName(String name) {
            this.uniqueName = name;
            return this;
        }

        @Override
        protected void beforeBuild() {
            super.beforeBuild();

            setType(Optional.<Type>of(new IntType()));
            setLinkage(Optional.of(Linkage.NONE));
            setKind(ObjectKind.CONSTANT);
        }

        @Override
        protected void validate() {
            super.validate();

            checkNotNull(uniqueName, "unique name cannot be null");
            checkState(!uniqueName.isEmpty(), "the unique name cannot be an empty string");
        }

        @Override
        protected ConstantDeclaration create() {
            return new ConstantDeclaration(this);
        }
    }
}
