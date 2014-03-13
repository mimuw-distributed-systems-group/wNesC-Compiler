package pl.edu.mimuw.nesc.token;

import com.google.common.base.Objects;
import pl.edu.mimuw.nesc.ast.Location;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class ConstantToken extends Token {

    protected final String value;

    public ConstantToken(Location startLocation, Location endLocation, String value) {
        super(startLocation, endLocation);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("startLocation", startLocation)
                .add("endLocation", endLocation)
                .add("value", value)
                .toString();
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(value);
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
        final ConstantToken other = (ConstantToken) obj;
        return Objects.equal(this.value, other.value);
    }
}
