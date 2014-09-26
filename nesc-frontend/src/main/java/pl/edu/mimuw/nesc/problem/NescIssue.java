package pl.edu.mimuw.nesc.problem;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.problem.issue.Issue;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class for issues reported by compiler such as errors or warnings.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @see pl.edu.mimuw.nesc.problem.issue.Issue Issue
 */
public abstract class NescIssue {

    protected final Optional<Location> startLocation;
    protected final Optional<Location> endLocation;
    protected final Optional<Issue.Code> code;
    protected final String message;

    public NescIssue(Optional<Location> startLocation, Optional<Location> endLocation,
                     Optional<Issue.Code> code, String message) {
        checkNotNull(startLocation, "start location cannot be null");
        checkNotNull(endLocation, "end location cannot be null");
        checkNotNull(code, "code cannot be null");
        checkNotNull(message, "message cannot be null");
        this.startLocation = startLocation;
        this.endLocation = endLocation;
        this.code = code;
        this.message = message;
    }

    public Optional<Location> getStartLocation() {
        return startLocation;
    }

    public Optional<Location> getEndLocation() {
        return endLocation;
    }

    public Optional<Issue.Code> getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("startLocation", startLocation)
                .add("endLocation", endLocation)
                .add("code", code)
                .add("message", message)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(startLocation, endLocation, code, message);
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
                && Objects.equal(this.code, other.code)
                && Objects.equal(this.message, other.message);
    }

    public abstract <R, A> R accept(Visitor<R, A> visitor, A arg);

    public interface Visitor<R, A> {

        R visit(NescError error, A arg);

        R visit(NescWarning warning, A arg);

    }
}
