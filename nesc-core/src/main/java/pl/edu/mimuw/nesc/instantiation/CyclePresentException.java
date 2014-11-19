package pl.edu.mimuw.nesc.instantiation;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import pl.edu.mimuw.nesc.ast.gen.Component;
import pl.edu.mimuw.nesc.ast.gen.Word;

import static java.lang.String.format;

/**
 * Exception thrown when a cycle in instantiating components is detected.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class CyclePresentException extends Exception {

    static CyclePresentException newInstance(Deque<ConfigurationNode> path, String startComponentName) {

        final Optional<List<String>> componentsPath = getComponentsPath(path);

        if (!componentsPath.isPresent() && startComponentName == null) {
            return new CyclePresentException("A cycle in components instantiations is present");
        } else if (!componentsPath.isPresent()) {
            return new CyclePresentException(format("A cycle in components instantiations that starts in component '%s' is present",
                    startComponentName));
        }

        final StringBuilder builder = new StringBuilder();
        boolean first = true, cycleVisualization = false;

        builder.append("A cycle in components instantiations is present: ");

        for (String componentName : componentsPath.get()) {
            if (!first) {
                builder.append(" -> ");
            }

            if (startComponentName != null && startComponentName.equals(componentName)) {
                builder.append('[');
                cycleVisualization = true;
            }

            builder.append(componentName);
            first = false;
        }

        if (cycleVisualization) {
            builder.append(']');
        }

        return new CyclePresentException(builder.toString());
    }

    private static Optional<List<String>> getComponentsPath(Deque<ConfigurationNode> path) {
        if (path == null) {
            return Optional.absent();
        }

        final List<String> result = new ArrayList<>();

        for (ConfigurationNode node : path) {
            final Optional<String> componentName = getCarefullyComponentName(node);

            if (componentName.isPresent()) {
                result.add(componentName.get());
            } else {
                return Optional.absent();
            }
        }

        return Optional.of(result);
    }

    /**
     * Get the component name from given node carefully.
     *
     * @param node Configuration node to inspect.
     * @return Name of the component associated with given node or absent object
     *         if it is impossible.
     */
    private static Optional<String> getCarefullyComponentName(ConfigurationNode node) {
        if (node == null) {
            return Optional.absent();
        }

        final ComponentData componentData = node.getComponentData();
        if (componentData == null) {
            return Optional.absent();
        }

        final Component astComponent = componentData.getComponent();
        if (astComponent == null) {
            return Optional.absent();
        }

        final Word nameWord = astComponent.getName();
        if (nameWord == null) {
            return Optional.absent();
        }

        return Optional.fromNullable(nameWord.getName());
    }

    private CyclePresentException(String message) {
        super(message);
    }
}
