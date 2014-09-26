package pl.edu.mimuw.nesc.problem.issue;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A skeletal implementation of <code>Issue</code> interface.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 * @see Issue
 */
public abstract class AbstractIssue implements Issue {
    /**
     * The code of this issue.
     */
    private final Code code;

    /**
     * The kind of this issue.
     */
    private final Kind kind;

    /**
     * Initialize this issue.
     *
     * @param kind Kind of this issue.
     * @throws NullPointerException One of the arguments is null.
     */
    protected AbstractIssue(Code code, Kind kind) {
        checkNotNull(code, "the issue code cannot be null");
        checkNotNull(kind, "the kind cannot be null");

        this.code = code;
        this.kind = kind;
    }

    @Override
    public final Code getCode() {
        return code;
    }

    @Override
    public final Kind getKind() {
        return kind;
    }

    /**
     * Partial implementation of the <code>Code</code> interface.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     * @see Code Code
     */
    protected static abstract class AbstractCode implements Code {
        /**
         * The code number.
         */
        private final int codeNumber;

        /**
         * Initializes this object setting given code number.
         *
         * @param codeNumber Number of this issue code.
         */
        protected AbstractCode(int codeNumber) {
            this.codeNumber = codeNumber;
        }

        @Override
        public final int getCodeNumber() {
            return codeNumber;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(this)
                    .add("number", codeNumber)
                    .toString();
        }
    }
}
