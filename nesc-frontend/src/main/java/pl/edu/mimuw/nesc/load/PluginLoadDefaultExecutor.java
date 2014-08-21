package pl.edu.mimuw.nesc.load;

import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.FileData;
import pl.edu.mimuw.nesc.FrontendContext;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.preprocessor.PreprocessorMacro;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * The strategy for load executor for default files in the plug-in mode.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
class PluginLoadDefaultExecutor extends LoadFileExecutor {

    private static final Logger LOG = Logger.getLogger(PluginLoadDefaultExecutor.class);

    /**
     * Files which cached data was already used in the parsing process.
     * Each file should be "visited" at most once.
     */
    protected final Set<String> visitedFiles;

    /**
     * Creates parse file executor.
     *
     * @param context         context
     * @param currentFilePath current file path
     */
    public PluginLoadDefaultExecutor(FrontendContext context, String currentFilePath) {
        super(context, currentFilePath, true);
        this.visitedFiles = new HashSet<>();
    }

    @Override
    protected void setUpEnvironments() {
        super.setUpEnvironments();
        this.environment = context.getDefaultSymbols();
    }

    @Override
    protected void setUpLexer() throws IOException {
        super.setUpLexer();
        /* Files included by default uses default macros map instead
         * of macrosManager all the time. After all default files are
         * parsed, the map will contain all default macros. */
        lexer.addMacros(context.getDefaultMacros().values());
    }

    @Override
    protected void handlePublicMacros(Map<String, PreprocessorMacro> macros) {
        super.handlePublicMacros(macros);
        context.getDefaultMacros().clear();
        context.getDefaultMacros().putAll(macros);
    }

    @Override
    public boolean beforeInclude(String filePath, int line) {
        final Location visibleFrom = new Location(currentFilePath, line, 0);
        includeDependency(currentFilePath, filePath, visibleFrom);
        /* Never include body of files, they should be parsed separately. */
        return true;
    }

    @Override
    public boolean interfaceDependency(String currentEntityPath, String interfaceName, Location visibleFrom) {
        return nescDependency(currentEntityPath, interfaceName, visibleFrom, true);
    }

    @Override
    public boolean componentDependency(String currentEntityPath, String componentName, Location visibleFrom) {
        return nescDependency(currentEntityPath, componentName, visibleFrom, false);
    }

    private boolean nescDependency(String currentEntityPath, String dependencyName, Location visibleFrom,
                                   boolean collectData) {
        throw new IllegalStateException("NesC files should not be included by default.");
    }

    private void includeDependency(String currentFilePath, String includedFilePath, Location visibleFrom) {
        final boolean wasVisited = wasFileVisited(includedFilePath);
        /* Update files graph. */
        updateFilesGraph(currentFilePath, includedFilePath);

        if (!wasVisited) {
            visitedFiles.add(includedFilePath);
            /* Saves current macros in the context.
             * Symbols are already shared (in the shared table). */
            context.getDefaultMacros().clear();
            context.getDefaultMacros().putAll(lexer.getMacros());
            /* Load dependency. */
            fileDependency(includedFilePath);
            /* Replace macros. */
            lexer.getMacros().clear();
            lexer.addMacros(context.getDefaultMacros().values());
        }
    }

    private void fileDependency(String otherFilePath) {
        final PluginLoadDefaultExecutor executor = new PluginLoadDefaultExecutor(context, otherFilePath);
        try {
            final LinkedList<FileData> datas = executor.parseFile();
            fileDatas.addAll(datas);
        } catch (IOException e) {
            LOG.error("Unexpected IOException occurred.", e);
        }
    }

    private boolean wasFileVisited(String otherFilePath) {
        return visitedFiles.contains(otherFilePath);
    }
}
