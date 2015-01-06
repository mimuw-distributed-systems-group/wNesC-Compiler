package pl.edu.mimuw.nesc.facade.iface;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Map;
import pl.edu.mimuw.nesc.type.Type;

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
     * @return Value indicating if the interface must be provided. If not, it is
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
     * <p>Get the object that depicts the command or event with given name.
     * The function type in the object is after all necessary substitutions that
     * are implied by instantiating a generic interface.</p>
     *
     * @param name Name of a command or event.
     * @return Object that depicts the command or event with given name. It is
     *         absent if the referred interface does not contain any command or
     *         event with given name.
     * @throws NullPointerException Name is null.
     * @throws IllegalArgumentException Name is an empty string.
     */
    Optional<InterfaceEntity> get(String name);

    /**
     * <p>Get the set with objects that depict all commands and events from the
     * referred interface. Function types in these objects are after all
     * necessary substitutions that are implied by instantiating a generic
     * interface. Keys of the entries are commands and events names.</p>
     *
     * @return Immutable set that allows identifying all commands and events
     *         from the referred interface.
     */
    ImmutableSet<Map.Entry<String, InterfaceEntity>> getAll();
}
