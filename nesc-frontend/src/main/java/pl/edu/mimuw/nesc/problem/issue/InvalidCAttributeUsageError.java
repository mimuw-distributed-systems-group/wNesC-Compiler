package pl.edu.mimuw.nesc.problem.issue;

import org.omg.CORBA.DynAnyPackage.Invalid;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidCAttributeUsageError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_CATTRIBUTE_USAGE);
    public static final Code CODE = _CODE;

    private final String description;

    public static InvalidCAttributeUsageError invalidScope() {
        final String description = "Attribute @C() cannot be used in this context; it must be specified in the implementation scope of a non-generic module";
        return new InvalidCAttributeUsageError(description);
    }

    public static InvalidCAttributeUsageError usageInGenericComponent() {
        final String description = "Attribute @C() cannot be used inside a generic component";
        return new InvalidCAttributeUsageError(description);
    }

    public static InvalidCAttributeUsageError usageForNescObject() {
        final String description = "Attribute @C() cannot be applied to a command, an event or a task";
        return new InvalidCAttributeUsageError(description);
    }

    public static InvalidCAttributeUsageError internalLinkage() {
        final String description = "Attribute @C() cannot be applied to an object with internal linkage";
        return new InvalidCAttributeUsageError(description);
    }

    public static InvalidCAttributeUsageError appliedToAttribute() {
        final String description = "Attribute @C() cannot be applied to an attribute definition";
        return new InvalidCAttributeUsageError(description);
    }

    public static InvalidCAttributeUsageError appliedToUnnamedTag() {
        final String description = "Attribute @C() cannot be applied to an anonymous tag";
        return new InvalidCAttributeUsageError(description);
    }

    public static InvalidCAttributeUsageError appliedToInvalidEntity() {
        final String description = "Attribute @C() cannot be used in this context; it must be applied to a variable, a function, a tag or a type definition";
        return new InvalidCAttributeUsageError(description);
    }

    public static InvalidCAttributeUsageError conflictWithGlobalDeclaration(String name) {
        final String description = format("'%s' with @C() attribute clashes with declaration of the same name in the global scope",
                name);
        return new InvalidCAttributeUsageError(description);
    }

    private InvalidCAttributeUsageError(String description) {
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
