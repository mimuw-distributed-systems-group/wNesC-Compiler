package pl.edu.mimuw.nesc.compilation;

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
        checkNotNull(component.getInstantiationChain(),
                "instantiation chain in the given component cannot be null");
        checkArgument(!component.getIsAbstract(), "the instantiated component cannot be generic");
        checkArgument(component.getInstantiationChain().isPresent(),
                "instantiation chain in the given component cannot be absent");
        checkArgument(component.getInstantiationChain().get().size() > 1,
                "length of an instantiation chain in a component must have at least two elements");

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
        final int lastIndex = component.getInstantiationChain().get().size() - 1;
        return component.getInstantiationChain().get().get(lastIndex).getComponentName();
    }

    @Override
    <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
