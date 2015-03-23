package pl.edu.mimuw.nesc.problem;

import com.google.common.base.Optional;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.Paths;
import pl.edu.mimuw.nesc.ast.Location;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Visitor that prints issues it visits using a writer created at construction.
 * Each printed issue is ended by a newline character.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class NescIssuePrinter {
    /**
     * String used as the separator for tokens of location.
     */
    private static final String SEPARATOR = ":";

    /**
     * The writer used for printing issues.
     */
    private final PrintWriter output;

    /**
     * Visitor that prints issues that it visits.
     */
    private final WritingVisitor issueSwitch = new WritingVisitor();

    public NescIssuePrinter(OutputStream outputStream, String charset)
            throws UnsupportedEncodingException {
        checkNotNull(outputStream, "output stream cannot be null");
        checkNotNull(charset, "character set cannot be null");
        checkArgument(!charset.isEmpty(), "charset cannot be an empty string");

        this.output = new PrintWriter(new OutputStreamWriter(outputStream, charset));
    }

    public NescIssuePrinter(Writer writer) {
        checkNotNull(writer, "writer cannot be null");

        this.output = writer instanceof PrintWriter
                ? (PrintWriter) writer
                : new PrintWriter(writer);
    }

    public void print(NescIssue issue) {
        checkNotNull(issue, "issue cannot be null");
        issue.accept(issueSwitch, null);
    }

    private void printIssue(String issueType, NescIssue issue) {
        // Start location

        final Optional<Location> startLocation = issue.getStartLocation();
        if (startLocation.isPresent()) {
            output.print(Paths.get(startLocation.get().getFilePath()).getFileName());
            output.print(SEPARATOR);
            output.print(startLocation.get().getLine());
            output.print(SEPARATOR);
            output.print(startLocation.get().getColumn());
            output.print(SEPARATOR);
            output.print(" ");
        }

        // Type of the issue

        output.print(issueType);

        if (issue.getCode().isPresent()) {
            output.print(" ");
            output.print(issue.getCode().get().asString());
        }

        output.print(SEPARATOR);
        output.print(" ");

        // Message

        output.println(issue.getMessage());
        output.flush();
    }

    /**
     * Visitor that actually prints visited issues.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class WritingVisitor implements NescIssue.Visitor<Void, Void> {
        @Override
        public Void visit(NescWarning warning, Void arg) {
            printIssue("warning", warning);
            return null;
        }

        @Override
        public Void visit(NescError error, Void arg) {
            printIssue("error", error);
            return null;
        }
    }
}
