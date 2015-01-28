package pl.edu.mimuw.nesc.constexpr.value.type;

/**
 * <p>Skeletal implementation of a constant type.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class AbstractConstantType implements ConstantType {
    @Override
    public boolean equals(Object obj) {
        return !(obj == null || getClass() != obj.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
