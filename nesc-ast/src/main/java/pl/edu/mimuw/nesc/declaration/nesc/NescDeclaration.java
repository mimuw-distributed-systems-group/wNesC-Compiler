package pl.edu.mimuw.nesc.declaration.nesc;

import com.google.common.base.Objects;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.declaration.Declaration;
import pl.edu.mimuw.nesc.environment.Environment;

/**
 * Base class for nesc entity declarations.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class NescDeclaration extends Declaration {

    private final String name;

    protected NescDeclaration(String name, Location location) {
        super(location);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns the parameter environment of the NesC entity.
     *
     * @return parameter environment
     */
    public abstract Environment getParameterEnvironment();

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
        final NescDeclaration other = (NescDeclaration) obj;
        return Objects.equal(this.name, other.name);
    }

    public abstract <R, A> R accept(Visitor<R, A> visitor, A arg);

    public interface Visitor<R, A> {
        R visit(ConfigurationDeclaration configuration, A arg);

        R visit(InterfaceDeclaration iface, A arg);

        R visit(ModuleDeclaration module, A arg);
    }
}
