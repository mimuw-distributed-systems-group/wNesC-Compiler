package pl.edu.mimuw.nesc.load;

import com.google.common.base.Optional;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.FrontendContext;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.preprocessor.PreprocessorMacro;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

/**
 * The strategy for load executor in the standalone mode.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
class StandaloneLoadExecutor extends LoadFileExecutor {

    private static final Logger LOG = Logger.getLogger(StandaloneLoadExecutor.class);

    /**
     * Creates parse file executor.
     *
     * @param context         context
     * @param currentFilePath current file path
     */
    public StandaloneLoadExecutor(FrontendContext context, String currentFilePath) {
        super(context, currentFilePath);
    }

    @Override
    protected void setUpEnvironments() {
        super.setUpEnvironments();
        this.environment = context.getEnvironment();
    }

    @Override
    protected void setUpLexer() throws IOException {
        super.setUpLexer();
        lexer.addMacros(context.getMacroManager().getAll().values());
    }

    @Override
    protected void consumeData(FileCache newCache) {
        context.getCache().put(currentFilePath, newCache);
        LOG.trace("Put file cache into context; file: " + currentFilePath);
        fileCacheList.add(newCache);
    }

    @Override
    public boolean beforeInclude(String filePath, int line) {
        final Location visibleFrom = new Location("", line, 0);
        includeDependency(currentFilePath, filePath, visibleFrom);
        /*
         * "private includes" are pasted to the current file, since
         * the definitions and declarations from the included file are
         * an inherent part of the current file's AST structure.
         */
        return !wasExtdefsFinished;
    }

    @Override
    public boolean interfaceDependency(String currentEntityPath, String interfaceName, Location visibleFrom) {
        return nescDependency(currentEntityPath, interfaceName, visibleFrom);
    }

    @Override
    public boolean componentDependency(String currentEntityPath, String componentName, Location visibleFrom) {
        return nescDependency(currentEntityPath, componentName, visibleFrom);
    }

    @Override
    protected void handlePublicMacros(Map<String, PreprocessorMacro> macros) {
        super.handlePublicMacros(macros);
        context.getMacroManager().replace(macros);
        fileCacheBuilder.endFileMacros(context.getMacroManager().getAll());
    }

    @SuppressWarnings("UnusedParameters")
    private void includeDependency(String currentFilePath, String includedFilePath, Location visibleFrom) {
        final boolean wasVisited = wasFileVisited(includedFilePath);
        /* Update files graph. */
        updateFilesGraph(currentFilePath, includedFilePath);
        if (!wasVisited) {
            if (!wasExtdefsFinished) {
                context.getMacroManager().replace(lexer.getMacros());
            }
            fileDependency(includedFilePath, true);
        }
    }

    @SuppressWarnings("UnusedParameters")
    private boolean nescDependency(String currentEntityPath, String dependencyName, Location visibleFrom) {
        final Optional<String> filePathOptional = context.getPathsResolver().getEntityFile(dependencyName);
        if (!filePathOptional.isPresent()) {
            return false;
        }
        // TODO update components graph

        final boolean wasVisited = wasFileVisited(filePathOptional.get());
        /* Update files graph. */
        updateFilesGraph(currentFilePath, filePathOptional.get());
        if (!wasVisited) {
            fileDependency(filePathOptional.get(), false);
        }
        return true;
    }

    /**
     * Resolves dependency upon specified file.
     *
     * @param otherFilePath other file path
     * @param includeMacros indicates whether macros from dependency
     *                      file should be visible
     */
    private void fileDependency(String otherFilePath, boolean includeMacros) {
        final StandaloneLoadExecutor executor = new StandaloneLoadExecutor(context, otherFilePath);
        try {
            final LinkedList<FileCache> datas = executor.parseFile();
            fileCacheList.addAll(datas);
        } catch (IOException e) {
            LOG.error("Unexpected IOException occurred.", e);
        }

        /*
         * In the standalone mode, load only macros and only in
         * the case of included files.
         * Symbols are already in the symbol table.
         */
        if (includeMacros) {
            lexer.replaceMacros(context.getMacroManager().getAll().values());
        }
    }

    private boolean wasFileVisited(String otherFilePath) {
        return context.getFilesGraph().containsFile(otherFilePath);
    }

    /*
     *  Macros handling in the standalone mode.
     *
     * Before parsing the file, load into the lexer all macros from
     * the macroManager.
     *
     * The symbol is shared among all files, therefore it does not require
     * special affection.
     *
     * |
     * |-> #include, reference to Nesc entity
     * |    If external definitions have not yet been finished, get all current
     * |    macros from the lexer and save them in the macrosManager, since they
     * |    need to be visible in the included file. If the #include or the
     * |    reference to a NesC entity appears inside a component or interface
     * |    definition, changes to macros made inside the definition will not be
     * |    visible although changes of macros made in references to other NesC
     * |    entities will be visible.
     * |
     * |<- #include finished
     * |    Get the current macros from the macroManager, and pass them
     * |    to the lexer (but before that clear the lexer's map of macros).
     * |    Some of the macros might be #undefined!
     * |
     * |-> extdefsFinished (in the case of NesC files) or EOF (C files)
     * |    Saves all current macros in the fileCache as "endFileMacros".
     * |
     * |    "private" macros are handled as usual
     * |    "private" #includes are handled as usual
     */
}
