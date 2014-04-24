package pl.edu.mimuw.nesc.lexer;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.preprocessor.directive.PreprocessorDirective;
import pl.edu.mimuw.nesc.token.MacroToken;

/**
 * Listener for lexer callbacks.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public interface LexerListener {

    /**
     * Called when the preprocessor had switched to other source file.
     *
     * @param from path to file that was previously processed,
     *             <code>Optional.absent()</code> when current file has no
     *             predecessor
     * @param to   path to file that will be processed
     * @param push indicates if a new file will be processed or the processing
     *             of previous file will be resumed
     */
    void fileChanged(Optional<String> from, String to, boolean push);

    /**
     * Called when preprocessor is about to include file.
     *
     * @param filePath file path
     * @param line     line of include directive
     * @return <code>true</code> if specified file should be skipped,
     * <code>false</code> otherwise
     */
    boolean beforeInclude(String filePath, int line);

    /**
     * Called when the preprocessor directive was recognized.
     *
     * @param preprocessorDirective preprocessor directive
     */
    void preprocessorDirective(PreprocessorDirective preprocessorDirective);

    /**
     * Called when the comment was recognized.
     *
     * @param comment comments
     */
    void comment(Comment comment);

    /**
     * Called when error was detected.
     *
     * @param fileName    current file name
     * @param startLine   start location of erroneous token(s)
     * @param startColumn end location of erroneous token(s)
     * @param endLine     optional end line of erroneous token(s)
     * @param endColumn   optional end column of erroneous token(s)
     * @param message     error message
     */
    void error(String fileName, int startLine, int startColumn,
               Optional<Integer> endLine, Optional<Integer> endColumn, String message);

    /**
     * Called when warning was detected.
     *
     * @param fileName    current file name
     * @param startLine   start location of incorrect token(s)
     * @param startColumn end location of incorrect token(s)
     * @param endLine     optional end line of incorrect token(s)
     * @param endColumn   optional end column of incorrect token(s)
     * @param message     warning message
     */
    void warning(String fileName, int startLine, int startColumn,
                 Optional<Integer> endLine, Optional<Integer> endColumn, String message);

    /**
     * Called when a use of a macro was detected.
     *
     * @param macroToken object with information about the use of the macro
     */
    void macroInstantiation(MacroToken macroToken);
}
