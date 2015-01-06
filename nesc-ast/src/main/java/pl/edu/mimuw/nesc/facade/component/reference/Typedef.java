package pl.edu.mimuw.nesc.facade.component.reference;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.type.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class that represents a type definition from the specification of
 * a component.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class Typedef {
    /**
     * Name of the type definition.
     */
    private final String name;

    /**
     * Unique name of this type definition.
     */
    private final String uniqueName;

    /**
     * Type denoted by this type definition.
     */
    private final Optional<Type> type;

    Typedef(String name, String uniqueName, Optional<Type> type) {
        checkNotNull(name, "name cannot be null");
        checkNotNull(uniqueName, "unique name cannot be null");
        checkNotNull(type, "type cannot be null");
        checkArgument(!name.isEmpty(), "name cannot be an empty string");
        checkArgument(!uniqueName.isEmpty(), "unique name cannot be an empty string");

        this.name = name;
        this.uniqueName = uniqueName;
        this.type = type;
    }

    /**
     * Get the name of this type definition.
     *
     * @return Name of this type definition.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the mangled, globally unique name of this type definition.
     *
     * @return Unique name of this type definition.
     */
    public String getUniqueName() {
        return uniqueName;
    }

    /**
     * Get the type denoted by this type definition.
     *
     * @return Type denoted by this type definition.
     */
    public Optional<Type> getType() {
        return type;
    }
}
