package pl.edu.mimuw.nesc.ast.type;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An artificial type that represents a type of an interface, e.g.
 * <code>Queue&lt;unsigned long, signed char&gt;</code>. Objects from the
 * uses-provides clauses have such types.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InterfaceType extends NescType {
    /**
     * Name of the interface that is referred by this interface type.
     * Never null or empty.
     */
    private final String interfaceName;

    /**
     * Type arguments for the interface that form this type. If present, the
     * list should be at least one parameter long.
     */
    private final Optional<List<Optional<Type>>> maybeTypeArguments;

    /**
     * Initializes this interface type with given parameters.
     *
     * @throws NullPointerException One of the arguments is null.
     * @throws IllegalArgumentException <code>interfaceName</code> is an empty
     *                                 string or <code>maybeTypeArguments</code>
     *                                 contains a list with an empty element.
     */
    public InterfaceType(String interfaceName, Optional<List<Optional<Type>>> maybeTypeArguments) {
        // Validate arguments
        checkNotNull(interfaceName, "interface name cannot be null");
        checkNotNull(maybeTypeArguments, "type arguments cannot be null");
        checkArgument(!interfaceName.isEmpty(), "interface name cannot be an empty string");
        if (maybeTypeArguments.isPresent()) {
            for (Optional<Type> type : maybeTypeArguments.get()) {
                checkArgument(type != null, "a type argument for an interface cannot be null");
            }
        }

        // Initialize this object
        this.interfaceName = interfaceName;
        this.maybeTypeArguments =
                  maybeTypeArguments.isPresent()
                ? Optional.of(Collections.unmodifiableList(new ArrayList<>(maybeTypeArguments.get())))
                : Optional.<List<Optional<Type>>>absent();
    }

    /**
     * @return Name of the interface specified by this type. Never null or
     *         empty.
     */
    public final String getInterfaceName() {
        return interfaceName;
    }

    /**
     * @return Type parameters that are part of this interface type. The object
     *         is present if and only if this type is a type of a generic
     *         interface.
     */
    public final Optional<List<Optional<Type>>> getTypeParameters() {
        return maybeTypeArguments;
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
