package pl.edu.mimuw.nesc;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContextRef that = (ContextRef) o;

        if (id != that.id) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "ContextRef{" +
                "id=" + id +
                '}';
    }
}
