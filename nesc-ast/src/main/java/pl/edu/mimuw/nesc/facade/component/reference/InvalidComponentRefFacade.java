package pl.edu.mimuw.nesc.facade.component.reference;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.declaration.object.ComponentRefDeclaration;

/**
 * <p>A facade for an invalid component reference. It acts like the
 * specification of the referred component was empty.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidComponentRefFacade extends AbstractComponentRefFacade {

    /**
     * Initializes this facade with the given declaration.
     *
     * @param declaration Declaration that represents the component reference.
     * @throws NullPointerException Given argument is null.
     */
    public InvalidComponentRefFacade(ComponentRefDeclaration declaration) {
        super(declaration);
    }

    @Override
    public boolean goodComponentRef() {
        return false;
    }

    @Override
    public Optional<Typedef> getTypedef(String name) {
        checkName(name);
        return Optional.absent();
    }

    @Override
    public Optional<EnumerationConstant> getEnumerationConstant(String name) {
        checkName(name);
        return Optional.absent();
    }

    @Override
    public Optional<SpecificationEntity> get(String name) {
        checkName(name);
        return Optional.absent();
    }

    @Override
    public ImmutableSet<Map.Entry<String, SpecificationEntity>> getAll() {
        return ImmutableSet.of();
    }
}
