package pl.edu.mimuw.nesc.declaration.tag;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.declaration.Declaration;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class TagDeclaration extends Declaration {

    /**
     * Name is absent for anonymous tags.
     */
    private final Optional<String> name;

    public TagDeclaration(Optional<String> name, Location location) {
        super(location);
        this.name = name;
    }

    public Optional<String> getName() {
        return name;
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
