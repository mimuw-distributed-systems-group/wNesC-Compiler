package pl.edu.mimuw.nesc;

import pl.edu.mimuw.nesc.environment.NescEnvironment;
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

    private final OptionsHolder options;
    private final Map<String, String> predefinedMacros;
    private final List<String> defaultIncludeFiles;

    private final PathsResolver pathsResolver;

    private final FilesGraph filesGraph;
    private final Map<String, FileCache> cache;
    private final Map<String, FileData> fileDatas;

    private final NescEnvironment nescEnvironment;

    private List<NescIssue> issues;

    public FrontendContext(OptionsHolder options) {
        this.options = options;
        this.predefinedMacros = options.getPredefinedMacros();
        this.defaultIncludeFiles = options.getDefaultIncludeFiles();

        this.pathsResolver = PathsResolver.builder()
                .sourcePaths(options.getSourcePaths())
                .quoteIncludePaths(options.getUserSourcePaths())
                .projectPath(options.getProjectPath())
                .build();

        this.filesGraph = new FilesGraph();
        this.cache = new HashMap<>();
        this.fileDatas = new HashMap<>();
        this.nescEnvironment = new NescEnvironment();

        this.issues = new ArrayList<>();
    }

    public OptionsHolder getOptions() {
        return options;
    }

    public PathsResolver getPathsResolver() {
        return pathsResolver;
    }

    public FilesGraph getFilesGraph() {
        return filesGraph;
    }

    public Map<String, FileCache> getCache() {
        return cache;
    }

    public Map<String, FileData> getFileDatas() {
        return fileDatas;
    }

    public Map<String, String> getPredefinedMacros() {
        return predefinedMacros;
    }

    public List<String> getDefaultIncludeFiles() {
        return defaultIncludeFiles;
    }

    public NescEnvironment getNescEnvironment() {
        return nescEnvironment;
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
        return new FrontendContext(this.options);
    }
}
