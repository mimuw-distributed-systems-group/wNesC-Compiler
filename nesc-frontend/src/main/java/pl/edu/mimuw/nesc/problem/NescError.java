package pl.edu.mimuw.nesc.problem;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.problem.issue.Issue;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class NescError extends NescIssue {

    public NescError(Optional<Location> startLocation, Optional<Location> endLocation, String message) {
        this(startLocation, endLocation, Optional.<Issue.Code>absent(), message);
    }

    public NescError(Location startLocation, Optional<Location> endLocation, String message) {
        this(Optional.of(startLocation), endLocation, Optional.<Issue.Code>absent(), message);
    }

    public NescError(Optional<Location> startLocation, Optional<Location> endLocation,
                     Optional<Issue.Code> code, String message) {
        super(startLocation, endLocation, code, message);
    }

    public NescError(Location startLocation, Optional<Location> endLocation,
                     Optional<Issue.Code> code, String message) {
        this(Optional.of(startLocation), endLocation, code, message);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
