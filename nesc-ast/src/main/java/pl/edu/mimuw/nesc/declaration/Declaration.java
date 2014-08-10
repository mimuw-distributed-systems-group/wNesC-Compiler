package pl.edu.mimuw.nesc.declaration;

import com.google.common.base.Objects;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.environment.Environment;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class Declaration {

    protected Location location;
    protected Environment environment;

    protected Declaration(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(location);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Declaration other = (Declaration) obj;
        return Objects.equal(this.location, other.location);
    }
}
