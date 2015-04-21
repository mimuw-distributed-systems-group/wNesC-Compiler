package pl.edu.mimuw.nesc.backend8051;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Level;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.ast.gen.AttrTransformer;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.astutil.DeclarationsSeparator;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;
import pl.edu.mimuw.nesc.astwriting.WriteSettings;
import pl.edu.mimuw.nesc.backend8051.option.Options8051Holder;
import pl.edu.mimuw.nesc.backend8051.option.Options8051Parser;
import pl.edu.mimuw.nesc.codepartition.PartitionImpossibleException;
import pl.edu.mimuw.nesc.codepartition.SimpleCodePartitioner;
import pl.edu.mimuw.nesc.codesize.SDCCCodeSizeEstimator;
import pl.edu.mimuw.nesc.common.util.VariousUtils;
import pl.edu.mimuw.nesc.common.util.file.FileUtils;
import pl.edu.mimuw.nesc.compilation.CompilationExecutor;
import pl.edu.mimuw.nesc.compilation.CompilationResult;
import pl.edu.mimuw.nesc.compilation.DefaultCompilationListener;
import pl.edu.mimuw.nesc.compilation.ErroneousIssueException;
import pl.edu.mimuw.nesc.exception.InvalidOptionsException;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;
import pl.edu.mimuw.nesc.option.OptionsProvider;
import pl.edu.mimuw.nesc.refsgraph.ReferencesGraph;

/**
 * <p>Compilation for 8051 microcontrollers.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class Main {
    /**
     * Set of no-parameter target attributes recognized by the 8051 version of
     * the compiler. It is the set of keywords used to specify storage-class
     * extensions and "__reentrant" and "__banked" keywords.
     */
    private static final ImmutableSet<String> TARGET_ATTRIBUTES0;
    static {
        final ImmutableSet.Builder<String> targetAttributesBuilder = ImmutableSet.builder();
        targetAttributesBuilder.addAll(StorageClassExtension.getAllKeywords());
        targetAttributesBuilder.add("__reentrant", "__banked");
        TARGET_ATTRIBUTES0 = targetAttributesBuilder.build();
    }

    /**
     * Set with one-argument target attributes recognized by the 8051 version of
     * the compiler. They are the SDCC attributes: "__at", "__interrupt",
     * "__using".
     */
    private static final ImmutableSet<String> TARGET_ATTRIBUTES1 = ImmutableSet.of(
        "__at",
        "__interrupt",
        "__using"
    );

    /**
     * Code returned by the compiler to the system when the compilation fails.
     */
    private static final int STATUS_ERROR = 1;

    /**
     * Code returned by the compiler to the system when the compilation
     * succeeds.
     */
    private static final int STATUS_SUCCESS = 0;

    /**
     * Options for the 8051 version of the compiler.
     */
    private final Options8051Holder options;

    /**
     * Object that provides options for the frontend.
     */
    private final OptionsProvider frontendOptions;

    /**
     * Settings used for writing files with programs.
     */
    private final WriteSettings writeSettings;

    /**
     * Parse and validate parameters for the 8051 version of the compiler. If
     * the parsing process fails or the options don't validate, then compilation
     * is terminated.
     *
     * @param args The parameters to parse.
     * @return Parser that has parsed the given parameters and allows retrieving
     *         information about them.
     * @throws IOException Cannot read frontend parameters.
     */
    private static Options8051Parser parseParameters(String[] args) throws IOException {
        final Options8051Parser optionsParser = new Options8051Parser(args);

        // Parse options
        try {
            optionsParser.parse();
        } catch (ParseException e) {
            if (args.length != 0) {
                optionsParser.printError(e.getMessage());
            } else {
                optionsParser.printHelpWithError(e.getMessage());
            }
            System.exit(STATUS_ERROR);
        }

        // Validate options
        final Optional<String> error = optionsParser.getValidator().validate();
        if (error.isPresent()) {
            optionsParser.printError(error.get());
            System.exit(STATUS_ERROR);
        }

        return optionsParser;
    }

    public static void main(String[] args) {
        try {
            VariousUtils.setLoggingLevel(Level.OFF);
            final Options8051Parser parser = parseParameters(args);
            new Main(parser.getOptions8051(), parser).compile();
            System.exit(STATUS_SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(STATUS_ERROR);
        }
    }

    private Main(Options8051Holder options, OptionsProvider frontendOptions) {
        this.options = options;
        this.frontendOptions = frontendOptions;
        this.writeSettings = WriteSettings.builder()
                .charset("UTF-8")
                .indentWithSpaces(3)
                .nameMode(WriteSettings.NameMode.USE_UNIQUE_NAMES)
                .uniqueMode(WriteSettings.UniqueMode.OUTPUT_VALUES)
                .build();
    }

    /**
     * Performs the whole compilation process for 8051 microcontrollers. This
     * method shall be called exactly once.
     */
    private void compile() throws InvalidOptionsException {
        try {
            checkSDCC();
            final CompilationExecutor executor = new CompilationExecutor(
                    TARGET_ATTRIBUTES0, TARGET_ATTRIBUTES1);
            executor.setListener(new DefaultCompilationListener());
            final CompilationResult result = executor.compile(frontendOptions);
            final ImmutableList<Declaration> separatedDecls =
                    separateDeclarations(result.getDeclarations(), result.getNameMangler());
            dumpCallGraph(result.getReferencesGraph());
            reduceAttributes(separatedDecls);
            adjustSpecifiers(separatedDecls);
            assignInterrupts(separatedDecls, options.getInterrupts(), result.getABI());
            final ImmutableMap<String, Range<Integer>> funsSizesEstimation =
                    estimateFunctionsSizes(separatedDecls);
            final ImmutableList<ImmutableSet<FunctionDecl>> funsPartition =
                    partitionFunctions(separatedDecls, funsSizesEstimation);
            final DeclarationsPartitioner.Partition declsPartition =
                    partitionDeclarations(separatedDecls, funsPartition, result.getReferencesGraph());
            writeDeclarations(declsPartition, result.getOutputFileName());
        } catch (ErroneousIssueException e) {
            System.exit(STATUS_ERROR);
        } catch (InterruptedException e) {
            System.err.println("interrupted");
            System.exit(STATUS_ERROR);
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
            System.exit(STATUS_ERROR);
        } catch (PartitionImpossibleException e) {
            System.err.println("error: cannot partition functions into the code banks - "
                    + e.getMessage());
            System.exit(STATUS_ERROR);
        }
    }

    /**
     * Separates declarations for convenient manipulation of attributes,
     * storage-class specifiers and type elements. Types of declarations that
     * are separated: top-level declarations, declarations in compound
     * statements. Declarations of fields in tag definitions are preserved in
     * their original state.
     *
     * @param declarations Declarations to separate.
     * @param nameMangler Name mangler to use in the separation.
     * @return List that contains the given declarations separated.
     */
    private ImmutableList<Declaration> separateDeclarations(ImmutableList<Declaration> declarations,
                NameMangler nameMangler) {
        final DeclarationsSeparator separator = new DeclarationsSeparator(nameMangler);
        separator.configure(true, false);
        return separator.separate(declarations);
    }

    /**
     * Saves the call graph to file if the user needs it.
     *
     * @param refsGraph References graph with the call graph to dump.
     */
    private void dumpCallGraph(ReferencesGraph refsGraph) {
        if (!options.getCallGraphFile().isPresent()) {
            return;
        }

        try {
            refsGraph.writeCallGraph(options.getCallGraphFile().get());
        } catch (IOException e) {
            System.err.println("warning: cannot write the call graph to file '"
                    + options.getCallGraphFile().get() + "' (" + e.getMessage()
                    + ")");
        }
    }

    /**
     * Remove or transform all GCC and NesC attributes as SDCC does not accept
     * them.
     *
     * @param declarations Declarations of the whole program.
     */
    private void reduceAttributes(ImmutableList<Declaration> declarations) {
        final AttrTransformer<Void> reduceTransformer = new AttrTransformer<>(
                new ReduceAttrTransformation(new DefaultCompilationListener()));
        for (Declaration declaration : declarations) {
            declaration.traverse(reduceTransformer, null);
        }
    }

    /**
     * Adjust specifiers in declarations related to SDCC storage-class
     * extensions.
     *
     * @param declarations List with declarations to be adjusted.
     */
    private void adjustSpecifiers(ImmutableList<Declaration> declarations) {
        new DeclarationsAdjuster().adjust(declarations);
    }

    /**
     * Check the SDCC that will be used for the compilation. Warnings are
     * emitted if issues are detected.
     */
    private void checkSDCC() {
        final String sdccExecutablePath = options.getSDCCExecutable().or("sdcc");
        final Iterable<String> issues = new SDCCChecker().check(sdccExecutablePath);

        for (String issue : issues) {
            System.err.println("warning: " + issue);
        }
    }

    /**
     * Add '__interrupt' attributes where they are missing according to
     * '--interrupts' option of the compiler.
     *
     * @param declarations Declarations that will be modified.
     * @param interrupts Multimap from unique names of functions to numbers of
     *                   interrupts handled by them.
     * @param abi ABI of the project.
     */
    private void assignInterrupts(ImmutableList<Declaration> declarations,
                SetMultimap<String, Integer> interrupts, ABI abi) {
        new InterruptHandlerAssigner(interrupts, abi).assign(declarations);
    }

    /**
     * Estimate sizes of functions whose definitions are on the given list using
     * the SDCC code size estimator.
     *
     * @param declarations List of declarations. Sizes of defined functions from
     *                     the list will be estimated.
     * @return Map with the result of estimation.
     */
    private ImmutableMap<String, Range<Integer>> estimateFunctionsSizes(
                ImmutableList<Declaration> declarations) throws InterruptedException, IOException {

        final SDCCCodeSizeEstimator.Builder estimatorBuilder =
                SDCCCodeSizeEstimator.builder(declarations, writeSettings);

        // Memory model and SDCC executable
        estimatorBuilder.memoryModel(options.getMemoryModel().orNull())
                .sdccExecutable(options.getSDCCExecutable().orNull())
                .addSDCCParameter("--std-c99");

        // Threads count
        final Optional<Integer> threadsCount = options.getEstimateThreadsCount();
        if (threadsCount.isPresent()) {
            estimatorBuilder.threadsCount(threadsCount.get());
        }

        return estimatorBuilder.build().estimate();
    }

    /**
     * Run heuristics that partition functions into banks.
     *
     * @param declarations Declarations that constitute the whole program.
     * @param estimation Estimation of functions sizes.
     * @return Partition of functions into the banks.
     * @throws PartitionImpossibleException It is impossible to assign functions
     *                                      to banks.
     */
    private ImmutableList<ImmutableSet<FunctionDecl>> partitionFunctions(
                ImmutableList<Declaration> declarations,
                ImmutableMap<String, Range<Integer>> estimation
    ) throws PartitionImpossibleException {
        final List<FunctionDecl> functions = FluentIterable.from(declarations)
                .filter(FunctionDecl.class)
                .toList();
        return new SimpleCodePartitioner(options.getBankSize().or(32768),
                        options.getBanksCount().or(8))
                .partition(functions, estimation);
    }

    /**
     * Partition declarations after assignment of functions to banks to files.
     *
     * @param allDeclarations All declarations that constitute the program in
     *                        proper order (they shall be separated).
     * @param partition Partition of functions to banks.
     * @param refsGraph Graph of references between entities in the program.
     * @return Partition of declarations to files.
     */
    private DeclarationsPartitioner.Partition partitionDeclarations(
            ImmutableList<Declaration> allDeclarations,
            ImmutableList<ImmutableSet<FunctionDecl>> partition,
            ReferencesGraph refsGraph
    ) {
        return new DeclarationsPartitioner(allDeclarations, partition, refsGraph)
                .partition();
    }

    /**
     * Write all declarations into proper files.
     *
     * @param declsPartition Partition of all declarations of the program.
     * @param outputFile Name of the output file specified by options (or the
     *                   default one).
     */
    private void writeDeclarations(DeclarationsPartitioner.Partition declsPartition,
            String outputFile) throws IOException {
        final String baseName = FileUtils.getFileNameWithoutExtension(outputFile);
        final String headerName = baseName + ".h";

        // Write the header file
        try (final ASTWriter headerWriter = new ASTWriter(headerName, writeSettings)) {
            headerWriter.write(declsPartition.getHeaderFile());
        }

        // Write declarations inside banks
        final Iterator<ImmutableList<Declaration>> banksIt =
                declsPartition.getCodeFiles().iterator();
        if (banksIt.hasNext()) {
            // Common area
            try (final ASTWriter commonBankWriter = new ASTWriter(outputFile, writeSettings)) {
                commonBankWriter.write("#include \"" + headerName + "\"\n");
                commonBankWriter.write("#pragma codeseg HOME\n\n");
                commonBankWriter.write(banksIt.next());
            }

            // Other banks
            int bankNumber = 1;
            while (banksIt.hasNext()) {
                final String fileName = baseName + "-" + bankNumber + ".c";
                try (final ASTWriter bankFileWriter = new ASTWriter(fileName, writeSettings)) {
                    bankFileWriter.write("#include \"" + headerName + "\"\n");
                    bankFileWriter.write("#pragma codeseg BANK" + bankNumber + "\n\n");
                    bankFileWriter.write(banksIt.next());
                }
                ++bankNumber;
            }
        }
    }
}
