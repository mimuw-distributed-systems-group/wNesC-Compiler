package pl.edu.mimuw.nesc.lexer;

import pl.edu.mimuw.nesc.exception.LexerException;
import pl.edu.mimuw.nesc.parser.Symbol;
import pl.edu.mimuw.nesc.preprocessor.PreprocessorMacro;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Lexer interface. Provides a stream of tokens.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public interface Lexer {

    /**
     * Notify the lexer that all preparation are done (e.g. listeners, macros
     * are set).
     */
    void start();

    /**
     * Sets a listener for lexer callbacks.
     *
     * @param listener listener
     */
    void setListener(LexerListener listener);

    /**
     * Removes the listener.
     */
    void removeListener();

    /**
     * Returns the next token from the preprocessed source file. Each token contains
     * token's location in the original input file.
     *
     * @return next token
     * @throws LexerException
     */
    Symbol nextToken() throws LexerException;

    /**
     * Adds a macro definition.
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
     * Returns a map of macros recognized so far.
     *
     * @return map of macros
     */
    Map<String, PreprocessorMacro> getMacros();

    /**
     * Removes all known macros except predefined ones.
     */
    void removeMacros();

    /**
     * Sets the current macros that are known to the given macros.
     */
    void replaceMacros(Collection<PreprocessorMacro> macros) throws LexerException;

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
