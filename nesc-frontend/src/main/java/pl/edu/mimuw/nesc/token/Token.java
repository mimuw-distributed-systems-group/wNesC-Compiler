package pl.edu.mimuw.nesc.token;

import com.google.common.base.Objects;
import pl.edu.mimuw.nesc.ast.Location;

/**
 * <p>Base class for tokens from source files.</p>
 * <p>Instance of Token carries some semantic information. Tokens could be
 * divided in several semantic groups, such as:
 * </p>
 * <ul>
 * <li>macro usages,</li>
 * <li>identifiers (variables, type names, interface references etc.).</li>
 * </ul>
 * </p>
 * <p>Token may contain some references to semantic structures: AST nodes,
 * symbol table etc.</p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class Token {

    protected final Location startLocation;
    protected final Location endLocation;

    protected Token(Location startLocation, Location endLocation) {
        this.startLocation = startLocation;
        this.endLocation = endLocation;
    }

    public Location getStartLocation() {
        return startLocation;
    }

    public Location getEndLocation() {
        return endLocation;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("startLocation", startLocation)
                .add("endLocation", endLocation)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(startLocation, endLocation);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Token other = (Token) obj;
        return Objects.equal(this.startLocation, other.startLocation)
                && Objects.equal(this.endLocation, other.endLocation);
    }

    public abstract <R, A> R accept(Visitor<R, A> visitor, A arg);

    public static interface Visitor<R, A> {
        R visit(MacroToken macroToken, A arg);
    }
}
