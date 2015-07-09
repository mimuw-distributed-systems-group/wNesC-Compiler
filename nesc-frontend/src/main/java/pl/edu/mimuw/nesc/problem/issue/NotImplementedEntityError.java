package pl.edu.mimuw.nesc.problem.issue;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import pl.edu.mimuw.nesc.ast.InstantiationOrigin;
import pl.edu.mimuw.nesc.facade.iface.InterfaceEntity;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class NotImplementedEntityError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.NOT_IMPLEMENTED_ENTITY);
    public static final Code CODE = _CODE;

    private final String description;

    public static NotImplementedEntityError entityFromInstantiatedComponent(
            ImmutableList<InstantiationOrigin> instantiationChain,
            Optional<String> interfaceRefName,
            String entityName,
            InterfaceEntity.Kind kind
    ) {
        // Create the identification text for the component
        final StringBuilder identificationTextBuilder = new StringBuilder("instantiated component ");
        final Iterator<InstantiationOrigin> chainIt = instantiationChain.iterator();
        identificationTextBuilder.append('\'');
        identificationTextBuilder.append(chainIt.next().getComponentName());
        identificationTextBuilder.append('\'');
        while (chainIt.hasNext()) {
            final InstantiationOrigin origin = chainIt.next();
            identificationTextBuilder.append(" -> '");
            identificationTextBuilder.append(origin.getComponentRefName().get());
            identificationTextBuilder.append('\'');

            if (!origin.getComponentName().equals(origin.getComponentRefName().get())) {
                identificationTextBuilder.append(" ('");
                identificationTextBuilder.append(origin.getComponentName());
                identificationTextBuilder.append("')");
            }
        }

        return notImplementedEntity(identificationTextBuilder.toString(), interfaceRefName,
                entityName, kind);
    }

    public static NotImplementedEntityError entityFromNormalComponent(String componentName,
            Optional<String> interfaceRefName, String entityName, InterfaceEntity.Kind entityKind) {
        return notImplementedEntity("component '" + componentName + "'", interfaceRefName,
                entityName, entityKind);
    }

    private static NotImplementedEntityError notImplementedEntity(String componentDescription,
             Optional<String> interfaceRefName, String entityName, InterfaceEntity.Kind entityKind) {
        final StringBuilder descriptionBuilder = new StringBuilder();

        // 'Bare' word
        if (!interfaceRefName.isPresent()) {
            descriptionBuilder.append("Bare ");
        }

        // 'command' or 'event' word
        descriptionBuilder.append(IssuesUtils.getInterfaceEntityText(entityKind,
                interfaceRefName.isPresent()));

        // Name of the command or event
        descriptionBuilder.append(" '");
        descriptionBuilder.append(entityName);
        descriptionBuilder.append('\'');

        // Information about interface
        if (interfaceRefName.isPresent()) {
            descriptionBuilder.append(" from interface '");
            descriptionBuilder.append(interfaceRefName.get());
            descriptionBuilder.append('\'');
        }

        // Information about the component
        descriptionBuilder.append(" in ");
        descriptionBuilder.append(componentDescription);

        // Information about the error
        descriptionBuilder.append(" is not connected and it lacks a default implementation");

        return new NotImplementedEntityError(descriptionBuilder.toString());
    }

    private NotImplementedEntityError(String description) {
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
