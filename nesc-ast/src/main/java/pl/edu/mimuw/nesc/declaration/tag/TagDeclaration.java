package pl.edu.mimuw.nesc.declaration.tag;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.ast.gen.TagRef;
import pl.edu.mimuw.nesc.declaration.CopyController;
import pl.edu.mimuw.nesc.declaration.Declaration;
import pl.edu.mimuw.nesc.type.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class TagDeclaration extends Declaration {

    /**
     * Name is absent for anonymous tags.
     */
    private final Optional<String> name;

    /**
     * Kind of the tag this object reflects. Never null.
     */
    private final StructKind kind;

    /**
     * Mangled name of the tag that is globally unique.
     */
    private final Optional<String> uniqueName;

    protected TagDeclaration(Builder<? extends TagDeclaration> builder) {
        super(builder);
        this.name = builder.name;
        this.uniqueName = builder.uniqueName;
        this.kind = builder.kind;
    }

    /**
     * Get the name of the tag this object reflects.
     *
     * @return Name of the tag.
     */
    public Optional<String> getName() {
        return name;
    }

    /**
     * Get the globally unique name of the tag this object represents.
     *
     * @return The mangled, unique name of the tag. The object is present if and
     *         only if the normal name is present.
     */
    public Optional<String> getUniqueName() {
        return uniqueName;
    }

    /**
     * Check if this tag declaration corresponds to a defined tag.
     *
     * @return <code>true</code> if and only if this tag declaration corresponds
     *         to a defined tag and it has information from its definition.
     */
    public abstract boolean isDefined();

    /**
     * Get the kind of the tag this declaration corresponds to.
     *
     * @return Kind of the tag.
     */
    public StructKind getKind() {
        return kind;
    }

    /**
     * Check if this declaration corresponds to an external tag.
     *
     * @return <code>true</code> if and only if this tag corresponds to an
     *         external type.
     */
    public boolean isExternal() {
        return getKind().isExternal();
    }

    /**
     * @return Newly created object that represents the type that this tag
     *         corresponds to.
     */
    public abstract Type getType(boolean constQualified, boolean volatileQualified);

    /**
     * @return AST node that this object reflects.
     */
    public abstract TagRef getAstNode();

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final TagDeclaration other = (TagDeclaration) obj;
        return Objects.equal(this.name, other.name);
    }

    public abstract <R, A> R accept(Visitor<R, A> visitor, A arg);

    public interface Visitor<R, A> {
        R visit(AttributeDeclaration attribute, A arg);

        R visit(EnumDeclaration _enum, A arg);

        R visit(StructDeclaration struct, A arg);

        R visit(UnionDeclaration union, A arg);
    }

    @Override
    public abstract TagDeclaration deepCopy(CopyController controller);

    /**
     * Builder for a tag declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public abstract static class Builder<T extends TagDeclaration> extends Declaration.Builder<T> {
        /**
         * Variables to allow control of the building process.
         */
        private boolean kindSet;

        /**
         * Data needed to build a tag declaration.
         */
        private Optional<String> name = Optional.absent();
        private Optional<String> uniqueName = Optional.absent();
        private StructKind kind;

        protected Builder() {
        }

        /**
         * Set the name of a tag. It may be null and then no name will be used.
         *
         * @param name Name of a tag to set.
         * @return <code>this</code>
         */
        public Builder<T> name(String name, String uniqueName) {
            this.name = Optional.fromNullable(name);
            this.uniqueName = Optional.fromNullable(uniqueName);
            return this;
        }

        /**
         * Set the kind of the tag.
         *
         * @param kind Kind to set.
         * @throws IllegalStateException The tag has been already set.
         */
        protected void setKind(StructKind kind) {
            checkState(!kindSet, "the tag kind can be set exactly once");
            this.kind = kind;
            this.kindSet = true;
        }

        /**
         * Get the name of the tag that has been set (or not).
         *
         * @return Name of the tag that has been set.
         */
        protected Optional<String> getName() {
            return name;
        }

        @Override
        protected void validate() {
            super.validate();
            checkNotNull(name, "tag name cannot be null");
            checkNotNull(uniqueName, "unique name cannot be null");
            checkNotNull(kind, "tag kind must not be null");
            checkState(name.isPresent() == uniqueName.isPresent(),
                    "unique name must be set if and only if the name is set");
        }
    }
}
