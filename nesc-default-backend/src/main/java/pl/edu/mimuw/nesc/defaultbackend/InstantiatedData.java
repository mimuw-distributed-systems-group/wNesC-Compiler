package pl.edu.mimuw.nesc.defaultbackend;

import pl.edu.mimuw.nesc.ast.gen.Component;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Data of a component that exists because of being instantiated.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class InstantiatedData extends ProcessedNescData {
    /**
     * The instantiated component.
     */
    private final Component component;

    InstantiatedData(Component component) {
        checkNotNull(component, "component cannot be null");
        checkNotNull(component.getInstantiatedComponentName(),
                "name of the instantiated component in the given component cannot be null");
        checkArgument(component.getInstantiatedComponentName().isPresent(),
                "name of the instantiated component in the given component cannot be absent");
        checkArgument(!component.getInstantiatedComponentName().get().isEmpty(),
                "name of the instantiated component in the given component cannot be an empty string");

        this.component = component;
    }

    /**
     * Get the instantiated component contained in this data object.
     *
     * @return The instantiated component carried by this data object.
     */
    Component getComponent() {
        return component;
    }

    /**
     * Get the name of the generic component that has been instantiated to
     * create the component associated with this data object.
     *
     * @return Name of the "parent" generic component.
     */
    String getInstantiatedComponentName() {
        return component.getInstantiatedComponentName().get();
    }

    @Override
    <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
