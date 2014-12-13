package pl.edu.mimuw.nesc.problem.issue;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.facade.iface.InterfaceEntity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static pl.edu.mimuw.nesc.problem.issue.IssuesUtils.getInterfaceEntityText;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class MissingImplementationElementError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.MISSING_IMPLEMENTATION_ELEMENT);
    public static final Code CODE = _CODE;

    private final String description;

    public static MissingImplementationElementError interfaceEntity(InterfaceEntity.Kind entityKind,
            String name, Optional<String> interfaceName) {

        checkNotNull(entityKind, "entity kind cannot be null");
        checkNotNull(name, "name of a command or event cannot be null");
        checkNotNull(interfaceName, "interface name cannot be null");
        checkArgument(!name.isEmpty(), "name of a command or event cannot be an empty string");
        checkArgument(!interfaceName.isPresent() || !interfaceName.get().isEmpty(),
                "name of the interface cannot be an empty string");

        final String description = interfaceName.isPresent()
                ? format("%s '%s' required by interface '%s' is not implemented",
                         getInterfaceEntityText(entityKind, true), name, interfaceName.get())
                : format("Bare %s '%s' is not implemented",
                         getInterfaceEntityText(entityKind, false), name);

        return new MissingImplementationElementError(description);
    }

    public static MissingImplementationElementError task(String name) {
        checkNotNull(name, "name of the task cannot be null");
        checkArgument(!name.isEmpty(), "name of the task cannot be an empty string");

        final String description = format("Task '%s' is not implemented", name);
        return new MissingImplementationElementError(description);
    }

    private MissingImplementationElementError(String description) {
        super(_CODE);

        checkNotNull(description, "description cannot be null");
        checkArgument(!description.isEmpty(), "description cannot be an empty string");

        this.description = description;
    }

    @Override
    public String generateDescription() {
        return description;
    }

}
