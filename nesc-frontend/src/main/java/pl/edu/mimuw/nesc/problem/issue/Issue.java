package pl.edu.mimuw.nesc.problem.issue;

/**
 * <p>Interface with common operations for a particular issue in a code. Classes
 * that implement this interface are supposed not to contain locations
 * because they will be attached after processing the issue in an
 * <code>NescIssue</code> object.</p>
 * <p>The basic difference between the hierarchy rooted at this interface and
 * the hierarchy rooted at <code>NescIssue</code> abstract class is that this
 * hierarchy abstracts the generation of issues messages and the other hierarchy
 * does not. In turn, <code>NescIssue</code> objects store locations associated
 * with issues and already generated messages.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 * @see pl.edu.mimuw.nesc.problem.NescIssue NescIssue
 * @see pl.edu.mimuw.nesc.problem.ErrorHelper ErrorHelper
 */
public interface Issue {
    /**
     * Get the code of this issue.
     *
     * @return Code of this issue.
     */
    Code getCode();

    /**
     * Generate the description of this issue.
     *
     * @return The generated description of the issue.
     */
    String generateDescription();

    /**
     * Get the kind of this issue.
     *
     * @return The kind of this issue.
     */
    Kind getKind();

    /**
     * An enumeration type that represents the kind of an issue.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    enum Kind {
        ERROR,
        WARNING,
    }

    /**
     * Interface for an issue code that allows to identify an issue.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    interface Code {
        /**
         * Get number of this code.
         *
         * @return The number of this code.
         */
        int getCodeNumber();

        /**
         * Get the textual representation of this issue code.
         *
         * @return Textual representation of this issue code.
         */
        String asString();
    }
}
