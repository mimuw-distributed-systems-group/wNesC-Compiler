package pl.edu.mimuw.nesc.compilation;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;
import pl.edu.mimuw.nesc.refsgraph.ReferencesGraph;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class with results of the compilation of a NesC program to a list of
 * equivalent C declarations and definitions.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class CompilationResult {
    /**
     * The resulting C declarations.
     */
    private final ImmutableList<Declaration> declarations;

    /**
     * Name mangler used to for mangling names of entities in the list of
     * declarations.
     */
    private final NameMangler nameMangler;

    /**
     * Graph of references between entities.
     */
    private final ReferencesGraph refsGraph;

    /**
     * Name of the output file that is expected to contain the declarations.
     */
    private final String outputFileName;

    /**
     * Multimap that contains information about the external variables that are
     * defined by the user using options to the frontend.
     */
    private final SetMultimap<Optional<String>, String> externalVariables;

    /**
     * Name of the file that is expected to contain unique names of external
     * variables.
     */
    private final Optional<String> externalVariablesFileName;

    /**
     * ABI for the project.
     */
    private final ABI abi;

    CompilationResult(
            ImmutableList<Declaration> declarations,
            NameMangler nameMangler,
            ReferencesGraph refsGraph,
            String outputFileName,
            SetMultimap<Optional<String>, String> externalVariables,
            Optional<String> externalVariablesFileName,
            ABI abi
    ) {
        checkNotNull(declarations, "declarations cannot be null");
        checkNotNull(nameMangler, "name mangler cannot be null");
        checkNotNull(refsGraph, "references graph cannot be null");
        checkNotNull(outputFileName, "output file name cannot be null");
        checkNotNull(externalVariables, "external variables cannot be null");
        checkNotNull(externalVariablesFileName, "name of the file with external variables cannot be null");
        checkArgument(!outputFileName.isEmpty(), "output file name cannot be an empty string");
        checkArgument(!externalVariablesFileName.isPresent() || !externalVariablesFileName.get().isEmpty(),
                "external variables file name cannot be an empty string");
        checkNotNull(abi, "ABI cannot be null");

        this.declarations = declarations;
        this.nameMangler = nameMangler;
        this.refsGraph = refsGraph;
        this.outputFileName = outputFileName;
        this.externalVariables = externalVariables;
        this.externalVariablesFileName = externalVariablesFileName;
        this.abi = abi;
    }

    /**
     * Get the C declarations that are the result of compilation.
     *
     * @return List with C declarations and definitions that are the result of
     *         the compilation.
     */
    public ImmutableList<Declaration> getDeclarations() {
        return declarations;
    }

    /**
     * Get the name mangler that has been used to mangle names in the program and
     * that can be used to generate unique names.
     *
     * @return The name mangler of the project.
     */
    public NameMangler getNameMangler() {
        return nameMangler;
    }

    /**
     * Get the graph of references between entities in the program.
     *
     * @return The references graph.
     */
    public ReferencesGraph getReferencesGraph() {
        return refsGraph;
    }

    /**
     * Get the name of the output file name.
     *
     * @return Name of the output file that is expected to contain the result of
     *         compilation.
     */
    public String getOutputFileName() {
        return outputFileName;
    }

    /**
     * Get the multimap with information about external variables that are
     * defined by the user using the options to the frontend. The multimap is
     * unmodifiable and the entries are returned in the same order they have
     * been specified in the option for the frontend.
     *
     * @return Multimap that defines the external variables.
     */
    public SetMultimap<Optional<String>, String> getExternalVariables() {
        return externalVariables;
    }

    /**
     * Get the name of the file that is to be created and contain names of
     * external variables.
     *
     * @return Name of the file with unique names of external variables to
     *         create. If it is absent, then such file is not expected to be
     *         created.
     */
    public Optional<String> getExternalVariablesFileName() {
        return externalVariablesFileName;
    }

    /**
     * Get ABI of the target platform of the project.
     *
     * @return ABI of the project.
     */
    public ABI getABI() {
        return abi;
    }
}
