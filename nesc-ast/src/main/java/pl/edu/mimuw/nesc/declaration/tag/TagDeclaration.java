package pl.edu.mimuw.nesc.declaration.tag;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.declaration.Declaration;
import pl.edu.mimuw.nesc.ast.type.Type;

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
     * <code>true</code> if and only if this object represents a tag that has
     * been already defined and contains information from its definition.
     */
    private final boolean isDefined;

    /**
     * If this object represents a declaration but not definition, then this
     * field can contain a reference to the object representing definition.
     * Otherwise, it shall be absent.
     */
    private Optional<TagDeclaration> definitionLink = Optional.absent();

    protected TagDeclaration(Optional<String> name, Location location, boolean isDefined) {
        super(location);
        this.name = name;
        this.isDefined = isDefined;
    }

    public Optional<String> getName() {
        return name;
    }

    public boolean isDefined() {
        return isDefined;
    }

    /**
     * @return Newly created object that represents the type that this tag
     *         corresponds to.
     */
    public abstract Type getType(boolean constQualified, boolean volatileQualified);

    /**
     * Sets the definition link to the given argument.
     *
     * @param tagDefinition Tag declaration to be set as the definition link.
     * @throws NullPointerException Given argument is null.
     * @throws IllegalStateException The definition link has been already set.
     *                               This object represents the definition.
     *                               The given object does not represents the
     *                               definition.
     */
    public final void setDefinitionLink(TagDeclaration tagDefinition) {
        checkNotNull(tagDefinition, "tag definition cannot be null");
        checkArgument(getClass().equals(tagDefinition.getClass()), "cannot set a tag declaration of a different class as the definition link");
        checkState(!isDefined(), "cannot check the definition link on an object that represents the definition");
        checkState(!getDefinitionLink().isPresent(), "the definition link can be set exactly once");
        checkState(tagDefinition.isDefined(), "cannot set a declaration but not definition as the definition link");

        definitionLink = Optional.of(tagDefinition);
    }

    /**
     * @return If this object represents a declaration but not definition,
     *         reference to the object that represents the definition if it has
     *         been already reached. Otherwise, the value is absent.
     */
    public final Optional<TagDeclaration> getDefinitionLink() {
        return definitionLink;
    }

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

    public abstract <R, A> R visit(Visitor<R, A> visitor, A arg);

    public interface Visitor<R, A> {
        R visit(AttributeDeclaration attribute, A arg);

        R visit(EnumDeclaration _enum, A arg);

        R visit(StructDeclaration struct, A arg);

        R visit(UnionDeclaration union, A arg);
    }
}
