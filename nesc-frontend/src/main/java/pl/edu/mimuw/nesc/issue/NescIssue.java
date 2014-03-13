package pl.edu.mimuw.nesc.issue;

import com.google.common.base.Objects;
import pl.edu.mimuw.nesc.ast.Location;

/**
 * Base class for issues reported by compiler such as errors or warnings.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class NescIssue {

    protected final Location startLocation;
    protected final Location endLocation;
    protected final String message;

    public NescIssue(Location startLocation, Location endLocation, String message) {
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.message = message;
    }

    public Location getStartLocation() {
        return startLocation;
    }

    public Location getEndLocation() {
        return endLocation;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("startLocation", startLocation)
                .add("endLocation", endLocation)
                .add("message", message)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(startLocation, endLocation, message);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final NescIssue other = (NescIssue) obj;
        return Objects.equal(this.startLocation, other.startLocation)
                && Objects.equal(this.endLocation, other.endLocation)
                && Objects.equal(this.message, other.message);
    }

    public abstract <R, A> R accept(Visitor<R, A> visitor, A arg);

    public interface Visitor<R, A> {

        R visit(NescError error, A arg);

        R visit(NescWarning warning, A arg);

    }
}
