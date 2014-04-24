package pl.edu.mimuw.nesc.lexer;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.preprocessor.directive.PreprocessorDirective;
import pl.edu.mimuw.nesc.token.MacroToken;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class TestLexerListener implements LexerListener {

    @Override
    public void fileChanged(Optional<String> from, String to, boolean push) {

    }

    @Override
    public boolean beforeInclude(String filePath, int line) {
        return true;
    }

    @Override
    public void preprocessorDirective(PreprocessorDirective preprocessorDirective) {

    }

    @Override
    public void comment(Comment comment) {

    }

    @Override
    public void error(String fileName, int startLine, int startColumn,
                      Optional<Integer> endLine, Optional<Integer> endColumn, String message) {

    }

    @Override
    public void warning(String fileName, int startLine, int startColumn,
                        Optional<Integer> endLine, Optional<Integer> endColumn, String message) {

    }

    @Override
    public void macroInstantiation(MacroToken macroToken) {

    }
}
