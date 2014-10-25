package pl.edu.mimuw.nesc.facade;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.ast.type.Type;

/**
 * <p>Interface with common operations that are related to interface references
 * in components specifications. It allows obtaining information from both the
 * interface reference declaration object and the interface definition
 * itself.</p>
 * <p>It follows the facade pattern.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface InterfaceRefFacade {
    /**
     * <p>Check if the definition of the interface that is referred has been
     * located, successfully parsed and is available for querying by this
     * object and if the interface has been properly instantiated if it is
     * generic.</p>
     *
     * @return <code>true</code> if and only if the definition is alright as
     *         described above.
     */
    boolean goodInterfaceRef();

    /**
     * <p>Check if the interface that is referred is to be provided or used.</p>
     *
     * @return Value indicating if the interface must be provided. If not, is is
     *         used.
     */
    boolean isProvided();

    /**
     * <p>Get the name of the referred interface.</p>
     *
     * @return Name of the referred interface.
     */
    String getInterfaceName();

    /**
     * <p>Get the names used in a component to refer to the interface
     * reference.</p>
     *
     * @return Instance name of the interface.
     */
    String getInstanceName();

    /**
     * <p>Get the instance parameters of the interface reference. Instance
     * parameters are the parameters in brackets:</p>
     * <pre>
     *     provides interface Resource[uint8_t id];
     *                                 ▲        ▲
     *                                 |        |
     *                                 |        |
     *                                 |        |
     * </pre>
     *
     * @return Instance parameters of the interface if they are used. Otherwise,
     *         the object is absent.
     */
    Optional<ImmutableList<Optional<Type>>> getInstanceParameters();

    /**
     * <p>Check if the interface that is behind this facade contains a command
     * or event with given name.</p>
     *
     * @param name Name of a command or event.
     * @return <code>true</code> if and only if the referred interface contains
     *         a command or event with given name.
     * @throws NullPointerException Name is null.
     * @throws IllegalArgumentException Name is an empty string.
     */
    boolean contains(String name);

    /**
     * <p>Check if the entity with given name in the referred interface is
     * a command or event.</p>
     *
     * @param name Name of a command or event.
     * @return Kind of the entity with given name. The object is absent if the
     *         referred interface does not contain any command or event with
     *         given name.
     * @throws NullPointerException Name is null.
     * @throws IllegalArgumentException Name is an empty string.
     */
    Optional<InterfaceEntityKind> getKind(String name);

    /**
     * <p>Get the return type of a command or event with given name in the
     * referred interface. The type is after all necessary substitutions that
     * are implied by instantiating a generic interface.</p>
     *
     * @param name Name of a command or event.
     * @return Return type of the command or event with given name in the
     *         referred interface. The object is absent if the type is specified
     *         incorrectly or the interface does not contain any command or
     *         event with given name.
     * @throws NullPointerException Name is null.
     * @throws IllegalArgumentException Name is an empty string.
     */
    Optional<Type> getReturnType(String name);

    /**
     * <p>Get the types of arguments of a command or event with given name in
     * the referred interface. The types are after all necessary substitutions
     * that are implied by instantiating a generic interface.</p>
     *
     * @param name Name of a command or event.
     * @return Immutable list with types of arguments of a command or event with
     *         given name in the referred interface. It is absent if the
     *         referred interface does not contain a command or event with given
     *         name.
     * @throws NullPointerException Name is null.
     * @throws IllegalArgumentException Name is an empty string.
     */
    Optional<ImmutableList<Optional<Type>>> getArgumentsTypes(String name);

    /**
     * Enumeration type that represents an entity that can be declared inside an
     * interface.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    enum InterfaceEntityKind {
        COMMAND,
        EVENT,
    }
}
