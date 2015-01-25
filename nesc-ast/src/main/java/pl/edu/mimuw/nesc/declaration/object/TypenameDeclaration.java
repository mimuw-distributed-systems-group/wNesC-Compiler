package pl.edu.mimuw.nesc.declaration.object;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.declaration.CopyController;
import pl.edu.mimuw.nesc.external.ExternalScheme;
import pl.edu.mimuw.nesc.type.IntegerType;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.type.TypeDefinitionType;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class TypenameDeclaration extends ObjectDeclaration {

    /**
     * Type that is defined by the type definition this object represents. It
     * shall be absent if the type is unknown or erroneous.
     */
    private Optional<Type> denotedType;

    /**
     * Value indicating if this type definition represents a generic, type
     * parameter of a generic component or a generic interface.
     */
    private final boolean isGenericParameter;

    /**
     * The globally unique name of this type definition.
     */
    private final String uniqueName;

    public static Builder builder() {
        return new Builder();
    }

    protected TypenameDeclaration(Builder builder) {
        super(builder);

        this.denotedType = builder.denotedType;
        this.isGenericParameter = builder.isGenericParameter;
        this.uniqueName = builder.uniqueName;
    }

    public Optional<Type> getDenotedType() {
        return denotedType;
    }

    /**
     * <p>Check if this declaration represents a generic type parameter of
     * a generic component or a generic interface.</p>
     *
     * @return Value indicating if this declaration represents a generic type
     *         parameter of a generic component or a generic interface.
     */
    public boolean isGenericParameter() {
        return isGenericParameter;
    }

    /**
     * <p>Get the globally unique name of this type definition.</p>
     *
     * @return The globally unique name of this type definition.
     */
    public String getUniqueName() {
        return uniqueName;
    }

    /**
     * <p>Make the denoted type an external base type by adding to it the given
     * external scheme. It the denoted type is absent, this call has no
     * effect. If the denoted type is not an integer type, an cast exception
     * will be thrown. If the denoted type is an enumerated type, an
     * unsupported operation exception will be thrown.</p>
     *
     * @param externalScheme External scheme to associate with the denoted type.
     */
    public void addExternalScheme(ExternalScheme externalScheme) {
        checkNotNull(externalScheme, "external scheme cannot be null");
        this.denotedType.transform(new ExternalSchemeAddition(externalScheme));
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    @Override
    public TypenameDeclaration deepCopy(CopyController controller) {
        return TypenameDeclaration.builder()
                .denotedType(controller.mapType(this.denotedType).orNull())
                .isGenericParameter(this.isGenericParameter)
                .uniqueName(controller.mapUniqueName(this.uniqueName))
                .name(this.name)
                .startLocation(this.location)
                .build();
    }

    /**
     * Builder for the typename declarations.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static class Builder extends ObjectDeclaration.Builder<TypenameDeclaration> {
        /**
         * Data needed to build a typename declaration.
         */
        private Optional<Type> denotedType = Optional.absent();
        private boolean isGenericParameter = false;
        private String uniqueName;

        protected Builder() {
        }

        /**
         * Set the type that is denoted by the typename declaration by
         * a nullable reference.
         *
         * @param denotedType The denoted type to set.
         * @return <code>this</code>
         */
        public Builder denotedType(Type denotedType) {
            this.denotedType = Optional.fromNullable(denotedType);
            return this;
        }

        /**
         * <p>Set the value indicating if this typename declaration represents
         * a generic type parameter of a generic component.</p>
         *
         * @param isGenericParameter Value to set.
         * @return <code>this</code>
         */
        public Builder isGenericParameter(boolean isGenericParameter) {
            this.isGenericParameter = isGenericParameter;
            return this;
        }

        /**
         * <p>Set the globally unique name of the type definition.</p>
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

            setType(Optional.<Type>of(TypeDefinitionType.getInstance()));
            setLinkage(Optional.of(Linkage.NONE));
            setKind(ObjectKind.TYPENAME);
        }

        @Override
        protected void validate() {
            super.validate();

            checkNotNull(denotedType, "the denoted type cannot be null");
            checkNotNull(uniqueName, "unique name cannot be null");
            checkState(!uniqueName.isEmpty(), "unique name cannot be an empty string");
        }

        @Override
        protected TypenameDeclaration create() {
            return new TypenameDeclaration(this);
        }
    }

    /**
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class ExternalSchemeAddition implements Function<Type, Type> {
        private final ExternalScheme scheme;

        private ExternalSchemeAddition(ExternalScheme toAdd) {
            this.scheme = toAdd;
        }

        @Override
        public Type apply(Type type) {
            checkNotNull(type, "type cannot be null");
            final IntegerType integerType = (IntegerType) type;
            return integerType.addExternalScheme(scheme);
        }
    }
}
