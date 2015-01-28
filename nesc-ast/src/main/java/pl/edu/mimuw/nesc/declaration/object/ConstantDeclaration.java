package pl.edu.mimuw.nesc.declaration.object;

import com.google.common.base.Optional;
import java.math.BigInteger;
import pl.edu.mimuw.nesc.ast.gen.Enumerator;
import pl.edu.mimuw.nesc.declaration.CopyController;
import pl.edu.mimuw.nesc.type.IntType;
import pl.edu.mimuw.nesc.type.Type;

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

    /**
     * Enumerator that declares this constant.
     */
    private final Enumerator enumerator;

    /**
     * Value of this enumeration constant. It is absent before it is computed.
     */
    private Optional<BigInteger> value;

    public static Builder builder() {
        return new Builder();
    }

    protected ConstantDeclaration(Builder builder) {
        super(builder);
        this.uniqueName = builder.uniqueName;
        this.enumerator = builder.enumerator;
        this.value = Optional.absent();
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

    /**
     * Get the enumerator that declares this constant.
     *
     * @return AST node of enumerator that declares this constant.
     */
    public Enumerator getEnumerator() {
        return enumerator;
    }

    /**
     * Get the value of this constant.
     *
     * @return Value of this constant. Never <code>null</code>. The object is
     *         absent if the value has not been set yet.
     */
    public Optional<BigInteger> getValue() {
        return value;
    }

    /**
     * Set the value of this enumeration constant.
     *
     * @param value Value to set.
     * @throws NullPointerException The given parameter is <code>null</code>.
     * @throws IllegalStateException The value has been already set.
     */
    public void setValue(BigInteger value) {
        checkNotNull(value, "value cannot be null");
        checkState(!this.value.isPresent(), "value has been already set");

        this.value = Optional.of(value);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    @Override
    public ConstantDeclaration deepCopy(CopyController controller) {
        final ConstantDeclaration result = ConstantDeclaration.builder()
                .enumerator(controller.mapNode(this.enumerator))
                .uniqueName(controller.mapUniqueName(this.uniqueName))
                .name(this.name)
                .startLocation(this.location)
                .build();
        result.value = this.value;

        return result;
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
        private Enumerator enumerator;

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

        public Builder enumerator(Enumerator enumerator) {
            this.enumerator = enumerator;
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
            checkNotNull(enumerator, "enumerator cannot be null");
            checkState(!uniqueName.isEmpty(), "the unique name cannot be an empty string");
        }

        @Override
        protected ConstantDeclaration create() {
            return new ConstantDeclaration(this);
        }
    }
}
