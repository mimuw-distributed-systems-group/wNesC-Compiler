package pl.edu.mimuw.nesc.lexer;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.preprocessor.directive.PreprocessorDirective;

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
     * @return <code>true</code> if specified file should be skipped,
     * <code>false</code> otherwise
     */
    boolean beforeInclude(String filePath);

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

}
