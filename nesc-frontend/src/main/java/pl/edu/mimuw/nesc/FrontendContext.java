package pl.edu.mimuw.nesc;

import pl.edu.mimuw.nesc.environment.NescEntityEnvironment;
import pl.edu.mimuw.nesc.environment.TranslationUnitEnvironment;
import pl.edu.mimuw.nesc.filesgraph.FilesGraph;
import pl.edu.mimuw.nesc.option.OptionsHolder;
import pl.edu.mimuw.nesc.problem.NescIssue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class FrontendContext {

    private final boolean isStandalone;
    private final OptionsHolder options;
    private final Map<String, String> predefinedMacros;
    private final List<String> defaultIncludeFiles;

    private final PathsResolver pathsResolver;
    private final MacroManager macroManager;

    private final FilesGraph filesGraph;
    private final Map<String, FileCache> cache;

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
    /**
     * In a standalone mode a single environment is used for the entire
     * application.
     */
    private final TranslationUnitEnvironment environment;

    private List<NescIssue> issues;

    public FrontendContext(OptionsHolder options, boolean isStandalone) {
        this.isStandalone = isStandalone;
        this.options = options;
        this.predefinedMacros = options.getPredefinedMacros();
        this.defaultIncludeFiles = options.getDefaultIncludeFiles();

        this.pathsResolver = PathsResolver.builder()
                .sourcePaths(options.getSourcePaths())
                .quoteIncludePaths(options.getUserSourcePaths())
                .projectPath(options.getProjectPath())
                .build();

        this.macroManager = new MacroManager();

        this.filesGraph = new FilesGraph();
        this.cache = new HashMap<>();
        this.nescEntityEnvironment = new NescEntityEnvironment();
        this.environments = new HashMap<>();
        this.environment = new TranslationUnitEnvironment();

        this.issues = new ArrayList<>();
    }

    public boolean isStandalone() {
        return isStandalone;
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

    public FilesGraph getFilesGraph() {
        return filesGraph;
    }

    public Map<String, FileCache> getCache() {
        return cache;
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

    public List<NescIssue> getIssues() {
        return issues;
    }

    public void setIssues(List<NescIssue> issues) {
        this.issues = issues;
    }

    /**
     * Clones only a part of current instance (only options are cloned).
     *
     * @return the new instance of {@link FrontendContext}
     */
    public FrontendContext basicCopy() {
        return new FrontendContext(this.options, this.isStandalone);
    }
}
