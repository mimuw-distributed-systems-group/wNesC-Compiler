package pl.edu.mimuw.nesc;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.environment.NescEntityEnvironment;
import pl.edu.mimuw.nesc.environment.TranslationUnitEnvironment;
import pl.edu.mimuw.nesc.filesgraph.FilesGraph;
import pl.edu.mimuw.nesc.load.FileCache;
import pl.edu.mimuw.nesc.load.MacroManager;
import pl.edu.mimuw.nesc.load.PathsResolver;
import pl.edu.mimuw.nesc.names.mangling.AlphabeticNameMangler;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;
import pl.edu.mimuw.nesc.option.OptionsHolder;
import pl.edu.mimuw.nesc.common.SchedulerSpecification;
import pl.edu.mimuw.nesc.preprocessor.PreprocessorMacro;
import pl.edu.mimuw.nesc.problem.NescIssue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class FrontendContext {

    private OptionsHolder options;
    private Map<String, String> predefinedMacros;
    private List<String> defaultIncludeFiles;
    private PathsResolver pathsResolver;
    private Optional<SchedulerSpecification> scheduler;
    private SetMultimap<Optional<String>, String> externalVariables;

    private final boolean isStandalone;
    private final ImmutableSet<String> targetAttributes0;
    private final ImmutableSet<String> targetAttributes1;
    private final MacroManager macroManager;

    private final FilesGraph filesGraph;
    private final Map<String, FileCache> cache;
    private final NameMangler nameMangler;

    /**
     * Each file may contain at most one NesC entity. This map keeps the
     * mapping between the file's name and the NesC entity in the file.
     */
    private final Map<String, String> fileToComponent;
    /**
     * Namespace for NesC components and interfaces is present only in
     * global scope.
     */
    private final NescEntityEnvironment nescEntityEnvironment;
    /**
     * When the frontend is used as a part of plug-in, each files has its
     * own global environment.
     */
    private final Map<String, TranslationUnitEnvironment> environments;

    private final List<NescIssue> issues;

    /**
     * Macros from files included by default. Once parsed, are served for
     * each file.
     */
    private Map<String, PreprocessorMacro> defaultMacros;
    /**
     * Symbols from files included by default.
     */
    private TranslationUnitEnvironment defaultSymbols;

    /**
     * In a standalone mode a single environment is used for the entire
     * application.
     * In a plugin mode a single environment is used for a single file.
     * However, the environment is shared with included header files.
     */
    private TranslationUnitEnvironment environment;

    private boolean wasInitialBuild;

    /**
     * ABI that will be assumed by the frontend.
     */
    private ABI abi;

    public FrontendContext(OptionsHolder options, boolean isStandalone,
            ImmutableSet<String> targetAttributes0, ImmutableSet<String> targetAttributes1,
            ABI abi) {
        this.isStandalone = isStandalone;
        this.targetAttributes0 = targetAttributes0;
        this.targetAttributes1 = targetAttributes1;
        this.options = options;
        this.predefinedMacros = options.getPredefinedMacros();
        this.defaultIncludeFiles = options.getDefaultIncludeFiles();

        this.pathsResolver = getPathsResolver(options);

        this.macroManager = new MacroManager();

        this.nameMangler = new AlphabeticNameMangler();
        this.fileToComponent = new HashMap<>();
        this.filesGraph = new FilesGraph();
        this.cache = new HashMap<>();
        this.scheduler = options.getSchedulerSpecification();
        this.externalVariables = options.getExternalVariables();
        this.nescEntityEnvironment = new NescEntityEnvironment();
        this.environments = new HashMap<>();
        this.environment = new TranslationUnitEnvironment();
        this.environment.addConstantFunctions();

        this.issues = new ArrayList<>();

        this.defaultMacros = new HashMap<>();
        this.defaultSymbols = new TranslationUnitEnvironment();
        this.wasInitialBuild = false;
        this.abi = abi;
    }

    public boolean isStandalone() {
        return isStandalone;
    }

    public ImmutableSet<String> getTargetAttributes0() {
        return targetAttributes0;
    }

    public ImmutableSet<String> getTargetAttributes1() {
        return targetAttributes1;
    }

    public OptionsHolder getOptions() {
        return options;
    }

    public PathsResolver getPathsResolver() {
        return pathsResolver;
    }

    public MacroManager getMacroManager() {
        return macroManager;
    }

    public NameMangler getNameMangler() {
        return nameMangler;
    }

    public Map<String, String> getFileToComponent() {
        return fileToComponent;
    }

    public FilesGraph getFilesGraph() {
        return filesGraph;
    }

    public Map<String, FileCache> getCache() {
        return cache;
    }

    public Optional<SchedulerSpecification> getSchedulerSpecification() {
        return scheduler;
    }

    public SetMultimap<Optional<String>, String> getExternalVariables() {
        return externalVariables;
    }

    public Map<String, String> getPredefinedMacros() {
        return predefinedMacros;
    }

    public List<String> getDefaultIncludeFiles() {
        return defaultIncludeFiles;
    }

    public NescEntityEnvironment getNescEntityEnvironment() {
        return nescEntityEnvironment;
    }

    public Map<String, TranslationUnitEnvironment> getEnvironments() {
        return environments;
    }

    public TranslationUnitEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(TranslationUnitEnvironment environment) {
        this.environment = environment;
    }

    public List<NescIssue> getIssues() {
        return issues;
    }

    public Map<String, PreprocessorMacro> getDefaultMacros() {
        return defaultMacros;
    }

    public void resetDefaultMacros() {
        this.defaultMacros = new HashMap<>();
    }

    public TranslationUnitEnvironment getDefaultSymbols() {
        return defaultSymbols;
    }

    public void resetDefaultSymbols() {
        this.defaultSymbols = new TranslationUnitEnvironment();
    }

    public boolean wasInitialBuild() {
        return wasInitialBuild;
    }

    public void setWasInitialBuild(boolean wasInitialBuild) {
        this.wasInitialBuild = wasInitialBuild;
    }

    public ABI getABI() {
        return abi;
    }

    public void setABI(ABI abi) {
        this.abi = abi;
    }

    public void updateOptions(OptionsHolder options) {
        this.options = options;
        this.pathsResolver = getPathsResolver(options);
        this.predefinedMacros = options.getPredefinedMacros();
        this.defaultIncludeFiles = options.getDefaultIncludeFiles();
        this.scheduler = options.getSchedulerSpecification();
        this.externalVariables = options.getExternalVariables();
    }

    /**
     * Clones only a part of current instance (only options are cloned).
     *
     * @return the new instance of {@link FrontendContext}
     */
    public FrontendContext basicCopy() {
        return new FrontendContext(this.options, this.isStandalone,
                this.targetAttributes0, this.targetAttributes1,
                this.abi);
    }

    private PathsResolver getPathsResolver(OptionsHolder options) {
        return PathsResolver.builder()
                .sourcePaths(options.getSourcePaths())
                .quoteIncludePaths(options.getUserSourcePaths())
                .projectPath(options.getProjectPath())
                .build();
    }
}
