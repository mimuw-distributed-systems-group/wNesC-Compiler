package pl.edu.mimuw.nesc.codesize;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.ExceptionVisitor;
import pl.edu.mimuw.nesc.ast.gen.ExtensionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRefDeclarator;
import pl.edu.mimuw.nesc.ast.gen.NestedDeclarator;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.astutil.AstUtils;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.astutil.TypeElementUtils;
import pl.edu.mimuw.nesc.astutil.TypeElementsAdjuster;
import pl.edu.mimuw.nesc.astutil.TypeElementsPreserver;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;
import pl.edu.mimuw.nesc.astwriting.CustomDeclarationsWriter;
import pl.edu.mimuw.nesc.astwriting.WriteSettings;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.external.ExternalConstants;
import pl.edu.mimuw.nesc.refsgraph.EntityNode;
import pl.edu.mimuw.nesc.refsgraph.Reference;
import pl.edu.mimuw.nesc.refsgraph.ReferencesGraph;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Code size estimator that estimates sizes of functions determining
 * functions that are to be inlined.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class InliningSDCCCodeSizeEstimator implements CodeSizeEstimator {
    /**
     * Name of the header file that will contain non-banked declarations.
     */
    private static final String NAME_NONBANKED_HEADER = "nonbanked_decls.h";

    /**
     * Name of the header file that will contain banked declarations.
     */
    private static final String NAME_BANKED_HEADER = "banked_decls.h";

    /**
     * Name of the code segment used for functions whose sizes are estimated.
     */
    private static final String NAME_CODE_SEGMENT = "CODESEG";

    /**
     * Memory model that is used for estimating sizes of functions. If the
     * object is absent, then the default model implied by SDCC is used.
     */
    private final Optional<SDCCMemoryModel> memoryModel;

    /**
     * Path to SDCC executable that will be invoked for the estimation.
     */
    private final String sdccExecutablePath;

    /**
     * Parameters that will be passed to SDCC.
     */
    private final ImmutableList<String> sdccParameters;

    /**
     * Path to the assembler that will be used to create .REL files.
     */
    private final String sdasExecutablePath;

    /**
     * Directory used for saving files necessary for the estimation.
     */
    private final String tempDirectory;

    /**
     * Count of threads used for the estimation process.
     */
    private final int threadsCount;

    /**
     * Settings for writing AST nodes for the estimation.
     */
    private final WriteSettings writeSettings;

    /**
     * Value indicating if an inline function can become not inline.
     */
    private final boolean isInlineRelaxed;

    /**
     * Maximum size of an inline function.
     */
    private final int maximumInlineFunSize;

    /**
     * List with all declarations that constitute a NesC program.
     */
    private final ImmutableList<Declaration> allDeclarations;

    /**
     * Graph of references between entities.
     */
    private final ReferencesGraph refsGraph;

    /**
     * Definitions of functions that are currently inline.
     */
    private ImmutableList<FunctionDecl> inlineFunctions;

    /**
     * Definitions of functions that are not inline currently.
     */
    private ImmutableList<FunctionDecl> normalFunctions;

    /**
     * Set with unique names of inline functions.
     */
    private Set<String> inlineFunctionsNames;

    /**
     * Object responsible for maintaining and preserving original state of
     * declarations.
     */
    private final TypeElementsPreserver astStatePreserver;

    /**
     * Threads that perform the estimation operation.
     */
    private ImmutableList<Thread> threads;

    /**
     * Queue that contains requests to the threads.
     */
    private final BlockingQueue<Request> requestsQueue;

    /**
     * Queue for responses coming from threads.
     */
    private final BlockingQueue<Response> responsesQueue;
    /**
     * Result of the estimation operation.
     */
    private Optional<CodeSizeEstimation> estimation;

    InliningSDCCCodeSizeEstimator(
            ImmutableList<Declaration> declarations,
            ImmutableList<FunctionDecl> functions,
            ReferencesGraph refsGraph,
            String sdccExecutablePath,
            ImmutableList<String> sdccParameters,
            String sdasExecutablePath,
            Optional<SDCCMemoryModel> memoryModel,
            String tempDirectory,
            int threadsCount,
            WriteSettings writeSettings,
            boolean isInlineRelaxed,
            int maximumInlineFunSize
    ) {
        checkNotNull(declarations, "declarations cannot be null");
        checkNotNull(functions, "functions cannot be null");
        checkNotNull(refsGraph, "references graph cannot be null");
        checkNotNull(sdccExecutablePath, "SDCC executable path cannot be null");
        checkNotNull(sdccParameters, "SDCC parameters cannot be null");
        checkNotNull(sdasExecutablePath, "SDAS executable path cannot be null");
        checkNotNull(memoryModel, "memory model cannot be null");
        checkNotNull(tempDirectory, "temporary directory cannot be null");
        checkNotNull(writeSettings, "write settings cannot be null");
        checkArgument(!sdccExecutablePath.isEmpty(), "SDCC executable path cannot be an empty string");
        checkArgument(!sdasExecutablePath.isEmpty(), "SDAS executable path cannot be an empty string");
        checkArgument(!tempDirectory.isEmpty(), "temporary directory cannot be an empty string");
        checkArgument(threadsCount > 0, "count of threads cannot be not positive");
        checkArgument(maximumInlineFunSize >= 0, "maximum size of an inline function cannot be negative");

        this.memoryModel = memoryModel;
        this.sdccExecutablePath = sdccExecutablePath;
        this.sdccParameters = sdccParameters;
        this.sdasExecutablePath = sdasExecutablePath;
        this.tempDirectory = tempDirectory;
        this.threadsCount = threadsCount;
        this.writeSettings = writeSettings;
        this.isInlineRelaxed = isInlineRelaxed;
        this.maximumInlineFunSize = maximumInlineFunSize;
        this.allDeclarations = declarations;
        this.refsGraph = refsGraph;
        this.inlineFunctions = ImmutableList.of();
        this.normalFunctions = functions;
        this.inlineFunctionsNames = new HashSet<>();
        this.astStatePreserver = new TypeElementsPreserver(new FunctionSpecifiersAdjuster());
        this.threads = ImmutableList.of();
        this.requestsQueue = new LinkedBlockingQueue<>();
        this.responsesQueue = new LinkedBlockingQueue<>();
        this.estimation = Optional.absent();
    }

    @Override
    public CodeSizeEstimation estimate() throws InterruptedException, IOException {
        if (estimation.isPresent()) {
            return estimation.get();
        }

        createThreads();
        addInitialInlineFunctionsNames();
        updateFunctionsLists();

        int initialInlineFunctionsCount;
        ImmutableMap<String, Range<Integer>> currentEstimation;

        do {
            initialInlineFunctionsCount = inlineFunctionsNames.size();

            prepareDeclarations();
            createHeaderFiles();
            currentEstimation = performEstimation();
            updateInlineFunctions(currentEstimation);
            updateFunctionsLists();
        } while (initialInlineFunctionsCount != inlineFunctionsNames.size());

        restoreDeclarations();
        terminateThreads();

        estimation = Optional.of(CodeSizeEstimation.builder()
                .addAllInlineFunctions(inlineFunctionsNames)
                .putAllFunctionsSizes(currentEstimation)
                .build());
        return estimation.get();
    }

    private void createThreads() {
        final ImmutableList.Builder<Thread> threadsBuilder = ImmutableList.builder();

        for (int i = 0; i < threadsCount; ++i) {
            final Thread estimateThread = new Thread(new EstimateRunnable(i + 1),
                    "estimate-thread-" + (i + 1));
            estimateThread.start();
            threadsBuilder.add(estimateThread);
        }

        this.threads = threadsBuilder.build();
    }

    private void addInitialInlineFunctionsNames() {
        // No initial inline functions if inline specifier is relaxed
        if (isInlineRelaxed) {
            return;
        }

        // Find candidates for inline functions
        final InlineFunctionsFindingVisitor findingVisitor = new InlineFunctionsFindingVisitor();
        for (Declaration declaration : allDeclarations) {
            declaration.accept(findingVisitor, null);
        }
        final ImmutableSet<String> candidates = findingVisitor.getCandidates();

        // Check candidates
        for (FunctionDecl funDecl : normalFunctions) {
            final String uniqueName = DeclaratorUtils.getUniqueName(
                    funDecl.getDeclarator()).get();
            if (candidates.contains(uniqueName) && canBeInline(uniqueName, funDecl.getDeclaration())) {
                inlineFunctionsNames.add(uniqueName);
            }
        }
    }

    private boolean canBeInline(String funUniqueName, FunctionDeclaration declaration) {
        // Spontaneous functions cannot be inline
        if (declaration != null && declaration.getCallAssumptions().compareTo(
                FunctionDeclaration.CallAssumptions.SPONTANEOUS) >= 0) {
            return false;
        }

        /* Only functions whose only evaluated references are calls can be
           inline. */
        final EntityNode node = refsGraph.getOrdinaryIds().get(funUniqueName);
        for (Reference reference : node.getPredecessors()) {
            if (!reference.isInsideNotEvaluatedExpr()) {
                if (reference.getType() != Reference.Type.CALL) {
                    return false;
                }
            }
        }

        return true;
    }

    private void updateFunctionsLists() {
        final ImmutableList.Builder<FunctionDecl> inlineFunctionsBuilder = ImmutableList.builder();
        final ImmutableList.Builder<FunctionDecl> normalFunctionsBuilder = ImmutableList.builder();

        inlineFunctionsBuilder.addAll(inlineFunctions);

        for (FunctionDecl funDecl : normalFunctions) {
            final String uniqueName = DeclaratorUtils.getUniqueName(
                    funDecl.getDeclarator()).get();
            if (inlineFunctionsNames.contains(uniqueName)) {
                inlineFunctionsBuilder.add(funDecl);
            } else {
                normalFunctionsBuilder.add(funDecl);
            }
        }

        inlineFunctions = inlineFunctionsBuilder.build();
        normalFunctions = normalFunctionsBuilder.build();
    }

    private void prepareDeclarations() {
        astStatePreserver.adjust(allDeclarations);
    }

    private void createHeaderFiles() throws IOException {
        // Header with non-banked declarations
        final String nonbankedHeaderFullPath =
                Paths.get(tempDirectory, NAME_NONBANKED_HEADER).toString();
        final CustomDeclarationsWriter declsWriter = new CustomDeclarationsWriter(
                nonbankedHeaderFullPath,
                true,
                CustomDeclarationsWriter.Banking.DEFINED_NOT_BANKED,
                writeSettings
        );
        declsWriter.setPrependedText(Optional.of(ExternalConstants.getExternalDefines()));
        declsWriter.write(allDeclarations);
        appendInlineFunctions(nonbankedHeaderFullPath);

        // Header with banked declarations
        final String bankedHeaderFullPath =
                Paths.get(tempDirectory, NAME_BANKED_HEADER).toString();
        declsWriter.setOutputFile(bankedHeaderFullPath);
        declsWriter.setBanking(CustomDeclarationsWriter.Banking.DEFINED_BANKED);
        declsWriter.write(allDeclarations);
        appendInlineFunctions(bankedHeaderFullPath);
    }

    private void appendInlineFunctions(String fullPath) throws IOException {
        try (final ASTWriter writer = new ASTWriter(fullPath, true, writeSettings)) {
            if (!allDeclarations.isEmpty()) {
                writer.write('\n');
            }
            writer.write(inlineFunctions);
        }
    }

    private ImmutableMap<String, Range<Integer>> performEstimation() throws InterruptedException {
        final ImmutableMap<String, Integer> lowerBounds = performEstimation(false);
        final ImmutableMap<String, Integer> upperBounds = performEstimation(true);

        if (lowerBounds.size() != normalFunctions.size()) {
            throw new RuntimeException("count of functions with lower bounds computed "
                    + lowerBounds.size() + " is different from the count of functions whose size is estimated "
                    + normalFunctions.size());
        } else if (lowerBounds.size() != upperBounds.size()) {
            throw new RuntimeException("count of functions with lower bounds computed "
                    + lowerBounds.size() + " is different from the count of function with upper bound computed "
                    + upperBounds.size());
        }

        // Join lower and upper bounds
        final ImmutableMap.Builder<String, Range<Integer>> fullEstimationBuilder = ImmutableMap.builder();
        for (Map.Entry<String, Integer> lowerBoundEntry : lowerBounds.entrySet()) {
            if (!upperBounds.containsKey(lowerBoundEntry.getKey())) {
                throw new RuntimeException("missing upper bound estimation for '"
                        + lowerBoundEntry.getKey() + "'");
            }
            final int lowerBound = Math.min(lowerBoundEntry.getValue(), upperBounds.get(lowerBoundEntry.getKey()));
            final int upperBound = Math.max(lowerBoundEntry.getValue(), upperBounds.get(lowerBoundEntry.getKey()));
            fullEstimationBuilder.put(lowerBoundEntry.getKey(), Range.closed(lowerBound, upperBound));
        }

        return fullEstimationBuilder.build();
    }

    private ImmutableMap<String, Integer> performEstimation(boolean isBanked) throws InterruptedException {
        final int functionsPerThread = normalFunctions.size() / threadsCount;
        int remainderFunctionsCount = normalFunctions.size() % threadsCount;
        int startIndex = 0;
        int requestsCount = 0;

        // Add requests for threads
        while (startIndex < normalFunctions.size()) {
            int endIndex = startIndex + functionsPerThread;
            if (remainderFunctionsCount > 0) {
                ++endIndex;
                --remainderFunctionsCount;
            }

            final ImmutableList<FunctionDecl> functionsChunk =
                    normalFunctions.subList(startIndex, endIndex);
            requestsQueue.add(new EstimateRequest(functionsChunk, isBanked));
            ++requestsCount;

            startIndex = endIndex;
        }

        // Receive results
        final ResponseCollectingVisitor collectingVisitor = new ResponseCollectingVisitor();
        for (; requestsCount > 0; --requestsCount) {
            responsesQueue.take().accept(collectingVisitor, null);
        }

        return collectingVisitor.getEstimation();
    }

    private void updateInlineFunctions(ImmutableMap<String, Range<Integer>> currentEstimation) {
        for (FunctionDecl inlineCandidate : normalFunctions) {
            final String uniqueName = DeclaratorUtils.getUniqueName(
                    inlineCandidate.getDeclarator()).get();
            final int maximumSize = currentEstimation.get(uniqueName).upperEndpoint();

            if (maximumSize <= maximumInlineFunSize
                    && canBeInline(uniqueName, inlineCandidate.getDeclaration())) {
                inlineFunctionsNames.add(uniqueName);
            }
        }
    }

    private void restoreDeclarations() {
        astStatePreserver.restore(allDeclarations);
    }

    private void terminateThreads() throws InterruptedException {
        // Add requests for termination
        for (int i = 0; i < threadsCount; ++i) {
            requestsQueue.add(new TerminationRequest());
        }

        // Wait for the termination
        for (Thread thread : threads) {
            thread.join();
        }

        // Check messages
        if (!responsesQueue.isEmpty()) {
            throw new RuntimeException("a thread hasn't terminated safely",
                    ((ExceptionResponse) responsesQueue.take()).getException());
        }
    }

    /**
     * Object responsible for adjusting specifiers of functions declarations.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class FunctionSpecifiersAdjuster implements TypeElementsAdjuster {
        @Override
        public void adjustFunctionDefinition(List<TypeElement> specifiers,
                Supplier<List<TypeElement>> substituteSupplier, String uniqueName,
                FunctionDeclaration declarationObj) {
            prepareFunctionSpecifiers(specifiers, substituteSupplier, uniqueName);
        }

        @Override
        public void adjustFunctionDeclaration(List<TypeElement> specifiers,
                Supplier<List<TypeElement>> substituteSupplier, String uniqueName,
                FunctionDeclaration declarationObj) {
            if (declarationObj == null || declarationObj.isDefined()) {
                prepareFunctionSpecifiers(specifiers, substituteSupplier, uniqueName);
            }
        }

        private void prepareFunctionSpecifiers(List<TypeElement> originalSpecifiers,
                    Supplier<List<TypeElement>> substituteSupplier, String uniqueName) {
            final EnumSet<RID> rids = TypeElementUtils.collectRID(originalSpecifiers);

            if (inlineFunctionsNames.contains(uniqueName)) {
                if (rids.contains(RID.EXTERN)) {
                    TypeElementUtils.removeRID(substituteSupplier.get(), RID.EXTERN);
                }
                if (!rids.contains(RID.INLINE)) {
                    substituteSupplier.get().add(0, AstUtils.newRid(RID.INLINE));
                }
                if (!rids.contains(RID.STATIC)) {
                    substituteSupplier.get().add(0, AstUtils.newRid(RID.STATIC));
                }
            } else {
                if (rids.contains(RID.STATIC) || rids.contains(RID.INLINE)) {
                    TypeElementUtils.removeRID(substituteSupplier.get(), RID.STATIC,
                            RID.INLINE);
                }
            }
        }
    }

    /**
     * Visitor that finds unique names of all functions with inline keyword.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class InlineFunctionsFindingVisitor extends ExceptionVisitor<Void, Void> {
        private final ImmutableSet.Builder<String> inlineFunsCandidatesBuilder = ImmutableSet.builder();

        public ImmutableSet<String> getCandidates() {
            return inlineFunsCandidatesBuilder.build();
        }

        @Override
        public Void visitDataDecl(DataDecl declaration, Void arg) {
            final EnumSet<RID> rids = TypeElementUtils.collectRID(declaration.getModifiers());
            if (rids.contains(RID.INLINE)) {
                for (Declaration innerDeclaration : declaration.getDeclarations()) {
                    innerDeclaration.accept(this, null);
                }
            }
            return null;
        }

        @Override
        public Void visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            declaration.getDeclaration().accept(this, null);
            return null;
        }

        @Override
        public Void visitFunctionDecl(FunctionDecl declaration, Void arg) {
            final EnumSet<RID> rids = TypeElementUtils.collectRID(declaration.getModifiers());
            if (rids.contains(RID.INLINE)) {
                final String uniqueName = DeclaratorUtils.getUniqueName(
                        declaration.getDeclarator()).get();
                inlineFunsCandidatesBuilder.add(uniqueName);
            }
            return null;
        }

        @Override
        public Void visitVariableDecl(VariableDecl declaration, Void arg) {
            final Optional<NestedDeclarator> deepestNestedDeclarator =
                    DeclaratorUtils.getDeepestNestedDeclarator(declaration.getDeclarator().get());

            if (deepestNestedDeclarator.isPresent()
                    && deepestNestedDeclarator.get() instanceof FunctionDeclarator) {
                final String uniqueName = DeclaratorUtils.getUniqueName(
                        declaration.getDeclarator().get()).get();
                inlineFunsCandidatesBuilder.add(uniqueName);
            } else if (deepestNestedDeclarator.isPresent()
                    && deepestNestedDeclarator.get() instanceof InterfaceRefDeclarator) {
                throw new RuntimeException("unexpected interface reference declarator");
            }

            return null;
        }
    }

    /**
     * Runnable for the estimation threads.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class EstimateRunnable implements Runnable, Request.Visitor<Void, Void> {
        private final String sourceFileFullPath;
        private final String assemblyFileFullPath;
        private final String cleanedAssemblyFileFullPath;
        private final String relocationFileFullPath;
        private final ProcessBuilder sdccProcessBuilder;
        private final ProcessBuilder sdasProcessBuilder;
        private final FunctionsSizesResolver functionsSizesResolver;
        private boolean terminate;

        /**
         * Private constructor to limit its accessibility.
         */
        private EstimateRunnable(int id) {
            this.sourceFileFullPath = Paths.get(tempDirectory, "fun" + id + ".c").toString();
            this.assemblyFileFullPath = Paths.get(tempDirectory, "fun" + id + ".asm").toString();
            this.cleanedAssemblyFileFullPath = Paths.get(tempDirectory, "fun" + id + "-cleaned.asm").toString();
            this.relocationFileFullPath = Paths.get(tempDirectory, "fun" + id + "-cleaned.rel").toString();
            this.sdccProcessBuilder = new ProcessBuilder();
            this.sdasProcessBuilder = new ProcessBuilder();
            this.functionsSizesResolver = new FunctionsSizesResolver(NAME_CODE_SEGMENT);
            this.terminate = false;
        }

        @Override
        public void run() {
            try {
                configureSDCCProcessBuilder();
                configureSDASProcessBuilder();

                while (!terminate) {
                    // Handle next request
                    requestsQueue.take().accept(this, null);
                }
            } catch (Exception e) {
                /* Report the exception by putting it into the responses queue
                   and terminate. */
                responsesQueue.add(new ExceptionResponse(e));
            }
        }

        @Override
        public Void visit(EstimateRequest request, Void arg) {
            try {
                final ChunkEstimator chunkEstimator = new ChunkEstimator(
                        request.getFunctions(),
                        request.isBanked,
                        sdccProcessBuilder,
                        sdasProcessBuilder,
                        functionsSizesResolver,
                        sourceFileFullPath,
                        assemblyFileFullPath,
                        cleanedAssemblyFileFullPath,
                        relocationFileFullPath
                );
                responsesQueue.add(new EstimationResponse(chunkEstimator.estimate(),
                        request.isBanked()));
                return null;
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException("estimation operation failed", e);
            }
        }

        @Override
        public Void visit(TerminationRequest request, Void arg) {
            terminate = true;
            return null;
        }

        private void configureSDCCProcessBuilder() {
            final List<String> sdccCmdList = new ArrayList<>();
            sdccCmdList.add(sdccExecutablePath);
            if (memoryModel.isPresent()) {
                sdccCmdList.add(memoryModel.get().getOption());
            }
            sdccCmdList.add("-S");
            sdccCmdList.add("-mmcs51");
            sdccCmdList.add("-o");
            sdccCmdList.add(assemblyFileFullPath);
            sdccCmdList.addAll(sdccParameters);
            sdccCmdList.add(sourceFileFullPath);

            // Update the builder
            this.sdccProcessBuilder.command(sdccCmdList)
                    .directory(new File(tempDirectory));
        }

        private void configureSDASProcessBuilder() {
            this.sdasProcessBuilder.command(sdasExecutablePath,
                    "-go", cleanedAssemblyFileFullPath)
                .directory(new File(tempDirectory));
        }
    }

    /**
     * Objects that act in estimate threads and perform estimation of chunks.
     * It is directly responsible for invoking SDCC, removing definitions of
     * inline function from generated files, invoking the assembler and reading
     * .REL files.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class ChunkEstimator {
        private final boolean isBanked;
        private final ProcessBuilder sdccProcessBuilder;
        private final ProcessBuilder sdasProcessBuilder;
        private final FunctionsSizesResolver functionsSizesResolver;
        private final String sourceFileFullPath;
        private final String assemblyFileFullPath;
        private final String cleanedAssemblyFileFullPath;
        private final String relocationFileFullPath;
        private final ImmutableList<FunctionDecl> chunk;
        private int estimationUnit;
        private int nextFunIndex;

        private ChunkEstimator(ImmutableList<FunctionDecl> chunk, boolean isBanked,
                    ProcessBuilder sdccProcessBuilder, ProcessBuilder sdasProcessBuilder,
                    FunctionsSizesResolver functionsSizesResolver, String sourceFileFullPath,
                    String assemblyFileFullPath, String cleanedAssemblyFileFullPath,
                    String relocationFileFullPath) {
            this.isBanked = isBanked;
            this.sdccProcessBuilder = sdccProcessBuilder;
            this.sdasProcessBuilder = sdasProcessBuilder;
            this.functionsSizesResolver = functionsSizesResolver;
            this.sourceFileFullPath = sourceFileFullPath;
            this.assemblyFileFullPath = assemblyFileFullPath;
            this.cleanedAssemblyFileFullPath = cleanedAssemblyFileFullPath;
            this.relocationFileFullPath = relocationFileFullPath;
            this.chunk = chunk;
            this.estimationUnit = chunk.size();
            this.nextFunIndex = 0;
        }

        private ImmutableMap<String, Integer> estimate() throws InterruptedException, IOException {
            final ImmutableMap.Builder<String, Integer> estimationBuilder =
                    ImmutableMap.builder();

            while (nextFunIndex < chunk.size()) {
                runSDCC();
                removeInlineFunctions();
                runAssembler();
                readFunctionsSizes(estimationBuilder);
                nextFunIndex = Math.min(chunk.size(), nextFunIndex + estimationUnit);
            }

            return estimationBuilder.build();
        }

        private void runSDCC() throws InterruptedException, IOException {
            final CustomDeclarationsWriter.Banking banking = isBanked
                    ? CustomDeclarationsWriter.Banking.DEFINED_BANKED
                    : CustomDeclarationsWriter.Banking.DEFINED_NOT_BANKED;
            final String includedHeader = isBanked
                    ? NAME_BANKED_HEADER
                    : NAME_NONBANKED_HEADER;
            final CustomDeclarationsWriter declsWriter = new CustomDeclarationsWriter(
                    sourceFileFullPath,
                    false,
                    banking,
                    writeSettings
            );
            declsWriter.setPrependedText(Optional.of("#include \"" + includedHeader
                    + "\"\n#pragma codeseg " + NAME_CODE_SEGMENT + "\n\n"));

            int returnCode;
            int initialEstimationUnit;

            do {
                initialEstimationUnit = estimationUnit;

                // Write declarations
                final int endIndex = Math.min(nextFunIndex + estimationUnit, chunk.size());
                declsWriter.write(chunk.subList(nextFunIndex, endIndex));

                // Run SDCC
                returnCode = sdccProcessBuilder.start().waitFor();
                if (returnCode != 0) {
                    estimationUnit = Math.max(estimationUnit / 2, 1);
                }
            } while (returnCode != 0 && initialEstimationUnit != 1);

            if (returnCode != 0) {
                throw new RuntimeException("SDCC returned code " + returnCode);
            }
        }

        private void removeInlineFunctions() throws IOException {
            new AssemblyFunctionsRemover(assemblyFileFullPath, cleanedAssemblyFileFullPath,
                    inlineFunctionsNames).remove();
        }

        private void runAssembler() throws InterruptedException, IOException {
            final int returnCode = sdasProcessBuilder.start().waitFor();
            if (returnCode != 0) {
                throw new RuntimeException("SDAS returned code " + returnCode);
            }
        }

        private void readFunctionsSizes(ImmutableMap.Builder<String, Integer> sizesBuilder)
                throws FileNotFoundException {
            sizesBuilder.putAll(functionsSizesResolver.resolve(relocationFileFullPath));
        }
    }

    /**
     * Request for the estimation thread.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static abstract class Request {
        private Request() {
        }

        public abstract <R, A> R accept(Visitor<R, A> visitor, A arg);

        public interface Visitor<R, A> {
            R visit(EstimateRequest request, A arg);
            R visit(TerminationRequest request, A arg);
        }
    }

    /**
     * Request for an estimation operation for a thread.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class EstimateRequest extends Request {
        private final boolean isBanked;
        private final ImmutableList<FunctionDecl> functions;

        private EstimateRequest(ImmutableList<FunctionDecl> functions, boolean isBanked) {
            checkNotNull(functions, "functions cannot be null");
            this.isBanked = isBanked;
            this.functions = functions;
        }

        public boolean isBanked() {
            return isBanked;
        }

        public ImmutableList<FunctionDecl> getFunctions() {
            return functions;
        }

        @Override
        public <R, A> R accept(Visitor<R, A> visitor, A arg) {
            return visitor.visit(this, arg);
        }
    }

    /**
     * Message for an estimation thread that causes its termination.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class TerminationRequest extends Request {
        private TerminationRequest() {
        }

        @Override
        public <R, A> R accept(Visitor<R, A> visitor, A arg) {
            return visitor.visit(this, arg);
        }
    }

    /**
     * Response from an estimation thread.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static abstract class Response {
        private Response() {
        }

        public abstract <R, A> R accept(Visitor<R, A> visitor, A arg);

        public interface Visitor<R, A> {
            R visit(EstimationResponse response, A arg);
            R visit(ExceptionResponse response, A arg);
        }
    }

    /**
     * Response with the result of the estimation.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class EstimationResponse extends Response {
        private final boolean isBanked;
        private final ImmutableMap<String, Integer> estimation;

        private EstimationResponse(ImmutableMap<String, Integer> estimation, boolean isBanked) {
            checkNotNull(estimation, "estimation cannot be null");
            this.isBanked = isBanked;
            this.estimation = estimation;
        }

        public boolean isBanked() {
            return isBanked;
        }

        public ImmutableMap<String, Integer> getEstimation() {
            return estimation;
        }

        @Override
        public <R, A> R accept(Visitor<R, A> visitor, A arg) {
            return visitor.visit(this, arg);
        }
    }

    /**
     * Message sent by an estimation thread before terminating (either requested
     * or not) if an exception occurred.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class ExceptionResponse extends Response {
        private final Exception exception;

        private ExceptionResponse(Exception exception) {
            checkNotNull(exception, "exception cannot be null");
            this.exception = exception;
        }

        public Exception getException() {
            return exception;
        }

        @Override
        public <R, A> R accept(Visitor<R, A> visitor, A arg) {
            return visitor.visit(this, arg);
        }
    }

    /**
     * Visitor responsible for collecting results from threads.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class ResponseCollectingVisitor implements Response.Visitor<Void, Void> {
        private final ImmutableMap.Builder<String, Integer> estimationBuilder = ImmutableMap.builder();

        private ImmutableMap<String, Integer> getEstimation() {
            return estimationBuilder.build();
        }

        @Override
        public Void visit(EstimationResponse response, Void arg) {
            estimationBuilder.putAll(response.getEstimation());
            return null;
        }

        @Override
        public Void visit(ExceptionResponse response, Void arg) {
            throw new RuntimeException("estimation operation failed", response.getException());
        }
    }
}
