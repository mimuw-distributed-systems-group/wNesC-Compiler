package pl.edu.mimuw.nesc.facade.iface;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import pl.edu.mimuw.nesc.declaration.object.InterfaceRefDeclaration;

/**
 * <p>Facade for interface references that are invalid. An invalid reference
 * is a reference to a non-existent interface, an invalid interface or that
 * is instantiated incorrectly if it refers to a generic interface.</p>
 *
 * <p>This facade acts as if the interface had an empty definition.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidInterfaceRefFacade extends AbstractInterfaceRefFacade {

    /**
     * Stores information from given arguments in the object.
     *
     * @param declaration The declaration that depicts the interface reference.
     * @throws NullPointerException Given argument is null.
     */
    public InvalidInterfaceRefFacade(InterfaceRefDeclaration declaration) {
        super(declaration); // throws if the declaration is null
    }

    @Override
    public boolean goodInterfaceRef() {
        return false;
    }

    @Override
    public boolean contains(String name) {
        checkName(name);
        return false;
    }

    @Override
    public Optional<InterfaceEntity> get(String name) {
        checkName(name);
        return Optional.absent();
    }

    @Override
    public ImmutableSet<Map.Entry<String, InterfaceEntity>> getAll() {
        return ImmutableSet.of();
    }
}
