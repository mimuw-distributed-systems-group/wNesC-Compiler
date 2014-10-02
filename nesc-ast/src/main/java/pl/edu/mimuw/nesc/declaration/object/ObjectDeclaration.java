package pl.edu.mimuw.nesc.declaration.object;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.declaration.Declaration;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * <p>Object namespace is defined both in C and nesc standard.
 * Object namespace contains:
 * <ul>
 * <li>variables,</li>
 * <li>typedefs,</li>
 * <li>function names,</li>
 * <li>enumeration constants,</li>
 * <li>interface reference,</li>
 * <li>component reference,</li>
 * // TODO magic_string, magic_function
 * </ul>
 * etc.
 * </p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class ObjectDeclaration extends Declaration {

    protected final String name;

    /**
     * Type of the object that this declaration represents. It may not be
     * present, e.g. when the type given in the corresponding declaration has
     * been invalid. Artificial types are created for objects other than
     * variables and functions.
     */
    protected final Optional<Type> type;

    /**
     * Linkage of this object. May be absent if it couldn't have been
     * determined.
     *
     * @see Linkage
     */
    protected final Optional<Linkage> linkage;

    /**
     * Kind of this object declaration.
     */
    protected final ObjectKind kind;

    protected ObjectDeclaration(Builder<? extends ObjectDeclaration> builder) {
        super(builder);
        this.name = builder.name;
        this.kind = builder.kind;
        this.type = builder.type;
        this.linkage = builder.linkage;
    }

    public String getName() {
        return name;
    }

    /**
     * Get the kind of this object declaration.
     *
     * @return Kind of this object declaration. Never null.
     */
    public ObjectKind getKind() {
        return kind;
    }

    public Optional<Type> getType() {
        return type;
    }

    public Optional<Linkage> getLinkage() {
        return linkage;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ObjectDeclaration other = (ObjectDeclaration) obj;
        return Objects.equal(this.name, other.name);
    }

    public abstract <R, A> R accept(Visitor<R, A> visitor, A arg);

    public interface Visitor<R, A> {
        R visit(ComponentRefDeclaration componentRef, A arg);

        R visit(ConstantDeclaration constant, A arg);

        R visit(FunctionDeclaration function, A arg);

        R visit(InterfaceRefDeclaration interfaceRef, A arg);

        R visit(TypenameDeclaration typename, A arg);

        R visit(VariableDeclaration variable, A arg);
    }

    /**
     * Builder that does not allow to set the type and linkage with public
     * methods.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     * @see ExtendedBuilder ExtendedBuilder
     */
    public static abstract class Builder<T extends ObjectDeclaration> extends Declaration.Builder<T> {
        /**
         * Data needed for building the declaration object.
         */
        private String name;
        private ObjectKind kind;
        private Optional<Type> type = Optional.absent();
        private Optional<Linkage> linkage = Optional.absent();

        /**
         * Remember if some fields have been set.
         */
        private boolean kindAssigned = false;
        private boolean typeAssigned = false;
        private boolean linkageAssigned = false;

        protected Builder() {
        }

        /**
         * Set the name of the object that will be represented by a declaration
         * class instance.
         *
         * @param name Name of the object to set.
         * @return <code>this</code>
         */
        public Builder<T> name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set the type of the object represented by the declaration. This
         * method should be called in a <code>beforeBuild</code> override.
         *
         * @param type Type to set.
         * @throws IllegalStateException The type has been already set.
         */
        protected void setType(Optional<Type> type) {
            checkState(!typeAssigned, "the type can be set exactly once");
            this.type = type;
            this.typeAssigned = true;
        }

        /**
         * Set the linkage of the object represented by the created declaration.
         * This method should be called in a <code>beforeBuild</code> override.
         *
         * @param linkage Linkage to set.
         * @throws IllegalStateException The linkage has been already set.
         */
        protected void setLinkage(Optional<Linkage> linkage) {
            checkState(!linkageAssigned, "the linkage can be set exactly once");
            this.linkage = linkage;
            this.linkageAssigned = true;
        }

        /**
         * Set the kind of the object declaration instance. This method should
         * be called in a <code>beforeBuild</code> override.
         *
         * @param kind Kind to set.
         * @throws IllegalStateException The kind has been already set.
         */
        protected void setKind(ObjectKind kind) {
            checkState(!kindAssigned, "the kind can be set exactly once");
            this.kind = kind;
            this.kindAssigned = true;
        }

        @Override
        protected void validate() {
            super.validate();
            checkNotNull(name, "the name cannot be null");
            checkNotNull(kind, "the kind cannot be null");
            checkNotNull(type, "the type cannot be null");
            checkNotNull(linkage, "the linkage cannot be null");
        }
    }

    /**
     * Builder that allows setting the type and linkage with public methods.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     * @see Builder Builder
     */
    public static abstract class ExtendedBuilder<T extends ObjectDeclaration> extends Builder<T> {
        /**
         * Type and linkage that must be saved before passing them to the
         * superclass.
         */
        private Optional<Type> type = Optional.absent();
        private Optional<Linkage> linkage = Optional.absent();

        protected ExtendedBuilder() {
        }

        /**
         * Set the type with a nullable reference.
         *
         * @param type The type to set. It can be null if no type is to be set.
         * @return <code>this</code>
         */
        public ExtendedBuilder<T> type(Type type) {
            this.type = Optional.fromNullable(type);
            return this;
        }

        /**
         * Set the linkage using a nullable reference.
         *
         * @param linkage Linkage to set. If it is null, then no linkage will be
         *                set.
         * @return <code>this</code>
         */
        public ExtendedBuilder<T> linkage(Linkage linkage) {
            this.linkage = Optional.fromNullable(linkage);
            return this;
        }

        @Override
        protected void beforeBuild() {
            super.beforeBuild();
            setType(type);
            setLinkage(linkage);
        }
    }
}
