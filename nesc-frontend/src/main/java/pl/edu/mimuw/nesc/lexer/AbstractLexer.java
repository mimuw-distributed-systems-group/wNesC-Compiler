package pl.edu.mimuw.nesc.lexer;

import pl.edu.mimuw.nesc.preprocessor.PreprocessorMacro;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Skeleton of lexer.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class AbstractLexer implements Lexer {

    protected final String mainFilePath;
    protected final List<String> userIncludePaths;
    protected final List<String> systemIncludePaths;
    protected final List<String> includeFilePaths;
    protected final Collection<PreprocessorMacro> macros;
    protected final Map<String, String> unparsedMacros;

    protected LexerListener listener;

    protected AbstractLexer(String mainFilePath, List<String> userIncludePaths, List<String> systemIncludePaths,
                            List<String> includeFilePaths, Collection<PreprocessorMacro> macros, Map<String, String> unparsedMacros) {
        this.mainFilePath = mainFilePath;
        this.userIncludePaths = userIncludePaths;
        this.systemIncludePaths = systemIncludePaths;
        this.includeFilePaths = includeFilePaths;
        this.macros = macros;
        this.unparsedMacros = unparsedMacros;
    }

    @Override
    public void setListener(LexerListener listener) {
        checkNotNull(listener, "listener cannot be null");
        this.listener = listener;
    }

    @Override
    public void removeListener() {
        this.listener = null;
    }
}
