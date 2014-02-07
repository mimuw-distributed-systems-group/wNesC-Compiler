package pl.edu.mimuw.nesc;

import com.google.common.base.Optional;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.common.FileType;
import pl.edu.mimuw.nesc.exception.LexerException;
import pl.edu.mimuw.nesc.filesgraph.GraphFile;
import pl.edu.mimuw.nesc.filesgraph.walker.FilesGraphWalker;
import pl.edu.mimuw.nesc.filesgraph.walker.NodeAction;
import pl.edu.mimuw.nesc.lexer.Comment;
import pl.edu.mimuw.nesc.lexer.LexerListener;
import pl.edu.mimuw.nesc.lexer.NescLexer;
import pl.edu.mimuw.nesc.parser.Parser;
import pl.edu.mimuw.nesc.parser.ParserListener;
import pl.edu.mimuw.nesc.parser.SymbolTable;
import pl.edu.mimuw.nesc.preprocessor.PreprocessorMacro;
import pl.edu.mimuw.nesc.preprocessor.directive.PreprocessorDirective;

import java.io.IOException;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static pl.edu.mimuw.nesc.common.FileType.C;
import static pl.edu.mimuw.nesc.common.FileType.HEADER;
import static pl.edu.mimuw.nesc.common.util.file.FileUtils.*;
import static pl.edu.mimuw.nesc.filesgraph.walker.FilesGraphWalkerFactory.ofDfsPostOrderWalker;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public class ParseExecutor {

    private static final Logger LOG = Logger.getLogger(ParseExecutor.class);

    private final FrontendContext context;

    /**
     * Creates parser executor.
     *
     * @param context context
     */
    public ParseExecutor(FrontendContext context) {
        checkNotNull(context, "context cannot be null");

        this.context = context;
    }

    /**
     * Parses file. Cached data is used when possible.kk
     *
     * @param filePath            file path
     * @param includeDefaultFiles indicates if default files should be
     *                            included (default files should be
     *                            independent from each other)
     * @throws IOException
     */
    public void parse(String filePath, boolean includeDefaultFiles) throws IOException {
        new ParseFileExecutor(context, includeDefaultFiles).parseFile(filePath);
    }

    /**
     * Implementation of {@link NodeAction} that collects preprocessor macros
     * and global definitions from files visited during walk over files graph.
     */
    private static final class CollectAction implements NodeAction {

        private final FrontendContext context;

        private final Set<String> visitedFiles;

        private final Map<String, PreprocessorMacro> macros;
        private final Map<String, Integer> idTypes;

        public CollectAction(FrontendContext context,
                             Set<String> visitedFiles,
                             Map<String, PreprocessorMacro> macros,
                             Map<String, Integer> idTypes) {
            this.context = context;
            this.visitedFiles = visitedFiles;
            this.macros = macros;
            this.idTypes = idTypes;
        }

        @Override
        public void run(GraphFile graphFile) {
            final String filePath = graphFile.getFilePath();
            if (visitedFiles.contains(filePath)) {
                return;
            }

            LOG.info("Trying to use cached data for file: " + filePath);
            final FileCache cache = context.getCache().get(filePath);
            assert (cache != null);

            this.visitedFiles.add(filePath);
            this.macros.putAll(cache.getMacros());
            this.idTypes.putAll(cache.getGlobalScope().getAll());
        }

    }

    /**
     * Executor that parses single source file and all header files included
     * into this file.
     *
     * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
     */
    private static final class ParseFileExecutor implements LexerListener, ParserListener {

        private static final EnumSet<FileType> INCLUDE_FILE_TYPES = EnumSet.of(C, HEADER);

        private final FrontendContext context;
        private final Set<String> visitedFiles;
        private final Map<String, FileCache.Builder> fileCacheBuilders;
        private final boolean includeDefaultFiles;

        private NescLexer lexer;
        private Parser parser;
        private SymbolTable.Scope globalScope;

        private String currentFile;


        /**
         * Creates parse file executor.
         *
         * @param context             context
         * @param includeDefaultFiles indicates if default files should be
         *                            included (default files should be
         *                            independent from each other)
         */
        public ParseFileExecutor(FrontendContext context, boolean includeDefaultFiles) {
            this.context = context;
            this.visitedFiles = new HashSet<>();
            this.fileCacheBuilders = new HashMap<>();
            this.includeDefaultFiles = includeDefaultFiles;
        }

        /**
         * Parses file.
         *
         * @param filePath file path
         * @throws java.io.IOException
         * @throws pl.edu.mimuw.nesc.exception.LexerException
         */
        public void parseFile(final String filePath) throws IOException, LexerException {
            LOG.info("Start parsing file: " + filePath);

            /* Infer file type. */
            final FileType fileType = fileTypeFromExtension(filePath);
            LOG.debug("Inferred file type: " + fileType);

            /*
             * Update files graph. Remove node corresponding to currently
             * parsed file and all adjacent edges from files graph.
             * Create "fresh" node.
             */
            context.getFilesGraph().removeFile(filePath);
            final GraphFile graphFile = new GraphFile(filePath, fileType);
            context.getFilesGraph().addFile(graphFile);

            /* Clear cache. */
            context.getCache().remove(filePath);

            /* Prepare file cache builder. */
            final FileCache.Builder builder = FileCache.builder();
            this.fileCacheBuilders.put(filePath, builder);

            /*
             * Collect macros and global definitions from files included by
             * default.
             * Files included by default depends only on predefined macros,
             * do not depends on each other.
             */
            final Map<String, PreprocessorMacro> macros = new HashMap<>();
            final Map<String, Integer> idTypes = new HashMap<>();

            if (includeDefaultFiles) {
                collectDefaultData(macros, idTypes);
            }

            /*
             * Build symbol table with given global scope.
             * Put global definitions from header files included by default.
             */
            globalScope = SymbolTable.Scope.ofGlobalScope();
            final SymbolTable symbolTable = new SymbolTable(globalScope);
            for (Map.Entry<String, Integer> entry : idTypes.entrySet()) {
                symbolTable.add(entry.getKey(), entry.getValue());
            }

		    /* Setup lexer */
            lexer = NescLexer.builder().mainFile(filePath)
                    .systemIncludePaths(context.getPathsResolver().getSearchOrder())
                    .userIncludePaths(context.getPathsResolver().getSearchOrder())
                    .unparsedMacros(context.getPredefinedMacros())
                    .macros(macros.values())
                    .build();

            lexer.addListener(this);
            lexer.start();

            /* Setup parser */
            parser = new Parser(filePath, lexer, symbolTable, fileType);
            parser.setListener(this);

		    /* Parsing */
            final boolean parseSuccess = parser.parse();
            final boolean errors = parser.errors();

		    /* Cleanup parser */
            parser.removeListener();

		    /* Cleanup lexer. */
            lexer.removeListener(this);
            lexer.close();

		    /* Errors occurred. */
            if (!parseSuccess || errors) {
                // TODO
            }
            /* Parsed successfully. */
            else {
                // TODO
            }

            /* Finalize file processing. */

            if (C.equals(fileType) || HEADER.equals(fileType)) {
                handlePublicMacros(lexer.getMacros());
            }

            final Node entity = parser.getEntityRoot();
            final List<Declaration> extdefs = parser.getExtdefs();

            builder.filePath(filePath)
                    .entityRoot(entity);

            buildFileCaches(extdefs, false);


            LOG.info("File parsing finished: " + filePath);
        }

        @Override
        public void fileChanged(Optional<String> from, String to, boolean push) {
            LOG.debug("File changed from " + from + " to " + to + "; " + (push ? "push" : "pop"));

            this.currentFile = to;

            if (push) {
                final FileType fileType = fileTypeFromExtension(to);
                /* Update files graph. */
                if (from.isPresent()) {
                    if (!this.context.getFilesGraph().containsFile(to)) {
                        final GraphFile graphFile = new GraphFile(to, fileType);
                        this.context.getFilesGraph().addFile(graphFile);
                    }
                    this.context.getFilesGraph().addEdge(from.get(), to);
                }

                /*
                 * Check if a new C or header file will be processed now. If
                 * the file is seen for the first time, create proper
                 * structure for the file.
                 */
                if ((isHeaderFile(to) || isCFile(to))
                        //&& !context.getCache().containsKey(to)
                        && !this.fileCacheBuilders.containsKey(to)) {
                    final FileCache.Builder builder = FileCache.builder()
                            .filePath(to)
                            .entityRoot(null);
                    this.fileCacheBuilders.put(to, builder);
                }
            }
        }

        @Override
        public void preprocessorDirective(PreprocessorDirective directive) {
            final FileCache.Builder builder = this.fileCacheBuilders.get(directive.getSourceFile());
            builder.directive(directive);
        }

        @Override
        public void comment(Comment comment) {
            final String source = comment.getLocation().getFilename();
            final FileCache.Builder builder = this.fileCacheBuilders.get(source);
            builder.comment(comment);
        }

        @Override
        public void extdefsFinished() {
            LOG.info("Extdefs finished; file: " + currentFile);
            /* Handle public macros. */
            // TODO private macros are currently ignored
            handlePublicMacros(this.lexer.getMacros());

            /* Collect extdefs for included files. */
            final List<Declaration> extdefs = parser.getExtdefs();
            buildFileCaches(extdefs, true);
        }

        @Override
        public void globalId(String id, Integer type) {
            this.fileCacheBuilders.get(currentFile).globalId(id, type);
        }

        @Override
        public void interfaceDependency(String currentEntityPath, String interfaceName) {
            dependency(currentEntityPath, interfaceName);
        }

        @Override
        public void componentDependency(String currentEntityPath, String componentName) {
            dependency(currentEntityPath, componentName);
        }

        private void dependency(String currentEntityPath, String dependencyName) {
            final String filePath = this.context.getPathsResolver().getEntityFile(dependencyName);
            if (filePath == null) {
                // TODO
                return;
            }

            /* Check if file was already visited. */
            if (this.visitedFiles.contains(filePath)) {
                return;
            }
            /* Check if file data cached. If not, it must be parsed first. */
            if (!this.context.getCache().containsKey(filePath)) {
                final ParseExecutor executor = new ParseExecutor(this.context);
                try {
                    executor.parse(filePath, this.includeDefaultFiles);
                } catch (IOException e) {
                    // TODO
                    e.printStackTrace();
                }
            }

            final Map<String, PreprocessorMacro> macros = new HashMap<>();
            final Map<String, Integer> idTypes = new HashMap<>();

            collectParsedData(macros, idTypes, filePath);

            /* Put cached data into proper structures. */
            for (Map.Entry<String, Integer> entry : idTypes.entrySet()) {
                this.globalScope.add(entry.getKey(), entry.getValue());
            }
            this.lexer.addMacros(macros.values());

            /* Update files graph. */
            this.context.getFilesGraph().addEdge(currentEntityPath, filePath);

        }

        private void collectDefaultData(Map<String, PreprocessorMacro> macros,
                                        Map<String, Integer> idTypes) {
            final CollectAction action = new CollectAction(context, visitedFiles, macros, idTypes);
            final FilesGraphWalker walker = ofDfsPostOrderWalker(context.getFilesGraph(), action);

            for (String filePath : this.context.getOptions().getDefaultIncludeFiles()) {
                walker.walk(filePath);
            }
        }

        private void collectParsedData(Map<String, PreprocessorMacro> macros,
                                       Map<String, Integer> idTypes,
                                       String filePath) {
            final CollectAction action = new CollectAction(context, visitedFiles, macros, idTypes);
            final FilesGraphWalker walker = ofDfsPostOrderWalker(context.getFilesGraph(), action);

            walker.walk(filePath);
        }

        private void buildFileCaches(List<Declaration> extdefs, boolean onlyIncludedFiles) {
            LOG.info("Building file caches; onlyIncludedFiles=" + onlyIncludedFiles);
            /*
             * NOTE: The extdefs list of the currently parsed file containing
             * entity definition may contain declarations that does not come
             * from the main file. The actual source of declaration may be
             * header file which was included into the current file. Therefore
             * the declarations should be associated with the files of their
             * actual origin (and put into the proper structures).
             */
            for (Declaration extdef : extdefs) {
                /*
                 * FIXME: temporarily all declarations are associated with
                 * current file to be able to determine the actual source of
                 * extdef, the location field must be set.
                 * TODO: put globalIds to proper builders
                 */
            }

            for (Map.Entry<String, FileCache.Builder> entry : this.fileCacheBuilders.entrySet()) {
                final String filePath = entry.getKey();
                final FileType fileType = fileTypeFromExtension(filePath);
                final FileCache.Builder builder = entry.getValue();

                builder.fileType(fileType);

                if (onlyIncludedFiles && !INCLUDE_FILE_TYPES.contains(fileType)) {
                    continue;
                }

                final FileCache cache = builder.build();
                this.context.getCache().put(filePath, cache);
                LOG.info("Put file cache into context; file: " + filePath);
            }
        }

        private void handlePublicMacros(Map<String, PreprocessorMacro> macros) {
            LOG.debug("Handling public macros; number of macros=" + macros.size());

            for (Map.Entry<String, PreprocessorMacro> entry : macros.entrySet()) {
                final String macroName = entry.getKey();
                final PreprocessorMacro preprocessorMacro = entry.getValue();
                final Optional<String> sourceFilePath = preprocessorMacro.getSourceFile();

                /*
                 * Only macros from "real" source files are taken into
                 * account (not predefined or internal).
                 */
                if (sourceFilePath.isPresent()) {
                    final FileCache.Builder builder = this.fileCacheBuilders.get(sourceFilePath.get());

                    /*
                     * Macros may come from file that was parsed previously and
                     * the lexer was fed with them. They should be ignored.
                     */
                    if (builder != null) {
                        builder.macro(macroName, preprocessorMacro);
                    }
                }
            }
        }

    }

}
