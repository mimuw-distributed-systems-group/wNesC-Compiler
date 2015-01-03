package pl.edu.mimuw.nesc.intermediate;

import com.google.common.collect.Multimap;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;

/**
 * Interface with operations of an intermediate functions generator.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public interface IntermediateGenerator {
    /**
     * Perform the generation of all necessary intermediate functions. All
     * information necessary for the process shall be given at the construction
     * of the generator.
     *
     * @return Multimap with names of specification elements and definitions of
     *         intermediate functions associated with them.
     */
    Multimap<String, FunctionDecl> generate();
}
