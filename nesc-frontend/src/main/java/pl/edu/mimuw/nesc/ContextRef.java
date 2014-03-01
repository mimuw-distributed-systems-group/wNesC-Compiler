package pl.edu.mimuw.nesc;

import com.google.common.base.Objects;

/**
 * <p>Context reference.</p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class ContextRef {

    private static int idCounter = 0;

    private static synchronized int getNewId() {
        return idCounter++;
    }

    private final int id;

    public ContextRef() {
        this.id = ContextRef.getNewId();
    }

    public int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ContextRef other = (ContextRef) obj;
        return Objects.equal(this.id, other.id);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("id", id)
                .toString();
    }
}
