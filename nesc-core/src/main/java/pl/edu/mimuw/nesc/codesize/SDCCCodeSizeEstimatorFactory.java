package pl.edu.mimuw.nesc.codesize;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import java.io.File;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.ExceptionVisitor;
import pl.edu.mimuw.nesc.ast.gen.ExtensionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.astwriting.WriteSettings;
import pl.edu.mimuw.nesc.refsgraph.ReferencesGraph;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Factory for creating SDCC code size estimators.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class SDCCCodeSizeEstimatorFactory {
    /**
     * Default values of options for the estimator.
     */
    private static final String DEFAULT_SDCC_EXEC = "sdcc";
    private static final String DEFAULT_SDAS_EXEC = "sdas8051";
    private static final String DEFAULT_TMP_DIR = System.getProperty("java.io.tmpdir");
    private static final int DEFAULT_THREADS_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int DEFAULT_MAXIMUM_INLINE_SIZE = 20;

    /**
     * Data necessary for an SDCC code size estimator.
     */
    private final ImmutableList.Builder<String> sdccParametersBuilder;
    private ImmutableList<Declaration> declarations;
    private WriteSettings writeSettings;
    private Optional<SDCCMemoryModel> memoryModel;
    private Optional<String> sdccExecutablePath;
    private Optional<String> temporaryDirectory;

    public SDCCCodeSizeEstimatorFactory(ImmutableList<Declaration> declarations,
            WriteSettings writeSettings) {
        checkNotNull(declarations, "declarations cannot be null");
        checkNotNull(writeSettings, "write settings cannot be null");

        this.sdccParametersBuilder = ImmutableList.builder();
        this.declarations = declarations;
        this.writeSettings = writeSettings;
        this.memoryModel = Optional.absent();
        this.sdccExecutablePath = Optional.absent();
        this.temporaryDirectory = Optional.absent();
    }

    /**
     * Set the memory model used for estimating the code size. The argument
     * can be <code>null</code>. In such case the default model implied by
     * SDCC will be used.
     *
     * @param memoryModel Memory model to use for estimation.
     * @return <code>this</code>
     */
    public SDCCCodeSizeEstimatorFactory setMemoryModel(SDCCMemoryModel memoryModel) {
        this.memoryModel = Optional.fromNullable(memoryModel);
        return this;
    }

    /**
     * Set path to SDCC executable that will be used to invoke it. The
     * argument can be <code>null</code>. In such case <code>sdcc</code>
     * command without any path will be used to invoke SDCC.
     *
     * @param executablePath Path to the SDCC executable.
     * @return <code>this</code>
     */
    public SDCCCodeSizeEstimatorFactory setSDCCExecutable(String executablePath) {
        this.sdccExecutablePath = Optional.fromNullable(executablePath);
        return this;
    }

    /**
     * Add a parameter to pass to SDCC when invoking it. The added
     * parameters will appear in the order of adding them.
     *
     * @param parameter Parameter to add.
     * @return <code>this</code>
     */
    public SDCCCodeSizeEstimatorFactory addSDCCParameter(String parameter) {
        this.sdccParametersBuilder.add(parameter);
        return this;
    }

    /**
     * Add parameters to pass to SDCC when invoking it. They will be added
     * and passed to SDCC in the order returned by the iterator of the given
     * iterable.
     *
     * @param parameters Iterable with parameters to add.
     * @return <code>this</code>
     */
    public SDCCCodeSizeEstimatorFactory addSDCCParameters(Iterable<String> parameters) {
        this.sdccParametersBuilder.addAll(parameters);
        return this;
    }

    /**
     * Set the directory used for storing files output by SDCC for
     * estimation of code size. The argument can be <code>null</code>.
     * In such case the default temporary directory will be used that
     * is provided by Java by using property "java.io.tmpdir".
     *
     * @param tmpDir Temporary directory to use.
     * @return <code>this</code>
     */
    public SDCCCodeSizeEstimatorFactory setTemporaryDirectory(String tmpDir) {
        this.temporaryDirectory = Optional.fromNullable(tmpDir);
        return this;
    }

    /**
     * Set the settings for writing declarations used by the created SDCC code
     * size estimator.
     *
     * @param settings New write settings to use.
     * @return <code>this</code>
     */
    public SDCCCodeSizeEstimatorFactory setWriteSettings(WriteSettings settings) {
        checkNotNull(settings, "settings cannot be null");
        this.writeSettings = settings;
        return this;
    }

    /**
     * Set the declarations with functions whose sizes will be estimated.
     *
     * @param declarations Declarations to set.
     * @return <code>this</code>
     */
    public SDCCCodeSizeEstimatorFactory setDeclarations(ImmutableList<Declaration> declarations) {
        checkNotNull(declarations, "declarations cannot be null");
        this.declarations = declarations;
        return this;
    }

    /**
     * Create a new unitary SDCC code size estimator that will use the default
     * count of threads. The default value is the count of cores in the system
     * as obtained by calling {@link java.lang.Runtime#availableProcessors}.
     *
     * @return Newly created unitary SDCC code size estimator.
     */
    public CodeSizeEstimator newUnitaryEstimator() {
        return newUnitaryEstimator(DEFAULT_THREADS_COUNT);
    }

    /**
     * Create a new unitary SDCC code size estimator that will use the given
     * number of threads for estimating the code size. The argument must be
     * positive.
     *
     * @param threadsCount Count of threads used for estimation.
     * @return Newly created instance of unitary SDCC code size estimator.
     * @throws IllegalArgumentException Count of threads is not positive.
     */
    public CodeSizeEstimator newUnitaryEstimator(int threadsCount) {
        checkArgument(threadsCount > 0, "threads count must be positive");
        final ImmutableList<String> sdccParameters = sdccParametersBuilder.build();
        validate(sdccParameters);
        return new UnitarySDCCCodeSizeEstimator(declarations, extractFunctions(),
                sdccExecutablePath.or(DEFAULT_SDCC_EXEC), sdccParameters,
                memoryModel, temporaryDirectory.or(DEFAULT_TMP_DIR), threadsCount,
                writeSettings);
    }

    /**
     * Create a new fast SDCC code size estimator according to the configuration
     * of the factory.
     *
     * @return Newly created fast SDCC code size estimator.
     */
    public CodeSizeEstimator newFastEstimator() {
        final ImmutableList<String> sdccParameters = sdccParametersBuilder.build();
        validate(sdccParameters);
        return new FastSDCCCodeSizeEstimator(declarations, extractFunctions(),
                sdccExecutablePath.or(DEFAULT_SDCC_EXEC), sdccParameters,
                memoryModel, temporaryDirectory.or(DEFAULT_TMP_DIR),
                writeSettings);
    }

    /**
     * Create a new inlining SDCC code size estimator according to configuration
     * of the factory and parameters given to this method.
     *
     * @param threadsCount Count of threads created and used by the estimator.
     * @param sdasExecutablePath Path to the executable of the assembler
     *                           included with SDCC to use.
     * @param refsGraph Graph of references between global entities in the
     *                  program.
     * @param maximumInlineFunSize Maximum size of a function to make it inline.
     * @param isInlineRelaxed Value indicating if functions that are already
     *                        marked as inline may became not inline if they
     *                        don't qualify for an inline function.
     * @return Newly created inlining SDCC code size estimator.
     */
    public CodeSizeEstimator newInliningEstimator(Optional<Integer> threadsCount,
            Optional<String> sdasExecutablePath, ReferencesGraph refsGraph,
            Optional<Integer> maximumInlineFunSize, boolean isInlineRelaxed) {
        checkNotNull(sdasExecutablePath, "sdas executable path cannot be null");
        checkNotNull(refsGraph, "references graph cannot be null");
        checkArgument(!threadsCount.isPresent() || threadsCount.get() > 0,
                "threads count must be positive");
        checkArgument(!maximumInlineFunSize.isPresent() || maximumInlineFunSize.get() >= 0,
                "maximum inline function size cannot be negative");

        final ImmutableList<String> sdccParameters = sdccParametersBuilder.build();
        validate(sdccParameters);
        return new InliningSDCCCodeSizeEstimator(declarations, extractFunctions(),
                refsGraph, sdccExecutablePath.or(DEFAULT_SDCC_EXEC), sdccParameters,
                sdasExecutablePath.or(DEFAULT_SDAS_EXEC), memoryModel,
                temporaryDirectory.or(DEFAULT_TMP_DIR), threadsCount.or(DEFAULT_THREADS_COUNT),
                writeSettings, isInlineRelaxed, maximumInlineFunSize.or(DEFAULT_MAXIMUM_INLINE_SIZE));
    }

    private void validate(ImmutableList<String> sdccParameters) {
        // Check if the temporary directory exists and is writable
        if (temporaryDirectory.isPresent()) {
            final File tmpDir = new File(temporaryDirectory.get());
            checkState(tmpDir.exists(), "directory '" + temporaryDirectory.get()
                    + "' does not exist");
            checkState(tmpDir.isDirectory(), "'" + temporaryDirectory.get()
                    + "' is not a directory");
            checkState(tmpDir.canWrite(), "lack of write permission for '"
                    + temporaryDirectory.get() + "'");
        }

        // Check if SDCC parameters are correct
        for (String parameter : sdccParameters) {
            checkState(parameter != null, "a null SDCC parameter is added");
            checkState(!parameter.isEmpty(), "an empty SDCC parameter is added");
            checkState(!SDCCMemoryModel.getAllOptions().contains(parameter),
                    "parameter that specifies a memory model is added");
            checkState(!parameter.equals("-c"), "'-c' parameter is added");
            checkState(!parameter.equals("--compile-only"), "'--compile-only' parameter is added");
            checkState(!parameter.equals("-S"), "'-S' parameter is added");
        }
    }

    private ImmutableList<FunctionDecl> extractFunctions() {
        final FunctionsCollectingVisitor collectingVisitor = new FunctionsCollectingVisitor();
        for (Declaration declaration : declarations) {
            declaration.accept(collectingVisitor, null);
        }
        return collectingVisitor.functionsBuilder.build();
    }

    /**
     * Visitor responsible for collecting definitions of functions.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class FunctionsCollectingVisitor extends ExceptionVisitor<Void, Void> {
        private final ImmutableList.Builder<FunctionDecl> functionsBuilder = ImmutableList.builder();

        @Override
        public Void visitFunctionDecl(FunctionDecl declaration, Void arg) {
            functionsBuilder.add(declaration);
            return null;
        }

        @Override
        public Void visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            declaration.getDeclaration().accept(this, null);
            return null;
        }

        @Override
        public Void visitDataDecl(DataDecl declaration, Void arg) {
            return null;
        }
    }
}
