package pl.edu.mimuw.nesc.problem.issue;

import com.google.common.base.Optional;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidTypeSpecifiersMixError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.TYPE_SPECIFIERS_MIX_ERROR);
    public static final Code CODE = _CODE;

    private final InvalidCombinationType combinationType;
    private final Optional<String> specifierText;

    public InvalidTypeSpecifiersMixError(InvalidCombinationType combinationType,
                                         Optional<String> specifierText) {
        super(_CODE);

        checkNotNull(combinationType, "invalid combination type cannot be null");
        checkNotNull(specifierText, "specifier text cannot be null");

        this.combinationType = combinationType;
        this.specifierText = specifierText;
    }

    @Override
    public String generateDescription() {
        Optional<String> description = Optional.absent();

        switch (combinationType) {
            case ATTRIBUTE_WITH_ATTRIBUTE:
                description = Optional.of("Only one attribute definition can be specified in a single declaration");
                break;
            case ATTRIBUTE_WITH_OTHER:
                description = Optional.of("Cannot combine an attribute definition with previously used type specifiers");
                break;
            case SIMPLE_WITH_TAG:
                if (specifierText.isPresent()) {
                    description = Optional.of(
                            format("Cannot combine '%s' type specifier with a tag type specifier",
                                   specifierText.get())
                    );
                }
                break;
            case SIMPLE_WITH_TYPENAME:
                if (specifierText.isPresent()) {
                    description = Optional.of(
                            format("Cannot combine '%s' type specifier with a typename type specifier",
                                   specifierText.get())
                    );
                }
                break;
            case TAG_WITH_OTHER:
                description = Optional.of("Cannot combine a tag type specifier with previously used type specifiers");
                break;
            case TYPENAME_WITH_OTHER:
                if (specifierText.isPresent()) {
                    description = Optional.of(
                            format("Cannot combine '%s' type specifier with previously used type specifiers",
                                   specifierText.get())
                    );
                }
                break;
            case SIMPLE_WITH_ATTRIBUTE:
                if (specifierText.isPresent()) {
                    description = Optional.of(
                            format("Cannot combine '%s' type specifier with an attribute definition",
                                   specifierText.get())
                    );
                }
                break;
            case TAG_WITH_ATTRIBUTE:
                description = Optional.of("Cannot combine a tag type specifier with an attribute definition");
                break;
            case TYPENAME_WITH_ATTRIBUTE:
                if (specifierText.isPresent()) {
                    description = Optional.of(
                            format("Cannot combine '%s' type specifier with an attribute definition",
                                   specifierText.get())
                    );
                }
                break;
        }

        return description.or("Invalid combination of type specifiers");
    }

    public enum InvalidCombinationType {
        ATTRIBUTE_WITH_ATTRIBUTE,
        ATTRIBUTE_WITH_OTHER,
        SIMPLE_WITH_TAG,
        SIMPLE_WITH_TYPENAME,
        SIMPLE_WITH_ATTRIBUTE,
        TAG_WITH_ATTRIBUTE,
        TAG_WITH_OTHER,
        TYPENAME_WITH_OTHER,
        TYPENAME_WITH_ATTRIBUTE,
        SIMPLE_WITH_SIMPLE
    }
}
