package pl.edu.mimuw.nesc;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import java.io.FileNotFoundException;
import java.io.InputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.analysis.SchedulerAnalyzer;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Interface;
import pl.edu.mimuw.nesc.common.AtomicSpecification;
import pl.edu.mimuw.nesc.common.NesCFileType;
import pl.edu.mimuw.nesc.exception.ABILoadFailureException;
import pl.edu.mimuw.nesc.exception.InvalidOptionsException;
import pl.edu.mimuw.nesc.filesgraph.FilesGraph;
import pl.edu.mimuw.nesc.filesgraph.GraphFile;
import pl.edu.mimuw.nesc.filesgraph.visitor.DefaultFileGraphVisitor;
import pl.edu.mimuw.nesc.load.FileCache;
import pl.edu.mimuw.nesc.load.LoadExecutor;
import pl.edu.mimuw.nesc.option.OptionsHelpPrinter;
import pl.edu.mimuw.nesc.option.OptionsHolder;
import pl.edu.mimuw.nesc.option.OptionsParser;
import pl.edu.mimuw.nesc.common.SchedulerSpecification;
import pl.edu.mimuw.nesc.option.OptionsProvider;
import pl.edu.mimuw.nesc.problem.NescError;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import pl.edu.mimuw.nesc.problem.NescIssue;
import pl.edu.mimuw.nesc.problem.NescWarning;
import pl.edu.mimuw.nesc.problem.issue.Issue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Frontend implementation for nesc language.
 *
 * TODO: check presence and correctness of definitions for 'atomic'
 * (__nesc_atomic_t, ...) and declared combining functions
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class NescFrontend implements Frontend {

    private static final String DEFAULT_OUTPUT_FILE = "app.c";
    private static final String DIR_PREDEFINED_ABI = "abi";
    private static final Logger LOG = Logger.getLogger(NescFrontend.class);

    public static Builder builder() {
        return new Builder();
    }

    private final boolean isStandalone;
    private final ImmutableSet<String> targetAttributes0;
    private final ImmutableSet<String> targetAttributes1;
    private final String defaultABIPlatform;
    private final Map<ContextRef, FrontendContext> contextsMap;

    private NescFrontend(Builder builder) {
        this.isStandalone = builder.isStandalone;
        this.targetAttributes0 = builder.targetAttributes0Builder.build();
        this.targetAttributes1 = builder.targetAttributes1Builder.build();
        this.defaultABIPlatform = builder.defaultABIPlatform;
        this.contextsMap = new HashMap<>();
    }

    @Override
    public ContextRef createContext(String[] args) throws InvalidOptionsException {
        checkNotNull(args, "arguments cannot be null");
        LOG.info("Create context; " + Arrays.toString(args));
        return _createContext(createContextFromOptions(args));
    }

    @Override
    public ContextRef createContext(OptionsProvider provider) throws InvalidOptionsException {
        checkNotNull(provider, "provider cannot be null");
        LOG.info("Create context using provided options");
        return _createContext(createContextFromOptions(provider.getOptions(),
                provider, provider.parametersCount()));
    }

    private ContextRef _createContext(FrontendContext context) {
        checkNotNull(context, "frontend context cannot be null");
        final ContextRef contextRef = new ContextRef();
        this.contextsMap.put(contextRef, context);
        return contextRef;
    }

    @Override
    public void deleteContext(ContextRef contextRef) {
        LOG.info("Delete context; " + contextRef);
        checkNotNull(contextRef, "context reference cannot be null");
        this.contextsMap.remove(contextRef);
    }

    @Override
    public void updateSettings(ContextRef contextRef, String[] args) throws InvalidOptionsException {
        checkNotNull(args, "arguments cannot be null");
        LOG.info("Update settings; " + Arrays.toString(args));

        try {
            _updateSettings(contextRef, new OptionsParser().parse(args));
        } catch (ParseException e) {
            final String msg = e.getMessage();
            throw new InvalidOptionsException(msg);
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        }
    }

    @Override
    public void updateSettings(ContextRef contextRef, OptionsProvider provider) throws InvalidOptionsException {
        checkNotNull(provider, "provider cannot be null");
        LOG.info("Update settings with provided options");
        _updateSettings(contextRef, provider.getOptions());
    }

    private void _updateSettings(ContextRef contextRef, OptionsHolder options) throws InvalidOptionsException {
        try {
            final Optional<String> error = options.validate(this.isStandalone);
            if (error.isPresent()) {
                throw new InvalidOptionsException(error.get());
            }
            final FrontendContext context = getContext(contextRef);
            context.updateOptions(options);
            context.setABI(loadABI(options));
        } catch (ABILoadFailureException e) {
            throw new RuntimeException("cannot load the ABI", e);
        }
    }

    @Override
    public ProjectData build(ContextRef contextRef) {
        checkNotNull(contextRef, "context reference cannot be null");
        LOG.info("Rebuild; contextRef=" + contextRef);

        final FrontendContext context = getContext(contextRef).basicCopy();
        setContext(context, contextRef);

        final ProjectData.Builder projectDataBuilder;
        final Optional<String> startFile = getStartFile(context);

        final List<FileData> defaultIncludeFilesData = parseFilesIncludedByDefault(context);
        context.setWasInitialBuild(true);

        if (startFile.isPresent()) {
            projectDataBuilder = _update(contextRef, startFile.get(), true);
            projectDataBuilder.addFileDatas(defaultIncludeFilesData);
        } else {
            final String msg = format("Cannot find main configuration '%s'", context.getOptions().getEntryEntityPath());
            LOG.error(msg);
            final NescError error = new NescError(Optional.<Location>absent(), Optional.<Location>absent(), msg);
            projectDataBuilder = ProjectData.builder()
                    .addIssue(error);
        }
        return projectDataBuilder.addIssues(context.getIssues()).build();
    }

    @Override
    public ProjectData update(ContextRef contextRef, String filePath) {
        checkNotNull(contextRef, "context reference cannot be null");
        checkNotNull(filePath, "file path cannot be null");
        final FrontendContext context = getContext(contextRef);

        if (context.wasInitialBuild()) {
            return _update(contextRef, filePath, false).build();
        } else {
            final ProjectData projectData = build(contextRef);
            context.setWasInitialBuild(true);
            final OptionsHolder options = context.getOptions();
            return ProjectData.builder()
                    .addFileDatas(projectData.getFileDatas().values())
                    .addRootFileData(projectData.getFileDatas().get(filePath))
                    .addIssues(projectData.getIssues())
                    .nameMangler(context.getNameMangler())
                    .schedulerSpecification(context.getSchedulerSpecification().orNull())
                    .addDefaultIncludeFiles(context.getDefaultIncludeFiles())
                    .outputFile(options.getOutputFile().or(DEFAULT_OUTPUT_FILE))
                    .abi(context.getABI())
                    .externalVariables(options.getExternalVariables())
                    .externalVariablesFile(options.getExternalVariablesFile().orNull())
                    .optimizeAtomic(options.getOptimizeAtomic())
                    .optimizeTasks(options.getOptimizeTasks())
                    .atomicSpecification(AtomicSpecification.DEFAULT_SPECIFICATION)
                    .build();
        }
    }

    private ProjectData.Builder _update(ContextRef contextRef, String filePath,
            boolean loadScheduler) {
        LOG.info("Update; contextRef=" + contextRef + "; filePath=" + filePath);

        final FrontendContext context = getContext(contextRef);
        clearDirtyFileCache(context, filePath);

        try {
            // FIXME: what if we would like to edit file included by default?
            List<FileCache> fileCacheList = new LoadExecutor(context).parse(filePath, false);
            final List<FileData> fileDatas = createRootFileDataList(fileCacheList);
            final OptionsHolder options = context.getOptions();
            final ProjectData.Builder result  = ProjectData.builder()
                    .addFileDatas(fileDatas)
                    .addRootFileData(findRootFileData(fileDatas, filePath))
                    .nameMangler(context.getNameMangler())
                    .schedulerSpecification(context.getSchedulerSpecification().orNull())
                    .addDefaultIncludeFiles(context.getDefaultIncludeFiles())
                    .outputFile(options.getOutputFile().or(DEFAULT_OUTPUT_FILE))
                    .abi(context.getABI())
                    .externalVariables(options.getExternalVariables())
                    .externalVariablesFile(options.getExternalVariablesFile().orNull())
                    .optimizeAtomic(options.getOptimizeAtomic())
                    .optimizeTasks(options.getOptimizeTasks())
                    .atomicSpecification(AtomicSpecification.DEFAULT_SPECIFICATION);

            if (context.getSchedulerSpecification().isPresent()) {
                if (loadScheduler) {
                    loadScheduler(context.getSchedulerSpecification().get(), fileDatas, context, result);
                }
                result.addIssues(checkScheduler(context.getSchedulerSpecification().get(), fileDatas));
            }

            return result;
        } catch (IOException e) {
            // TODO
            e.printStackTrace();
        }
        return ProjectData.builder();
    }

    private Optional<String> getStartFile(FrontendContext context) {
        final String entryFilePath = context.getOptions().getEntryEntityPath();
        return context.getPathsResolver().getEntryPointPath(entryFilePath);
    }

    private FrontendContext getContext(ContextRef contextRef) {
        final FrontendContext context = this.contextsMap.get(contextRef);
        if (context == null) {
            throw new IllegalArgumentException("unknown context reference");
        }
        return context;
    }

    private void setContext(FrontendContext context, ContextRef contextRef) {
        this.contextsMap.put(contextRef, context);
    }

    /**
     * Creates context from given options.
     *
     * @param args unstructured options
     * @return context
     * @throws InvalidOptionsException thrown when context options cannot be
     *                                 parsed successfully
     */
    private FrontendContext createContextFromOptions(String[] args) throws InvalidOptionsException {
        final OptionsParser optionsParser = new OptionsParser();
        try {
            return createContextFromOptions(optionsParser.parse(args),
                    optionsParser, args.length);
        } catch (ParseException e) {
            reactToOptionsErrors(e.getMessage(), optionsParser, args.length);
            return null;
        } catch (IOException e) {
            if (this.isStandalone) {
                e.printStackTrace();
                System.exit(1);
                return null;
            } else {
                final String msg = "Cannot find options.properties file.";
                throw new IllegalStateException(msg);
            }
        }
    }

    private FrontendContext createContextFromOptions(OptionsHolder options,
                OptionsHelpPrinter helpPrinter, int paramsCount) throws InvalidOptionsException {
        try {
            final Optional<String> error = options.validate(this.isStandalone);
            if (error.isPresent()) {
                reactToOptionsErrors(error.get(), helpPrinter, paramsCount);
            }
            return new FrontendContext(options, this.isStandalone,
                    this.targetAttributes0, this.targetAttributes1,
                    loadABI(options));
        } catch (ABILoadFailureException e) {
            if (this.isStandalone) {
                System.out.println("error: " + e.getMessage());
                System.exit(1);
                return null;
            } else {
                throw new RuntimeException("cannot load the ABI", e);
            }
        }
    }

    private void reactToOptionsErrors(String error, OptionsHelpPrinter helpPrinter,
                int paramsCount) throws InvalidOptionsException {
        if (this.isStandalone) {
            if (paramsCount == 0) {
                helpPrinter.printHelpWithError(error);
            } else {
                helpPrinter.printError(error);
            }
            System.exit(1);
        } else {
            throw new InvalidOptionsException(error);
        }
    }

    private List<FileData> parseFilesIncludedByDefault(FrontendContext context) {
        final List<FileData> result = new ArrayList<>();
        final List<String> defaultIncludes = context.getDefaultIncludeFiles();

        context.resetDefaultMacros();
        context.resetDefaultSymbols();

        for (String filePath : defaultIncludes) {
            try {
                final List<FileCache> fileCacheList = new LoadExecutor(context).parse(filePath, true);
                for (FileCache cache : fileCacheList) {
                    if (cache.isRoot()) {
                        result.add(new FileData(cache));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                LOG.error("Error during parsing default files.", e);
                final String msg = format("Could not find file included by default; path = %s.", filePath);
                final NescError error = new NescError(Optional.<Location>absent(), Optional.<Location>absent(), msg);
                context.getIssues().add(error);
            }
        }
        return result;
    }

    private void clearDirtyFileCache(FrontendContext context, String modifiedFile) {
        final FilesGraph filesGraph = context.getFilesGraph();
        final GraphFile file = filesGraph.getFile(modifiedFile);
        if (file == null) {
            return;
        }
        final DirtyFileVisitor visitor = new DirtyFileVisitor(filesGraph.getFile(modifiedFile), false);
        visitor.start();
        final Set<GraphFile> dirtyFiles = visitor.getDirtyFiles();
        for (GraphFile graphFile : dirtyFiles) {
            context.getCache().remove(graphFile.getFilePath());
        }
    }

    /**
     * Checks if the scheduler has been already loaded and if so, does nothing.
     * Otherwise, loads the scheduler. The given list is modified to contain the
     * data of new files required by the scheduler and so is the given project
     * data builder.
     *
     * @param schedulerSpec Specification of the scheduler to load.
     * @param fileDatas List with data of files that constitute the whole
     *                  project. It is modified, if the scheduler requires some
     *                  new files.
     */
    private void loadScheduler(SchedulerSpecification schedulerSpec, List<FileData> fileDatas,
            FrontendContext context, ProjectData.Builder builder) {

        if (getSchedulerFileData(schedulerSpec.getComponentName(), fileDatas).isPresent()) {
            return;
        }

        // Get the path of the scheduler
        final Optional<String> optPath = context.getPathsResolver().getEntityFile(schedulerSpec.getComponentName());
        if (!optPath.isPresent()) {
            final String errorMsg = format("Cannot find scheduler component '%s'",
                    schedulerSpec.getComponentName());
            LOG.error(errorMsg);
            final NescError error = new NescError(Optional.<Location>absent(),
                    Optional.<Location>absent(), errorMsg);
            builder.addIssue(error);
            return;
        }

        // Load the scheduler
        try {
            final String schedulerFilePath = optPath.get();
            final List<FileCache> newCaches = new LoadExecutor(context).parse(schedulerFilePath, false);
            final List<FileData> newFileDatas = createRootFileDataList(newCaches);
            fileDatas.addAll(newFileDatas);
            builder.addFileDatas(newFileDatas);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check the correctness of the scheduler from the given file data. If it is
     * present, then a list with errors is returned.
     *
     * @param fileData List with data of files that constitute the whole project
     *                 after loading the scheduler.
     * @return List with issues found in the scheduler and task interface.
     */
    private List<NescIssue> checkScheduler(SchedulerSpecification specification, List<FileData> fileData) {
        final Optional<FileData> optSchedulerFileData = getSchedulerFileData(
                specification.getComponentName(), fileData);

        if (!optSchedulerFileData.isPresent() || !optSchedulerFileData.get().getIssues().isEmpty()
                || !optSchedulerFileData.get().getEntityRoot().isPresent()) {
            return new ArrayList<>();
        }

        final SchedulerAnalyzer analyzer = new SchedulerAnalyzer(specification);
        analyzer.analyzeScheduler(optSchedulerFileData.get().getEntityRoot().get());

        // Look for and check the task interface

        for (FileData data : fileData) {
            if (data.getEntityRoot().isPresent() && data.getEntityRoot().get() instanceof Interface) {
                final Interface iface = (Interface) data.getEntityRoot().get();

                if (iface.getName().getName().equals(specification.getTaskInterfaceName())) {
                    if (data.getIssues().isEmpty()) {
                        analyzer.analyzeTaskInterface(iface);
                    }
                    break;
                }
            }
        }

        // Create the issues list

        final List<Issue> issues = analyzer.getIssues();
        final List<NescIssue> nescIssues = new ArrayList<>();

        for (Issue issue : issues) {
            switch (issue.getKind()) {
                case ERROR:
                    nescIssues.add(new NescError(Optional.<Location>absent(), Optional.<Location>absent(),
                            Optional.of(issue.getCode()), issue.generateDescription()));
                    break;
                case WARNING:
                    nescIssues.add(new NescWarning(Optional.<Location>absent(), Optional.<Location>absent(),
                            Optional.of(issue.getCode()), issue.generateDescription()));
                    break;
                default:
                    throw new RuntimeException("unexpected issue type");
            }
            LOG.error(issue.generateDescription());
        }

        return nescIssues;
    }

    private Optional<FileData> getSchedulerFileData(String schedulerComponentName,
            List<FileData> fileData) {

        final String expectedFileName = schedulerComponentName + ".nc";

        // Look for the file with the scheduler component
        for (FileData data : fileData) {
            final String onlyFileName = Paths.get(data.getFilePath()).getFileName().toString();
            if (onlyFileName.equals(expectedFileName)) {
                return Optional.of(data);
            }
        }

        return Optional.absent();
    }

    private List<FileData> createRootFileDataList(List<FileCache> fileCaches) {
        final List<FileData> fileDatas = new ArrayList<>();

        for (FileCache cache : fileCaches) {
            if (cache.isRoot()) {
                fileDatas.add(new FileData(cache));
            }
        }

        return fileDatas;
    }

    private FileData findRootFileData(List<FileData> fileDatas, String rootFilePath) {
        for (FileData fileData : fileDatas) {
            if (fileData.getFilePath().equals(rootFilePath)) {
                return fileData;
            }
        }

        throw new IllegalArgumentException("the list does not contain the root file data");
    }

    private ABI loadABI(OptionsHolder options) throws ABILoadFailureException {
        checkArgument(options.getABIFilename() == null || options.getABIPlatformName() == null,
                "cannot specify both ABI filename and ABI platform in options");

        try {
            if (options.getABIPlatformName() != null) {
                return loadABIPlatform(options.getABIPlatformName());
            } else if (options.getABIFilename() != null) {
                return loadABIFile(options.getABIFilename());
            } else {
                return loadABIPlatform(defaultABIPlatform);
            }
        } catch (SAXException e) {
            throw new ABILoadFailureException("invalid ABI XML data: " + e.getMessage(), e);
        } catch (ParserConfigurationException | XPathExpressionException e) {
            throw new ABILoadFailureException("internal XML processing error", e);
        } catch (IOException e) {
            throw new ABILoadFailureException("cannot read the ABI due to I/O error", e);
        }
    }

    private ABI loadABIPlatform(String platformName) throws ABILoadFailureException, SAXException,
                ParserConfigurationException, IOException, XPathExpressionException {
        final String xmlFilename = format("%s/%sabi.xml", DIR_PREDEFINED_ABI, platformName);
        final InputStream abiInput = getClass().getClassLoader().getResourceAsStream(xmlFilename);

        if (abiInput == null) {
            throw new ABILoadFailureException("cannot find ABI platform '" + platformName + "'");
        }

        return new ABI(abiInput);
    }

    private ABI loadABIFile(String fileName) throws ABILoadFailureException, SAXException,
                ParserConfigurationException, IOException, XPathExpressionException {
        try {
            return new ABI(fileName);
        } catch (FileNotFoundException e) {
            throw new ABILoadFailureException("file '" + fileName + "' does not exist");
        }
    }

    private static class DirtyFileVisitor extends DefaultFileGraphVisitor {

        private final Set<GraphFile> dirtyFiles;

        public DirtyFileVisitor(GraphFile startNode, boolean outgoing) {
            super(startNode, outgoing);
            this.dirtyFiles = new HashSet<>();
        }

        public Set<GraphFile> getDirtyFiles() {
            return dirtyFiles;
        }

        @Override
        public void start() {
            super.start();
            dirtyFiles.remove(startNode);
        }

        @Override
        protected boolean action(GraphFile file) {
            dirtyFiles.add(file);
            return file.getNesCFileType() == NesCFileType.NONE;
        }
    }

    /**
     * Nesc frontend builder.
     */
    public static final class Builder {

        private boolean isStandalone;
        private final ImmutableSet.Builder<String> targetAttributes0Builder;
        private final ImmutableSet.Builder<String> targetAttributes1Builder;
        private String defaultABIPlatform;

        public Builder() {
            this.isStandalone = false;
            this.targetAttributes0Builder = ImmutableSet.builder();
            this.targetAttributes1Builder = ImmutableSet.builder();
            this.defaultABIPlatform = "msp430";
        }

        /**
         * Sets whether the frontend is standalone or used as a library.
         * By default is not standalone.
         *
         * @param standalone is standalone
         * @return builder
         */
        public Builder standalone(boolean standalone) {
            this.isStandalone = standalone;
            return this;
        }

        /**
         * <p>Adds a new target attribute recognized by the frontend. The given
         * string is the name of the attribute. It will be a new language
         * keyword that will be represented as
         * a {@link pl.edu.mimuw.nesc.ast.gen.TargetAttribute} node. In a NesC
         * program such keyword will be able to appear wherever an attribute
         * can.</p>
         * <p>This method adds a no arguments target attribute.</p>
         *
         * @param attributeKeyword Attribute keyword to add.
         * @return <code>this</code>
         */
        public Builder addTargetAttribute0(String attributeKeyword) {
            this.targetAttributes0Builder.add(attributeKeyword);
            return this;
        }

        /**
         * <p>Adds several target attributes that will be recognized by the
         * frontend.</p>
         * <p>This method adds no arguments target attributes.</p>
         *
         * @param attributesKeywords Iterable with names of target attributes
         *                           to add.
         * @return <code>this</code>
         * @see Builder#addTargetAttribute0
         */
        public Builder addTargetAttributes0(Iterable<String> attributesKeywords) {
            this.targetAttributes0Builder.addAll(attributesKeywords);
            return this;
        }

        /**
         * <p>Add a target attribute that will take exactly one parameter.</p>
         *
         * @param attributeKeyword Name of the target attribute to add.
         * @return <code>this</code>
         */
        public Builder addTargetAttribute1(String attributeKeyword) {
            this.targetAttributes1Builder.add(attributeKeyword);
            return this;
        }

        /**
         * <p>Add several target attributes that take exactly one parameter.</p>
         *
         * @param attributesKeywords Iterable with names of target attributes to
         *                           add.
         * @return <code>this</code>
         */
        public Builder addTargetAttributes1(Iterable<String> attributesKeywords) {
            this.targetAttributes1Builder.addAll(attributesKeywords);
            return this;
        }

        /**
         * <p>Set the ABI platform to assume during compilation if no ABI file
         * or ABI platform is specified by parameters.</p>
         *
         * @param platformName Name of the ABI platform to set.
         * @return <code>this</code>
         */
        public Builder defaultABIPlatform(String platformName) {
            checkNotNull(platformName, "name of the platform cannot be null");
            checkArgument(!platformName.isEmpty(), "name of the platform cannot be an empty string");
            this.defaultABIPlatform = platformName;
            return this;
        }

        public NescFrontend build() {
            verify();
            return new NescFrontend(this);
        }

        private void verify() {
        }
    }
}
