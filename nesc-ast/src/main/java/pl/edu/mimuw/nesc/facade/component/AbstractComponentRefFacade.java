package pl.edu.mimuw.nesc.facade.component;

import pl.edu.mimuw.nesc.declaration.object.ComponentRefDeclaration;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Skeletal implementation of the component reference facade interface.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class AbstractComponentRefFacade implements ComponentRefFacade {

    /**
     * The declaration that represents the component reference and it is behind
     * the facade.
     */
    protected final ComponentRefDeclaration declaration;

    /**
     * Initialize this facade by storing the given declaration in a member
     * field.
     *
     * @param declaration Declaration that is behind this facade.
     * @throws NullPointerException Given argument is null.
     */
    protected AbstractComponentRefFacade(ComponentRefDeclaration declaration) {
        checkNotNull(declaration, "declaration cannot be null");
        this.declaration = declaration;
    }

    @Override
    public String getComponentName() {
        return declaration.getComponentName().getName();
    }

    @Override
    public String getInstanceName() {
        return declaration.getName();
    }

    protected void checkName(String name) {
        checkNotNull(name, "name cannot be null");
        checkArgument(!name.isEmpty(), "name cannot be an empty string");
    }
}
