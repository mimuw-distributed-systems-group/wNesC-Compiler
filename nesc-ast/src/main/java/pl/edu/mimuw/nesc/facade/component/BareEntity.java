package pl.edu.mimuw.nesc.facade.component;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.ast.type.FunctionType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.facade.iface.InterfaceEntity;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A specification entity that represents a bare command or event.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class BareEntity extends SpecificationEntity {
    /**
     * Type of this bare entity.
     */
    private final Optional<FunctionType> type;

    /**
     * Value indicating if this is a bare command or a bare event.
     */
    private final InterfaceEntity.Kind ifaceEntityKind;

    public BareEntity(boolean isProvided, String name, Optional<FunctionType> type, InterfaceEntity.Kind ifaceEntityKind,
            Optional<ImmutableList<Optional<Type>>> instanceParameters) {
        super(isProvided, name, instanceParameters);

        checkNotNull(type, "type cannot be null");
        checkNotNull(ifaceEntityKind, "interface entity kind cannot be null");

        this.type = type;
        this.ifaceEntityKind = ifaceEntityKind;
    }

    @Override
    public Kind getKind() {
        return Kind.BARE;
    }

    @Override
    public Optional<FunctionType> getType() {
        return type;
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * Check if this is a bare command or a bare event.
     *
     * @return Value indicating if this is a bare command or a bare event.
     */
    public InterfaceEntity.Kind getInterfaceEntityKind() {
        return ifaceEntityKind;
    }
}
