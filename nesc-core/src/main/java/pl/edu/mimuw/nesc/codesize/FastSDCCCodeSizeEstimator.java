package pl.edu.mimuw.nesc.codesize;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Range;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.DataDecl;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.ExceptionVisitor;
import pl.edu.mimuw.nesc.ast.gen.ExtensionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDecl;
import pl.edu.mimuw.nesc.ast.gen.FunctionDeclarator;
import pl.edu.mimuw.nesc.ast.gen.InterfaceRefDeclarator;
import pl.edu.mimuw.nesc.ast.gen.NestedDeclarator;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.ast.gen.TypeElement;
import pl.edu.mimuw.nesc.ast.gen.VariableDecl;
import pl.edu.mimuw.nesc.astutil.DeclaratorUtils;
import pl.edu.mimuw.nesc.astutil.TypeElementUtils;
import pl.edu.mimuw.nesc.astutil.NodesCopier;
import pl.edu.mimuw.nesc.astwriting.CustomDeclarationsWriter;
import pl.edu.mimuw.nesc.astwriting.WriteSettings;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

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
     * Regular expression that specifies the language of numbers in hexadecimal
     * notation.
     */
    private static final String REGEXP_HEX_NUMBER = "[0-9a-fA-F]+";

    /**
     * Regular expression that depicts the header of the code segment with
     * functions whose code size is estimated.
     */
    private static final Pattern REGEXP_CODE_SEGMENT = Pattern.compile("A "
            + NAME_CODE_SEGMENT + " size (?<size>" + REGEXP_HEX_NUMBER
            + ") flags " + REGEXP_HEX_NUMBER + " addr " + REGEXP_HEX_NUMBER);

    /**
     * Regular expression that describes a single entry of a code segment in
     * a .REL file.
     */
    private static final Pattern REGEXP_SEGMENT_ENTRY = Pattern.compile(
            "S _(?<funName>\\w+) Def(?<offset>" + REGEXP_HEX_NUMBER + ")");

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
     * Data that enables restoring original state of AST nodes of the NesC
     * program.
     */
    private final Map<Node, OriginalState> astPreservationData;

    /**
     * Object used for invoking SDCC.
     */
    private final ProcessBuilder sdccProcessBuilder;

    /**
     * Result of the estimation operation.
     */
    private Optional<ImmutableMap<String, Range<Integer>>> estimation;

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
        this.astPreservationData = new HashMap<>();
        this.sdccProcessBuilder = new ProcessBuilder();
        this.estimation = Optional.absent();
    }

    @Override
    public ImmutableMap<String, Range<Integer>> estimate() throws InterruptedException, IOException {
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
        final PreparingVisitor preparingVisitor = new PreparingVisitor();
        for (Declaration declaration : allDeclarations) {
            declaration.accept(preparingVisitor, null);
        }
    }

    private Optional<LinkedList<TypeElement>> prepareFunctionSpecifiers(LinkedList<TypeElement> typeElements) {
        final EnumSet<RID> rids = TypeElementUtils.collectRID(typeElements);
        final NodesCopier<TypeElement> copier = new NodesCopier<>(typeElements);

        if (rids.contains(RID.STATIC) || rids.contains(RID.INLINE)) {
            TypeElementUtils.removeRID(copier.getCopiedNodes(), RID.STATIC,
                    RID.INLINE);
        }

        return copier.maybeCopy();
    }

    private void createHeaderFiles() throws IOException {
        // Header file with non-banked declarations
        final CustomDeclarationsWriter declsWriter = new CustomDeclarationsWriter(
                Paths.get(tempDirectory, NAME_NONBANKED_HEADER).toString(),
                true,
                CustomDeclarationsWriter.Banking.DEFINED_NOT_BANKED,
                writeSettings
        );
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
        sdccCmdList.addAll(sdccParameters);
        sdccCmdList.add(Paths.get(tempDirectory, NAME_CODE_FILE).toString());

        // Update the builder
        this.sdccProcessBuilder.command(sdccCmdList)
                .directory(new File(tempDirectory));
    }

    private void performEstimation() throws InterruptedException, IOException {
        final ImmutableMap.Builder<String, Range<Integer>> estimationBuilder =
                ImmutableMap.builder();

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
        return computeFunctionsSizes(readSegmentData());
    }

    private CodeSegmentData readSegmentData() throws FileNotFoundException {
        final String relFilePath = Paths.get(tempDirectory, NAME_REL_FILE).toString();
        final Map<String, Integer> offsets = new HashMap<>();
        Optional<Integer> totalSize = Optional.absent();

        try (final Scanner scanner = new Scanner(new FileInputStream(relFilePath))) {
            while (scanner.hasNextLine()) {
                final String line = scanner.nextLine();

                if (!totalSize.isPresent()) {
                    final Matcher headerMatcher = REGEXP_CODE_SEGMENT.matcher(line);
                    if (headerMatcher.matches()) {
                        totalSize = Optional.of(Integer.valueOf(headerMatcher.group("size"), 16));
                    }
                } else {
                    final Matcher entryMatcher = REGEXP_SEGMENT_ENTRY.matcher(line);
                    if (entryMatcher.matches()) {
                        offsets.put(entryMatcher.group("funName"),
                                Integer.valueOf(entryMatcher.group("offset"), 16));
                    } else {
                        break;
                    }
                }
            }
        }

        if (!totalSize.isPresent()) {
            throw new RuntimeException("cannot find the code segment entry in .REL file");
        }

        return new CodeSegmentData(totalSize.get(), offsets);
    }

    private ImmutableMap<String, Integer> computeFunctionsSizes(CodeSegmentData segmentData) {
        // Sort functions in ascending order according to their offsets
        final List<String> functionsNames = new ArrayList<>(segmentData.offsets.keySet());
        Collections.sort(functionsNames, new FunctionOffsetComparator(segmentData.offsets));

        // Compute sizes of functions
        final Map<String, Integer> workMap = segmentData.offsets;
        for (int i = 0; i < functionsNames.size(); ++i) {
            final int minuend = i + 1 < functionsNames.size()
                    ? workMap.get(functionsNames.get(i + 1))
                    : segmentData.totalSize;
            workMap.put(functionsNames.get(i), minuend - workMap.get(functionsNames.get(i)));
        }

        return ImmutableMap.copyOf(workMap);
    }

    private void accumulateSizes(ImmutableMap.Builder<String, Range<Integer>> sizesBuilder,
                ImmutableMap<String, Integer> lowerBounds, ImmutableMap<String, Integer> upperBounds) {
        if (lowerBounds.size() != upperBounds.size()) {
            throw new RuntimeException("size of lower bounds map " + lowerBounds.size() +
                    "differs from the size of the upper bounds map " + upperBounds.size());
        } else if (lowerBounds.size() != estimationUnit) {
            throw new RuntimeException("actual size of maps with lower and upper bounds "
                    + lowerBounds.size() + " differs from the estimation unit "
                    + estimationUnit);
        }

        for (Map.Entry<String, Integer> lowerBoundEntry : lowerBounds.entrySet()) {
            if (!upperBounds.containsKey(lowerBoundEntry.getKey())) {
                throw new RuntimeException("cannot find entry in the upper bounds map for function '"
                        + lowerBoundEntry.getKey() + "'");
            }

            sizesBuilder.put(lowerBoundEntry.getKey(), Range.closed(lowerBoundEntry.getValue(),
                    upperBounds.get(lowerBoundEntry.getKey())));
        }
    }

    private void restoreDeclarations() {
        final RestoringVisitor restoringVisitor = new RestoringVisitor();
        for (Declaration declaration : allDeclarations) {
            declaration.accept(restoringVisitor, null);
        }
    }

    /**
     * Visitor that prepares declarations for the code size estimation.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class PreparingVisitor extends ExceptionVisitor<Void, Void> {
        @Override
        public Void visitFunctionDecl(FunctionDecl declaration, Void arg) {
            final LinkedList<TypeElement> originalSpecifiers = declaration.getModifiers();
            final Optional<LinkedList<TypeElement>> newTypeElements =
                    prepareFunctionSpecifiers(declaration.getModifiers());

            if (newTypeElements.isPresent()) {
                declaration.setModifiers(newTypeElements.get());
                astPreservationData.put(declaration, new OriginalState(originalSpecifiers));
            }

            return null;
        }

        @Override
        public Void visitExtensionDecl(ExtensionDecl declaration, Void arg) {
            declaration.getDeclaration().accept(this, null);
            return null;
        }

        @Override
        public Void visitDataDecl(DataDecl declaration, Void arg) {
            if (declaration.getDeclarations().isEmpty()) {
                return null;
            } else if (declaration.getDeclarations().size() != 1) {
                throw new IllegalStateException("unseparated declarations encountered");
            }

            final VariableDecl variableDecl =
                    (VariableDecl) declaration.getDeclarations().getFirst();
            final Optional<NestedDeclarator> deepestNestedDeclarator =
                    DeclaratorUtils.getDeepestNestedDeclarator(variableDecl.getDeclarator().get());

            if (deepestNestedDeclarator.isPresent()
                    && deepestNestedDeclarator.get() instanceof FunctionDeclarator) {
                // Forward declaration of a function
                final FunctionDeclaration declarationObj =
                        (FunctionDeclaration) variableDecl.getDeclaration();
                if (declarationObj != null && !declarationObj.isDefined()) {
                    return null;
                }
                final LinkedList<TypeElement> originalSpecifiers = declaration.getModifiers();
                final Optional<LinkedList<TypeElement>> newTypeElements =
                        prepareFunctionSpecifiers(declaration.getModifiers());
                if (newTypeElements.isPresent()) {
                    declaration.setModifiers(newTypeElements.get());
                    astPreservationData.put(declaration, new OriginalState(originalSpecifiers));
                }
            } else if (deepestNestedDeclarator.isPresent()
                    && deepestNestedDeclarator.get() instanceof InterfaceRefDeclarator) {
                throw new RuntimeException("unexpected interface reference declarator");
            }

            return null;
        }
    }

    /**
     * Visitor that restores state of declarations.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private final class RestoringVisitor extends ExceptionVisitor<Void, Void> {
        @Override
        public Void visitDataDecl(DataDecl declaration, Void arg) {
            if (astPreservationData.containsKey(declaration)) {
                declaration.setModifiers(astPreservationData.get(declaration).typeElements);
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
            if (astPreservationData.containsKey(declaration)) {
                declaration.setModifiers(astPreservationData.get(declaration).typeElements);
            }
            return null;
        }
    }

    /**
     * Helper class whose object carry information about the original state of
     * an AST declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class OriginalState {
        private final LinkedList<TypeElement> typeElements;

        private OriginalState(LinkedList<TypeElement> typeElements) {
            checkNotNull(typeElements, "type elements cannot be null");
            this.typeElements = typeElements;
        }
    }

    /**
     * A small helper class that represents data read from .REL file created by
     * SDCC.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class CodeSegmentData {
        private final Map<String, Integer> offsets;
        private final int totalSize;

        private CodeSegmentData(int totalSize, Map<String, Integer> offsets) {
            checkArgument(totalSize >= 0, "total size cannot be negative");
            checkNotNull(offsets, "offsets cannot be null");
            this.offsets = offsets;
            this.totalSize = totalSize;
        }
    }

    /**
     * Comparator that compares names of functions using their offsets from the
     * beginning of a code segment.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static final class FunctionOffsetComparator implements Comparator<String> {
        private final Map<String, Integer> offsets;

        private FunctionOffsetComparator(Map<String, Integer> offsets) {
            checkNotNull(offsets, "offsets cannot be null");
            this.offsets = offsets;
        }

        @Override
        public int compare(String funName1, String funName2) {
            checkNotNull(funName1, "name of the first function cannot be null");
            checkNotNull(funName2, "name of the second function cannot be null");
            checkState(offsets.containsKey(funName1), "unknown function '"
                    + funName1 + "'");
            checkState(offsets.containsKey(funName2), "unknown function '"
                    + funName2 + "'");
            return Integer.compare(offsets.get(funName1), offsets.get(funName2));
        }
    }
}
