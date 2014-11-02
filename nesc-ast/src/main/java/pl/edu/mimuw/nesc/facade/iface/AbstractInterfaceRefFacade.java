package pl.edu.mimuw.nesc.facade.iface;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.declaration.object.InterfaceRefDeclaration;

import static com.google.common.base.Preconditions.*;

/**
 * Skeletal implementation of the facade interface for an interface reference.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class AbstractInterfaceRefFacade implements InterfaceRefFacade {

    /**
     * The declaration that is behind the facade.
     */
    protected final InterfaceRefDeclaration declaration;

    /**
     * Stores values from given parameters in the object.
     *
     * @param declaration The interface reference declaration that is behind the
     *                    facade.
     * @throws NullPointerException Given argument is null.
     */
    protected AbstractInterfaceRefFacade(InterfaceRefDeclaration declaration) {
        checkNotNull(declaration, "the declaration cannot be null");
        this.declaration = declaration;
    }

    @Override
    public boolean isProvided() {
        return declaration.isProvides();
    }

    @Override
    public String getInterfaceName() {
        return declaration.getIfaceName();
    }

    @Override
    public String getInstanceName() {
        return declaration.getName();
    }

    @Override
    public Optional<ImmutableList<Optional<Type>>> getInstanceParameters() {
        return declaration.getInstanceParameters();
    }

    /**
     * <p>Check the correctness of the given name of a command or event: if it
     * is not null or if it is not empty string. If so, a proper exception is
     * thrown.</p>
     *
     * @param name Name of a command or event to check.
     */
    protected void checkName(String name) {
        checkNotNull(name, "name of a command or event cannot be null");
        checkArgument(!name.isEmpty(), "name of a command or event cannot be an empty string");
    }
}
