package pl.edu.mimuw.nesc.problem;

import java.util.Comparator;
import pl.edu.mimuw.nesc.ast.Location;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Comparator that compares issues in the following way:</p>
 * <ol>
 *     <li>If one issue has the start location and the other not, the other is
 *     considered lesser.</li>
 *     <li>Otherwise, if both issues don't have the start location, their
 *     messages are compared lexicographically and it is the result.</li>
 *     <li>Otherwise, if names of source files are different, the issue with
 *     the name that is lesser lexicographically is considered lesser.</li>
 *     <li>Otherwise, if numbers of lines and columns are different, the issue
 *     with the earlier location is considered lesser.</li>
 *     <li>Otherwise, the result is the same as messages of issues were compared
 *     lexicographically.</li>
 * </ol>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class NescIssueComparator implements Comparator<NescIssue> {
    @Override
    public int compare(NescIssue issue1, NescIssue issue2) {
        checkNotNull(issue1, "the first issue cannot be null");
        checkNotNull(issue2, "the second issue cannot be null");

        if (!issue1.getStartLocation().isPresent() && issue2.getStartLocation().isPresent()) {
            return -1;
        } else if (issue1.getStartLocation().isPresent() && !issue2.getStartLocation().isPresent()) {
            return 1;
        } else if (!issue1.getStartLocation().isPresent() && !issue2.getStartLocation().isPresent()) {
            return issue1.getMessage().compareTo(issue2.getMessage());
        } else {
            final Location startLoc1 = issue1.getStartLocation().get();
            final Location startLoc2 = issue2.getStartLocation().get();

            if (!startLoc1.getFilePath().equals(startLoc2.getFilePath())) {
                return startLoc1.getFilePath().compareTo(startLoc2.getFilePath());
            } else if (startLoc1.compareTo(startLoc2) != 0) {
                return startLoc1.compareTo(startLoc2);
            } else {
                return issue1.getMessage().compareTo(issue2.getMessage());
            }
        }
    }
}
