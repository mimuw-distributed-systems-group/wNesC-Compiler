package pl.edu.mimuw.nesc.facade.component;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.type.Type;

/**
 * <p>Interface with operations related to obtaining information about
 * declarations of specifications of components.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface ComponentRefFacade {
    /**
     * <p>Check if the component reference is correct, i.e. the referred
     * component exists, has been successfully parsed and analyzed and if it is
     * generic, then all parameters are correctly given.</p>
     *
     * @return <code>true</code> if and only if the reference to the component
     *         is valid.
     */
    boolean goodComponentRef();

    /**
     * <p>Get name of the referred component.</p>
     *
     * @return Name of the referred component.
     */
    String getComponentName();

    /**
     * <p>Get the name of the component used to refer to it in
     * the configuration.</p>
     *
     * @return Name used to refer to the component in the configuration.
     */
    String getInstanceName();

    /**
     * <p>Check if the specification of the referred component contains a type
     * definition with given name. If so, the object is present and the type is
     * in the nested object. If the type is present, it is after performing all
     * necessary substitutions.</p>
     *
     * @param name Name of the type definition to lookup.
     * @return The less nested object is present if the specification contains
     *         a type definition with the given name. The more nested object is
     *         present if the type definition is correct. If so, it is after all
     *        substitutions.
     * @throws NullPointerException Name is null.
     * @throws IllegalArgumentException Name is an empty string.
     */
    Optional<Optional<Type>> getTypedef(String name);

    /**
     * <p>Check if the specification of the referred component contains an
     * enumeration constant with given name.</p>
     *
     * @param name Name of the enumeration constant.
     * @return <code>true</code> if and only if the enumeration constant with
     *         given name exists in the specification of the referred component.
     * @throws NullPointerException Name is null.
     * @throws IllegalArgumentException Name is an empty string.
     */
    boolean containsConstant(String name);

    /**
     * <p>Get the object that depicts an interface reference or a bare command
     * or event with given name in the referred component. All types provided by
     * the returned object (if present) are after performing all necessary type
     * substitutions.</p>
     *
     * @param name Name of the interface reference or a bare command or event.
     * @return Object that depicts the interface reference or a bare command or
     *         event with given name. It is absent if the specification of the
     *         referred component does not contain any interface or bare command
     *         or event with given name.
     */
    Optional<SpecificationEntity> get(String name);

    /**
     * <p>Get a set with all interfaces and bare commands and events that are
     * declared in the specification of the referred component. All types in
     * <code>SpecificationEntity</code> objects are after all necessary
     * substitutions.</p>
     *
     * @return Immutable set with all interfaces and bare commands or events
     *         declared in the specification of the referred component.
     */
    ImmutableSet<Map.Entry<String, SpecificationEntity>> getAll();
}
