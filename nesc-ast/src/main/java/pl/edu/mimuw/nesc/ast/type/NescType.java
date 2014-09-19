package pl.edu.mimuw.nesc.ast.type;

/**
 * Artificial base type for NesC objects: interfaces and components.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class NescType extends ArtificialType {
    @Override
    public final boolean isTypeDefinition() {
        return false;
    }
}
