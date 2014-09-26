package pl.edu.mimuw.nesc.problem.issue;

import java.util.EnumMap;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A class that is used to facilitate creating codes of various types.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
abstract class CodeFactory<C extends Issue.Code, E extends Enum<E> & Issues.EnumCode> {
    /**
     * Map with unique and only instances for individual types of issues.
     */
    private final EnumMap<E, C> codes;

    /**
     * Initialize this factory to utilize codes from enumeration type that the
     * given class represents.
     *
     * @param enumClass Class instance of the enumeration type to use.
     * @throws NullPointerException Given argument is null.
     */
    public CodeFactory(Class<E> enumClass) {
        checkNotNull(enumClass, "enumeration class cannot be null");
        codes = new EnumMap<>(enumClass);
    }

    /**
     * Get the instance of the code class.
     *
     * @param issueType Issue type that the instance will be returned for.
     * @return The unique and only instance of the code class for the given
     *         issue type.
     */
    public C getInstance(E issueType) {
        checkNotNull(issueType, "the issue type cannot be null");

        if (codes.containsKey(issueType)) {
            return codes.get(issueType);
        }

        final C result = newCode(issueType.getCodeNumber());
        codes.put(issueType, result);

        return result;
    }

    /**
     * Get a new instance of the code class.
     *
     * @param codeNumber Code number that will be associated with the newly
     *                   created instance.
     * @return Newly created instance of the code class.
     */
    protected abstract C newCode(int codeNumber);
}
