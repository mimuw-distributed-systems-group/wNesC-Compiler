package pl.edu.mimuw.nesc.preprocessor;

import pl.edu.mimuw.nesc.lexer.Lexer;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>
 * Keeps track of <i>public</i> macro definitions.
 * </p>
 * <p>
 * When parser finishes parsing of all declarations and definitions located in
 * current file right before the nesc entity definition, the macros manager
 * saves all defined macros. The saved set of <i>public</i> macros will be the
 * input for the next files in the parse order. The remaining macros which will
 * appear in current file will be regarded as <i>private</i> and will not be
 * visible for the following files.
 * </p>
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class MacrosManager {

    private final Map<String, PreprocessorMacro> macros;
    private final Map<String, String> unparsedMacros;

    /**
     * Creates macros manager.
     */
    public MacrosManager() {
        this.macros = new HashMap<>();
        this.unparsedMacros = new HashMap<>();
    }

    /**
     * Adds a (predefined) macro.
     *
     * @param macrosMap map of macros
     */
    public void addUnparsedMacros(Map<String, String> macrosMap) {
        checkNotNull(macrosMap, "macros list should not be null");
        this.unparsedMacros.putAll(macrosMap);
    }

    /**
     * Retrieves and removes all unparsed macros.
     *
     * @return map of unparsed macros
     */
    public Map<String, String> removeUnparsedMacros() {
        final Map<String, String> result = new HashMap<>(this.unparsedMacros);
        this.unparsedMacros.clear();
        return result;
    }

    /**
     * Returns public macros.
     *
     * @return map of public macros recognized (and not undefined) so far
     */
    public Map<String, PreprocessorMacro> getMacros() {
        return this.macros;
    }

    /**
     * Saves public macros.
     *
     * @param lexer lexer
     */
    public void saveMacros(final Lexer lexer) {
        Map<String, PreprocessorMacro> lexerMacros = lexer.getMacros();
        this.macros.clear();
        for (Map.Entry<String, PreprocessorMacro> entry : lexerMacros.entrySet()) {
            final String macroName = entry.getKey();
            final PreprocessorMacro macro = entry.getValue();
            this.macros.put(macroName, macro);
        }
    }

}
