package pl.edu.mimuw.nesc.token;

import com.google.common.base.Objects;
import pl.edu.mimuw.nesc.ast.Location;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class KeywordToken extends Token {

    protected final String text;

    public KeywordToken(Location startLocation, Location endLocation, String text) {
        super(startLocation, endLocation);
        this.text = text;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("startLocation", startLocation)
                .add("endLocation", endLocation)
                .add("text", text)
                .toString();
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(text);
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
        final KeywordToken other = (KeywordToken) obj;
        return Objects.equal(this.text, other.text);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
