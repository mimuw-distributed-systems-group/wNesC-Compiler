package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.facade.iface.InterfaceEntity;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class IssuesUtils {
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
     * Private constructor to prevent this class from being instantiated.
     */
    private IssuesUtils() {
    }
}
