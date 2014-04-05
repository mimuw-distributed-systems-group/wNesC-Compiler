package pl.edu.mimuw.nesc.declaration.tag;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.declaration.Declaration;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class FieldDeclaration extends Declaration {

    /**
     * May be absent for bitfields.
     */
    private final Optional<String> name;

    public FieldDeclaration(Optional<String> name, Location location) {
        super(location);
        this.name = name;
    }

    public Optional<String> getName() {
        return name;
    }

    public Location getLocation() {
        return location;
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
        final FieldDeclaration other = (FieldDeclaration) obj;
        return Objects.equal(this.name, other.name);
    }
}
