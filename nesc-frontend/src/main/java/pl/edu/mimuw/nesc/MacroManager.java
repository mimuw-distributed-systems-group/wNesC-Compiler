package pl.edu.mimuw.nesc;

import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.preprocessor.PreprocessorMacro;

import java.util.*;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class MacroManager {

    private static final Logger LOG = Logger.getLogger(MacroManager.class);

    private static final int EXPECTED_NUMBER_OF_MACROS = 2000;

    /*
     * The order in which the macros are stored is not important, since
     * the object representing macros are already "final" objects.
     * When the lexer is fed with those macros, it does not need to
     * parse them again.
     */
    private final Set<PreprocessorMacro> allMacros;

    public MacroManager() {
        this.allMacros = new HashSet<>(EXPECTED_NUMBER_OF_MACROS);
    }

    public void clear() {
        LOG.trace("clear macros");
        this.allMacros.clear();

    }

    public void addMacro(PreprocessorMacro macro) {
        this.allMacros.add(macro);
    }

    public Collection<PreprocessorMacro> getAll() {
        return allMacros;
    }
}
