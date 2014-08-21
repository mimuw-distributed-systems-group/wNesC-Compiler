package pl.edu.mimuw.nesc.load;

import com.google.common.collect.ImmutableMap;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.preprocessor.PreprocessorMacro;

import java.util.*;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class MacroManager {

    private static final Logger LOG = Logger.getLogger(MacroManager.class);

    /*
     * The order in which the macros are stored is not important, since
     * the object representing macros are already "final" objects.
     * When the lexer is fed with those macros, it does not need to
     * parse them again.
     */
    private Map<String, PreprocessorMacro> allMacros;

    public MacroManager() {
        this.allMacros = new HashMap<>();
    }

    public void replace(Map<String, PreprocessorMacro> macros) {
        this.allMacros = ImmutableMap.copyOf(macros);

    }

    public Map<String, PreprocessorMacro> getAll() {
        return allMacros;
    }
}
