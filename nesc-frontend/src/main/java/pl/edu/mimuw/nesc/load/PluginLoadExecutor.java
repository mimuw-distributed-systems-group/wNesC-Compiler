package pl.edu.mimuw.nesc.load;

import com.google.common.base.Optional;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.FileData;
import pl.edu.mimuw.nesc.FrontendContext;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.common.FileType;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.TagDeclaration;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.environment.TranslationUnitEnvironment;
import pl.edu.mimuw.nesc.preprocessor.PreprocessorMacro;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

/**
 * The strategy for the load executor in the plug-in mode.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
class PluginLoadExecutor extends LoadFileExecutor {

    private static final Logger LOG = Logger.getLogger(PluginLoadExecutor.class);

    /**
     * Indicates whether the file has its own global table. If file is not a
     * root file, it inherits the symbol table (and macros) from its parent.
     * All NesC files are root files. Only header files included directly from
     * NesC files are root files. Currently edited file is always a root file
     * (even header files).
     * This is because the header files and its inclusions are not independent
     * and must be parsed as a single file (in fact, it is a single file, since
     * include = paste ;)). When a NesC file includes a header file, we can
     * assume that the header file is independent from the NesC file.
     */
    private final boolean isRoot;
    /**
     * Files which cached data was already used in the parsing process.
     * Each file should be "visited" at most once.
     */
    private final Set<String> visitedFiles;

    /**
     * Creates parse file executor.
     *
     * @param context         context
     * @param currentFilePath current file path
     * @param isRoot          indicates if the file is a root for
     *                        the update command
     */
    public PluginLoadExecutor(FrontendContext context, String currentFilePath, boolean isRoot) {
        super(context, currentFilePath, false);
        this.isRoot = isRoot;
        this.visitedFiles = new HashSet<>();
    }

    @Override
    protected void setUpEnvironments() {
        super.setUpEnvironments();
        if (isRoot) {
            this.environment = new TranslationUnitEnvironment();
            collectParsedData(context.getDefaultSymbols(), false);
            context.setEnvironment(environment);
        } else {
            this.environment = context.getEnvironment();
        }
    }

    @Override
    protected void setUpLexer() throws IOException {
        super.setUpLexer();
        if (isRoot) {
            lexer.addMacros(context.getDefaultMacros().values());
        } else {
            lexer.addMacros(context.getMacroManager().getAll().values());
        }
    }

    @Override
    protected void addData(FileData fileData) {
        super.addData(fileData);
    }

    @Override
    protected void handlePublicMacros(Map<String, PreprocessorMacro> macros) {
        super.handlePublicMacros(macros);
        context.getMacroManager().replace(macros);
    }

    @Override
    public boolean beforeInclude(String filePath, int line) {
        final Location visibleFrom = new Location(currentFilePath, line, 0);
        includeDependency(currentFilePath, filePath, visibleFrom);
        /* Never include body of files, they should be parsed separately. */
        /* FIXME: "private inclusions" ? */
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

    @SuppressWarnings("UnusedParameters")
    private boolean nescDependency(String currentEntityPath, String dependencyName, Location visibleFrom,
                                   boolean collectData) {
        final Optional<String> filePathOptional = context.getPathsResolver().getEntityFile(dependencyName);
        if (!filePathOptional.isPresent()) {
            return false;
        }
        final String otherFilePath = filePathOptional.get();
        /* Indicates whether there is an entry in cache for the dependency. */
        final boolean wasParsed = context.getCache().containsKey(otherFilePath);
        /* Indicates whether symbols and macros of the dependency has not been
         * loaded yet into structures of current file. */
        final boolean wasVisited = wasFileVisited(otherFilePath);
        updateFilesGraph(currentFilePath, otherFilePath);

        if (!wasVisited) {
            visitedFiles.add(otherFilePath);
            if (!wasParsed) {
                /* We need to persist our environment, since the dependency
                 * was not be loaded yet and will overwrite current environment
                 * in the context. */
                final TranslationUnitEnvironment currentEnv = context.getEnvironment();
                context.setEnvironment(null);
                fileDependency(otherFilePath, true);
                context.setEnvironment(currentEnv);
            }
            /* Only symbols are collected, macros are not.
             * Symbols are collected only from interfaces. */
            if (collectData) {
                collectParsedData(otherFilePath, false);
            }
        }
        // TODO update components graph
        return true;
    }

    private void collectParsedData(String otherFilePath, boolean areLoadable) {
        final Environment otherEnvironment = context.getCache().get(otherFilePath).getEnvironment();
        collectParsedData(otherEnvironment, areLoadable);
    }

    /**
     * Copies "loadable" symbols from one environment to another one.
     *
     * @param otherEnvironment from where copy the symbols
     * @param areLoadable      indicates whether the symbols should be marked
     *                         as loadable. Loadable symbols are symbols which
     *                         are defined in that file or in one of the
     *                         explicitly included files (not included by
     *                         default, to avoid redeclaration error for
     *                         symbols from default files which present in each
     *                         symbol table from the very beginning).
     */
    private void collectParsedData(Environment otherEnvironment, boolean areLoadable) {
        for (Map.Entry<String, ObjectDeclaration> entry : otherEnvironment.getObjects().getAllFromFile()) {
            environment.getObjects().add(entry.getKey(), entry.getValue(), areLoadable);
        }
        for (Map.Entry<String, TagDeclaration> entry : otherEnvironment.getTags().getAllFromFile()) {
            environment.getTags().add(entry.getKey(), entry.getValue(), areLoadable);
        }
    }


    @SuppressWarnings("UnusedParameters")
    private void includeDependency(String currentFilePath, String includedFilePath, Location visibleFrom) {
        final boolean isNescFile = fileType == FileType.NESC;
        /* Indicates whether symbols and macros of the dependency has not been
         * loaded yet into structures of current file. */
        final boolean wasVisited;
        /* Indicates whether there is an entry in cache for the dependency. */
        final boolean wasParsed;

        if (isNescFile) {
            /* For NesC files try to use cached data. */
            wasParsed = context.getCache().containsKey(includedFilePath);
        } else {
            /* Header files do not use cached data. Dependencies must always
             * be reparsed. */
            wasParsed = false;
        }
        wasVisited = wasFileVisited(includedFilePath);
        updateFilesGraph(currentFilePath, includedFilePath);

        if (isNescFile && !wasVisited) {
            visitedFiles.add(includedFilePath);
            if (!wasParsed) {
                /* We need to persist our environment, since the dependency
                 * was not be loaded yet and will overwrite current environment
                 * in the context. */
                final TranslationUnitEnvironment currentEnv = context.getEnvironment();
                context.setEnvironment(null);
                fileDependency(includedFilePath, true);
                context.setEnvironment(currentEnv);
            }
            collectParsedData(includedFilePath, true);
            lexer.getMacros().clear();
            lexer.addMacros(context.getMacroManager().getAll().values());
        } else if (!isNescFile && !wasVisited) {
            visitedFiles.add(includedFilePath);
            /* We need to pass macros to the dependency.
             * We do not need to pass symbols, since the symbol table
             * is shared with the dependency. */
            context.getMacroManager().replace(lexer.getMacros());
            fileDependency(includedFilePath, false);
            /* We need to get the macros from the dependency. */
            lexer.getMacros().clear();
            lexer.addMacros(context.getMacroManager().getAll().values());
            /* Invalidate cache for that dependency, since it is "polluted"
             * by symbols and macros from current file. */
            context.getCache().remove(includedFilePath);
        }
    }

    private void fileDependency(String otherFilePath, boolean isRoot) {
        final PluginLoadExecutor executor = new PluginLoadExecutor(context, otherFilePath, isRoot);
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
