package pl.edu.mimuw.nesc.facade.component;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.ast.type.InterfaceType;
import pl.edu.mimuw.nesc.ast.type.Type;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InterfaceRefEntity extends SpecificationEntity {
    /**
     * Type of this interface reference.
     */
    private final InterfaceType type;

    public InterfaceRefEntity(boolean isProvided, String name, InterfaceType type,
            Optional<ImmutableList<Optional<Type>>> instanceParameters) {

        super(isProvided, name, instanceParameters);
        checkNotNull(type, "type cannot be null");
        this.type = type;
    }

    @Override
    public Kind getKind() {
        return Kind.INTERFACE;
    }

    @Override
    public Optional<InterfaceType> getType() {
        return Optional.of(type);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
