package pl.edu.mimuw.nesc.facade.iface;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.type.FunctionType;

import static com.google.common.base.Preconditions.*;

/**
 * <p>A class that represents a command or an event declared in an interface.
 * <code>FunctionDeclaration</code> objects are not sufficient to represent
 * commands and events because of the need to perform types substitution in
 * a&nbsp;generic interface.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 * @see pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration FunctionDeclaration
 */
public final class InterfaceEntity {
    /**
     * Type of this command or event that contains its return type and types of
     * its arguments. The type can be the result of performing all substitutions
     * if an interface is instantiated but this is not a strict requirement.
     */
    private final Optional<FunctionType> type;

    /**
     * Kind of this entity: whether it is a command or an event.
     */
    private final Kind kind;

    /**
     * Name of this command or event.
     */
    private final String name;

    /**
     * Initialize this interface entity by storing given values in member
     * fields.
     *
     * @param kind Kind of the interface entity (command or event).
     * @param type Type of the interface entity.
     * @param name Name of the command or event.
     * @throws NullPointerException One of the arguments is null.
     * @throws IllegalArgumentException The name is an empty string.
     */
    InterfaceEntity(Kind kind, Optional<FunctionType> type, String name) {
        checkNotNull(kind, "kind of the interface entity cannot be null");
        checkNotNull(type, "type of the interface entity cannot be null");
        checkNotNull(name, "name of the interface entity cannot be null");
        checkArgument(!name.isEmpty(), "name of the interface entity cannot be an empty string");

        this.type = type;
        this.kind = kind;
        this.name = name;
    }

    /**
     * Get the return type and types of arguments of this interface entity.
     *
     * @return Function type that contains information about the return type
     *         and types of arguments of this command or event. The type is
     *         absent if it is specified incorrectly in the interface
     *         definition.
     */
    public Optional<FunctionType> getType() {
        return type;
    }

    /**
     * Get the kind of this interface entity: if it is a command or an event.
     *
     * @return Kind of this interface entity.
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Get the name of this command or event.
     *
     * @return Name of this command or event.
     */
    public String getName() {
        return name;
    }

    /**
     * Enumeration type that represents an entity that can be declared inside an
     * interface.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum Kind {
        COMMAND,
        EVENT,
    }
}
