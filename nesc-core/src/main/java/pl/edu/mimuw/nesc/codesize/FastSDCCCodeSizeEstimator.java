package pl.edu.mimuw.nesc.codesize;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.astutil.TypeElementUtils;
import pl.edu.mimuw.nesc.astutil.TypeElementsAdjuster;
import pl.edu.mimuw.nesc.astutil.TypeElementsPreserver;
import pl.edu.mimuw.nesc.astwriting.CustomDeclarationsWriter;
import pl.edu.mimuw.nesc.astwriting.WriteSettings;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;
import pl.edu.mimuw.nesc.external.ExternalConstants;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * SDCC code size estimator that estimates sizes of multiple functions
 * simultaneously which is significantly faster than estimation of a single
 * function at once.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class FastSDCCCodeSizeEstimator implements CodeSizeEstimator {
    /**
     * Name of the header file with non-banked declarations that will be
     * created.
     */
    private static final String NAME_NONBANKED_HEADER = "nonbanked_decls.h";

    /**
     * Name of the header file with banked declarations that will be created.
     */
    private static final String NAME_BANKED_HEADER = "banked_decls.h";

    /**
     * Name of the file with functions whose sizes are estimated that will be
     * created.
     */
    private static final String NAME_CODE_FILE = "fun.c";

    /**
     * Name of the file created as the result of compilation by SDCC with
     * functions sizes.
     */
    private static final String NAME_REL_FILE = "fun.rel";

    /**
     * Name of the code segment with functions whose sizes are estimated.
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
     * Directory used for saving files necessary for the estimation.
     */
    private final String tempDirectory;

    /**
     * Settings for writing AST nodes for the estimation.
     */
    private final WriteSettings writeSettings;

    /**
     * List with all declarations that constitute a NesC program.
     */
    private final ImmutableList<Declaration> allDeclarations;

    /**
     * Functions whose size will be estimated.
     */
    private final ImmutableList<FunctionDecl> functions;

    /**
     * Index of the next function in {@link FastSDCCCodeSizeEstimator#functions}
     * whose size is to be estimated.
     */
    private int nextFunIndex;

    /**
     * Count of functions whose sizes are estimated simultaneously.
     */
    private int estimationUnit;

    /**
     * Object responsible for preserving the state of AST nodes.
     */
    private final TypeElementsPreserver specifiersPreserver;

    /**
     * Object used for invoking SDCC.
     */
    private final ProcessBuilder sdccProcessBuilder;

    /**
     * Object for reading .REL files and computing sizes of functions.
     */
    private final FunctionsSizesResolver functionsSizesResolver;

    /**
     * Result of the estimation operation.
     */
    private Optional<CodeSizeEstimation> estimation;

    FastSDCCCodeSizeEstimator(
            ImmutableList<Declaration> declarations,
            ImmutableList<FunctionDecl> functions,
            String sdccExecutablePath,
            ImmutableList<String> sdccParameters,
            Optional<SDCCMemoryModel> memoryModel,
            String tempDirectory,
            WriteSettings writeSettings
    ) {
        checkNotNull(declarations, "declarations cannot be null");
        checkNotNull(functions, "functions cannot be null");
        checkNotNull(sdccExecutablePath, "SDCC executable path cannot be null");
        checkNotNull(sdccParameters, "SDCC parameters cannot be null");
        checkNotNull(memoryModel, "SDCC memory model cannot be null");
        checkNotNull(tempDirectory, "temporary directory cannot be null");
        checkNotNull(writeSettings, "write settings cannot be null");
        checkArgument(!sdccExecutablePath.isEmpty(), "SDCC executable path cannot be an empty string");
        checkArgument(!tempDirectory.isEmpty(), "temporary directory cannot be null");

        this.allDeclarations = declarations;
        this.sdccExecutablePath = sdccExecutablePath;
        this.sdccParameters = sdccParameters;
        this.memoryModel = memoryModel;
        this.tempDirectory = tempDirectory;
        this.writeSettings = writeSettings;
        this.functions = functions;
        this.nextFunIndex = 0;
        this.estimationUnit = this.functions.size();
        this.specifiersPreserver = new TypeElementsPreserver(new FunctionSpecifiersAdjuster());
        this.sdccProcessBuilder = new ProcessBuilder();
        this.functionsSizesResolver = new FunctionsSizesResolver(NAME_CODE_SEGMENT);
        this.estimation = Optional.absent();
    }

    @Override
    public CodeSizeEstimation estimate() throws InterruptedException, IOException {
        if (estimation.isPresent()) {
            return estimation.get();
        }

        prepareDeclarations();
        createHeaderFiles();
        configureProcessBuilder();
        performEstimation();
        restoreDeclarations();

        return estimation.get();
    }

    private void prepareDeclarations() {
        specifiersPreserver.adjust(allDeclarations);
    }

    private void prepareFunctionSpecifiers(List<TypeElement> typeElements,
                Supplier<List<TypeElement>> copySupplier) {
        final EnumSet<RID> rids = TypeElementUtils.collectRID(typeElements);

        if (rids.contains(RID.STATIC) || rids.contains(RID.INLINE)) {
            TypeElementUtils.removeRID(copySupplier.get(), RID.STATIC,
                    RID.INLINE);
        }
    }

    private void createHeaderFiles() throws IOException {
        // Header file with non-banked declarations
        final CustomDeclarationsWriter declsWriter = new CustomDeclarationsWriter(
                Paths.get(tempDirectory, NAME_NONBANKED_HEADER).toString(),
                true,
                CustomDeclarationsWriter.Banking.DEFINED_NOT_BANKED,
                writeSettings
        );
        declsWriter.setPrependedText(Optional.of(ExternalConstants.getExternalDefines()));
        declsWriter.write(allDeclarations);

        // Header file with banked declarations
        declsWriter.setOutputFile(Paths.get(tempDirectory, NAME_BANKED_HEADER).toString());
        declsWriter.setBanking(CustomDeclarationsWriter.Banking.DEFINED_BANKED);
        declsWriter.write(allDeclarations);
    }

    private void configureProcessBuilder() {
        // Create SDCC command invocation
        final List<String> sdccCmdList = new ArrayList<>();
        sdccCmdList.add(sdccExecutablePath);
        if (memoryModel.isPresent()) {
            sdccCmdList.add(memoryModel.get().getOption());
        }
        sdccCmdList.add("-c");
        sdccCmdList.add("-mmcs51");
        sdccCmdList.addAll(sdccParameters);
        sdccCmdList.add(Paths.get(tempDirectory, NAME_CODE_FILE).toString());

        // Update the builder
        this.sdccProcessBuilder.command(sdccCmdList)
                .directory(new File(tempDirectory));
    }

    private void performEstimation() throws InterruptedException, IOException {
        final CodeSizeEstimation.Builder estimationBuilder =
                CodeSizeEstimation.builder();

        while (nextFunIndex < functions.size()) {
            runSDCC(false);
            final ImmutableMap<String, Integer> lowerBounds = determineFunctionsSizes();
            runSDCC(true);
            final ImmutableMap<String, Integer> upperBounds = determineFunctionsSizes();
            accumulateSizes(estimationBuilder, lowerBounds, upperBounds);
            nextFunIndex += estimationUnit;
        }

        estimation = Optional.of(estimationBuilder.build());
    }

    private void runSDCC(boolean isBanked) throws IOException, InterruptedException {
        final CustomDeclarationsWriter.Banking banking = isBanked
                ? CustomDeclarationsWriter.Banking.DEFINED_BANKED
                : CustomDeclarationsWriter.Banking.DEFINED_NOT_BANKED;
        final String includedHeader = isBanked
                ? NAME_BANKED_HEADER
                : NAME_NONBANKED_HEADER;
        final CustomDeclarationsWriter declsWriter = new CustomDeclarationsWriter(
                Paths.get(tempDirectory, NAME_CODE_FILE).toString(),
                false,
                banking,
                writeSettings
        );

        int returnCode;
        int initialEstimationUnit;

        do {
            initialEstimationUnit = estimationUnit;

            // Write declarations to file
            final int endIndex = Math.min(nextFunIndex + estimationUnit, functions.size());
            declsWriter.setPrependedText(Optional.of("#include \"" + includedHeader
                    + "\"\n#pragma codeseg " + NAME_CODE_SEGMENT + "\n\n"));
            declsWriter.write(functions.subList(nextFunIndex, endIndex));

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

    private ImmutableMap<String, Integer> determineFunctionsSizes() throws FileNotFoundException {
        return functionsSizesResolver.resolve(Paths.get(tempDirectory, NAME_REL_FILE).toString());
    }

    private void accumulateSizes(CodeSizeEstimation.Builder estimationBuilder,
                ImmutableMap<String, Integer> lowerBounds, ImmutableMap<String, Integer> upperBounds) {
        if (lowerBounds.size() != upperBounds.size()) {
            throw new RuntimeException("size of lower bounds map " + lowerBounds.size() +
                    "differs from the size of the upper bounds map " + upperBounds.size());
        } else if (lowerBounds.size() != estimationUnit
                && lowerBounds.size() != functions.size() - nextFunIndex) {
            throw new RuntimeException("actual size of maps with lower and upper bounds "
                    + lowerBounds.size() + " differs from the estimation unit "
                    + estimationUnit + " and from the count of all remaining functions "
                    + (functions.size() - nextFunIndex));
        }

        for (Map.Entry<String, Integer> lowerBoundEntry : lowerBounds.entrySet()) {
            if (!upperBounds.containsKey(lowerBoundEntry.getKey())) {
                throw new RuntimeException("cannot find entry in the upper bounds map for function '"
                        + lowerBoundEntry.getKey() + "'");
            }

            estimationBuilder.putFunctionSize(lowerBoundEntry.getKey(), Range.closed(
                    lowerBoundEntry.getValue(), upperBounds.get(lowerBoundEntry.getKey())));
        }
    }

    private void restoreDeclarations() {
        specifiersPreserver.restore(allDeclarations);
    }

    /**
     * Class used for adjusting specifiers of functions.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class FunctionSpecifiersAdjuster implements TypeElementsAdjuster {
        @Override
        public void adjustFunctionDefinition(List<TypeElement> specifiers,
                Supplier<List<TypeElement>> substituteSupplier, String uniqueName,
                FunctionDeclaration declarationObj) {
            prepareFunctionSpecifiers(specifiers, substituteSupplier);
        }

        @Override
        public void adjustFunctionDeclaration(List<TypeElement> specifiers,
                Supplier<List<TypeElement>> substituteSupplier, String uniqueName,
                FunctionDeclaration declarationObj) {
            if (declarationObj == null || declarationObj.isDefined()) {
                prepareFunctionSpecifiers(specifiers, substituteSupplier);
            }
        }
    }
}
