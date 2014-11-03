package pl.edu.mimuw.nesc.facade.component;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.ast.type.Type;
import static com.google.common.base.Preconditions.*;

/**
 * A class that represents an entity from the specification of a component, e.g.
 * a bare command or a bare event or an interface reference.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class SpecificationEntity {
    /**
     * Value indicating if the interface or bare command or event is provided.
     */
    protected final boolean isProvided;

    /**
     * Name of this entity.
     */
    protected final String name;

    /**
     * Instance parameters of this entity if this is a paramaterised interface
     * or a parameterised bare command or event.
     */
    protected final Optional<ImmutableList<Optional<Type>>> instanceParameters;

    /**
     * Initialize this entity by storing given values in a member fields.
     *
     * @throws NullPointerException Name is null.
     * @throws IllegalArgumentException Name is an empty string.
     */
    protected SpecificationEntity(boolean isProvided, String name,
            Optional<ImmutableList<Optional<Type>>> instanceParameters) {
        checkNotNull(name, "name cannot be null");
        checkNotNull(instanceParameters, "instance parameters cannot be null");
        checkArgument(!name.isEmpty(), "name cannot be an empty string");

        this.isProvided = isProvided;
        this.name = name;
        this.instanceParameters = instanceParameters;
    }

    /**
     * Get the kind of this specification entity: either a bare command or event
     * or an interface reference.
     *
     * @return Kind of this entity.
     */
    public abstract Kind getKind();

    /**
     * Get the name used to refer to this specification entity in the component.
     *
     * @return Name of this specification entity.
     */
    public String getName() {
        return name;
    }

    /**
     * Check if this interface or bare command or event is provided.
     *
     * @return Value indicating if this interface or bare command or event is
     *         provided.
     */
    public boolean isProvided() {
        return isProvided;
    }

    /**
     * Check if this is a parameterised interface or a parameterised bare
     * command or event.
     *
     * @return Value indicating if this entity is parameterised.
     */
    public boolean isParameterised() {
        return instanceParameters.isPresent();
    }

    /**
     * Check if this is a parameterised entity and get its instance parameters.
     *
     * @return Instance parameters if this is a parameterised entity.
     */
    public Optional<ImmutableList<Optional<Type>>> getInstanceParameters() {
        return instanceParameters;
    }

    /**
     * Get type of this interface or bare command and event. It may be absent if
     * it is erroneous. It may be after all necessary substitutions but this is
     * not a strict requirement.
     *
     * @return Type of this entity in the types system.
     */
    public abstract Optional<? extends Type> getType();

    /**
     * Method that allows using the Visitor pattern for specification entities.
     *
     * @param visitor Visitor that will visit this object.
     * @param arg Argument to pass to the visitor method.
     * @param <R> Type of the result.
     * @param <A> Type of the argument.
     * @return Value returned by the given visitor.
     */
    public abstract <R, A> R accept(Visitor<R, A> visitor, A arg);

    /**
     * Visitor interface for a specification entity.
     *
     * @param <R> Type of the result.
     * @param <A> Type of the argument.
     */
    public interface Visitor<R, A> {
        R visit(BareEntity bareEntity, A arg);
        R visit(InterfaceRefEntity ifaceRefEntity, A arg);
    }

    /**
     * Enumeration type that represents the type of the entity.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public enum Kind {
        BARE,
        INTERFACE,
    }
}
