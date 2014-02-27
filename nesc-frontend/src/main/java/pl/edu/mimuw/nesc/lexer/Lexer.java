package pl.edu.mimuw.nesc.lexer;

import pl.edu.mimuw.nesc.exception.LexerException;
import pl.edu.mimuw.nesc.parser.Symbol;
import pl.edu.mimuw.nesc.preprocessor.PreprocessorMacro;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Lexer interface. Provides stream of tokens. The preprocessing phase is being
 * done simultaneously with lexical analysis. This is inevitable since we have
 * to know exactly in which part of input file each macro is (un)defined. The
 * second reason is to be able to retrieve the exact location of token in input
 * file before preprocessing phase.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public interface Lexer {

    /**
     * Notify lexer that all preparation are done (e.g. listeners, macros
     * are set).
     */
    void start();

    /**
     * Sets listener for lexer callbacks.
     *
     * @param listener listener
     */
    void setListener(LexerListener listener);

    /**
     * Removes listener.
     *
     */
    void removeListener();

    /**
     * Returns next token from preprocessed source file. Each token contains
     * token's location in original input file.
     *
     * @return next token
     * @throws LexerException
     */
    Symbol nextToken() throws LexerException;

    /**
     * Adds macro definition.
     *
     * @param macro macro
     * @throws LexerException
     */
    void addMacro(PreprocessorMacro macro) throws LexerException;

    /**
     * Adds macros definitions.
     *
     * @param macros macros
     * @throws LexerException
     */
    void addMacros(Collection<PreprocessorMacro> macros) throws LexerException;

    /**
     * Returns map of macros recognized so far.
     *
     * @return map of macros
     */
    Map<String, PreprocessorMacro> getMacros();

    /**
     * Returns <code>true</code>, whenever lexer is processing the outermost
     * file of the translation unit.
     *
     * @return
     */
    boolean isOnTopContext();

    /**
     * Cancels lexical analysis.
     *
     * @throws IOException
     */
    void cancel() throws IOException;

    /**
     * Closes lexer.
     *
     * @throws IOException
     */
    void close() throws IOException;

}
