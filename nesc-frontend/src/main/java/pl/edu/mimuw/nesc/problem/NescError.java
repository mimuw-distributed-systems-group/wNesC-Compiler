package pl.edu.mimuw.nesc.problem;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class NescError extends NescIssue {

    public NescError(Optional<Location> startLocation, Optional<Location> endLocation, String message) {
        super(startLocation, endLocation, message);
    }

    public NescError(Location startLocation, Optional<Location> endLocation, String message) {
        super(Optional.of(startLocation), endLocation, message);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
