package pl.edu.mimuw.nesc.symboltable;

import com.google.common.base.Objects;
import pl.edu.mimuw.nesc.ast.Location;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class Partition {

    private final String name;
    private Location visibleFrom;

    public Partition(String name) {
        this(name, new Location("", 0, 0));
    }

    public Partition(String name, Location visibleFrom) {
        this.name = name;
        this.visibleFrom = visibleFrom;
    }

    public String getName() {
        return name;
    }

    public Location getVisibleFrom() {
        return visibleFrom;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, visibleFrom);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Partition other = (Partition) obj;
        return Objects.equal(this.name, other.name) && Objects.equal(this.visibleFrom, other.visibleFrom);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("name", name)
                .add("visibleFrom", visibleFrom)
                .toString();
    }
}
