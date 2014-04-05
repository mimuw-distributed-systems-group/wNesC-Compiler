package pl.edu.mimuw.nesc.declaration.label;

import com.google.common.base.Objects;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.declaration.Declaration;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class LabelDeclaration extends Declaration {

    private final String name;

    public LabelDeclaration(String name, Location location) {
        super(location);
        this.name = name;
    }

    public String getName() {
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
        final LabelDeclaration other = (LabelDeclaration) obj;
        return Objects.equal(this.name, other.name);
    }
}
