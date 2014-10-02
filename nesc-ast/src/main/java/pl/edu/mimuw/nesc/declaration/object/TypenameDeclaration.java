package pl.edu.mimuw.nesc.declaration.object;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.type.TypeDefinitionType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class TypenameDeclaration extends ObjectDeclaration {

    /**
     * Type that is defined by the type definition this object represents. It
     * shall be absent if the type is unknown.
     */
    private final Optional<Type> denotedType;

    public static Builder builder() {
        return new Builder();
    }

    protected TypenameDeclaration(Builder builder) {
        super(builder);
        this.denotedType = builder.denotedType;
    }

    public Optional<Type> getDenotedType() {
        return denotedType;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
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
        }

        @Override
        protected TypenameDeclaration create() {
            return new TypenameDeclaration(this);
        }
    }
}
