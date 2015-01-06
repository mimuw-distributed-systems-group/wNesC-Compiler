package pl.edu.mimuw.nesc.problem.issue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.facade.iface.InterfaceEntity;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class IssuesUtils {
    /**
     * Regular expression used for creating compact texts for types.
     */
    private static final Pattern COMPACT_TYPE_PATTERN = Pattern.compile("interface (?<text>.*)");

    /**
     * Get the ordinal form of the given number.
     *
     * @param n Number to process.
     * @return Ordinal form of the given number.
     */
    static String getOrdinalForm(int n) {
        switch(n) {
            case 1:
                return "1st";
            case 2:
                return "2nd";
            case 3:
                return "3rd";
            default:
                return n + "th";
        }
    }

    /**
     * Get the text for given entity kind.
     *
     * @param kind Kind of the entity to prepare a text for.
     * @param firstLetterCapital Value indicating if the first letter of the
     *                           returned text is a capital letter.
     * @return Text generated with regards to given parameters.
     */
    static String getInterfaceEntityText(InterfaceEntity.Kind kind, boolean firstLetterCapital) {
        switch (kind) {
            case COMMAND:
                return firstLetterCapital ? "Command" : "command";
            case EVENT:
                return firstLetterCapital ? "Event" : "event";
            default:
                throw new RuntimeException("unexpected interface entity kind: " + kind);
        }
    }

    /**
     * Get the compact textual representation of given type. Its result is the
     * same as {@link Type#toString} but if it begins with the word
     * <code>interface</code>, the word is removed.
     *
     * @param type Type for creation of type compact text.
     * @return Compact textual representation of the given type.
     */
    static String getCompactTypeText(Type type) {
        final String fullTypeText = type.toString();
        final Matcher matcher = COMPACT_TYPE_PATTERN.matcher(fullTypeText);

        return matcher.matches()
                ? matcher.group("text")
                : fullTypeText;
    }

    /**
     * Get the adjective for the given provided value.
     *
     * @param provided Value indicating if a specification element is provided.
     * @param firstLetterCapital Value indicating if the first letter of the
     *                           returned string shall be capital.
     * @return Text for the given provided value.
     */
    static String getProvidedText(boolean provided, boolean firstLetterCapital) {
        if (firstLetterCapital) {
            return provided ? "Provided" : "Used";
        } else {
            return provided ? "provided" : "used";
        }
    }

    /**
     * Get the conjugation of the verb 'to be' to be combined with a noun of
     * plurality given by the argument.
     *
     * @param n Plurality of the noun to be combined with the verb 'to be'.
     * @return 'is' if 'n' is equal to 1 or 0, 'are' otherwise.
     */
    static String getToBeConjugation(int n) {
        return n == 0 || n == 1
                ? "is"
                : "are";
    }

    /**
     * Private constructor to prevent this class from being instantiated.
     */
    private IssuesUtils() {
    }
}
