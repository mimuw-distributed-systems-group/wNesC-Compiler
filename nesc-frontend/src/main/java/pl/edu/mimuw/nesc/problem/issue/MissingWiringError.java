package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.facade.component.specification.WiringElement;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class MissingWiringError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.MISSING_WIRING);
    public static final Code CODE = _CODE;

    private final String name;
    private final WiringElement.Kind kind;

    public MissingWiringError(String name, WiringElement.Kind kind) {
        super(_CODE);

        checkNotNull(name, "name cannot be null");
        checkNotNull(kind, "kind cannot be null");
        checkArgument(!name.isEmpty(), "name cannot be an empty string");

        this.name = name;
        this.kind = kind;
    }

    @Override
    public String generateDescription() {
        final String kindText;

        switch (kind) {
            case BARE_COMMAND:
                kindText = "Bare command";
                break;
            case BARE_EVENT:
                kindText = "Bare event";
                break;
            case INTERFACE:
                kindText = "Interface";
                break;
            default:
                throw new RuntimeException("unexpected specification kind '" + kind + "'");
        }

        return format("%s '%s' is not wired", kindText, name);
    }
}
