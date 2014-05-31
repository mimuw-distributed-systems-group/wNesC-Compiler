package pl.edu.mimuw.nesc.problem;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class for issues reported by compiler such as errors or warnings.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class NescIssue {

    protected final Optional<Location> startLocation;
    protected final Optional<Location> endLocation;
    protected final String message;

    public NescIssue(Optional<Location> startLocation, Optional<Location> endLocation, String message) {
        checkNotNull(startLocation, "start location cannot be null");
        checkNotNull(endLocation, "end location cannot be null");
        checkNotNull(message, "message cannot be null");
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.message = message;
    }

    public Optional<Location> getStartLocation() {
        return startLocation;
    }

    public Optional<Location> getEndLocation() {
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
