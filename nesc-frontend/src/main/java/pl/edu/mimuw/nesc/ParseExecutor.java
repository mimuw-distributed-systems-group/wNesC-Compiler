package pl.edu.mimuw.nesc;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.ast.gen.Printer;
import pl.edu.mimuw.nesc.common.FileType;
import pl.edu.mimuw.nesc.exception.LexerException;
import pl.edu.mimuw.nesc.filesgraph.GraphFile;
import pl.edu.mimuw.nesc.filesgraph.walker.FilesGraphWalker;
import pl.edu.mimuw.nesc.filesgraph.walker.NodeAction;
import pl.edu.mimuw.nesc.issue.NescError;
import pl.edu.mimuw.nesc.issue.NescIssue;
import pl.edu.mimuw.nesc.issue.NescWarning;
import pl.edu.mimuw.nesc.lexer.Comment;
import pl.edu.mimuw.nesc.lexer.LexerListener;
import pl.edu.mimuw.nesc.lexer.NescLexer;
import pl.edu.mimuw.nesc.parser.Parser;
import pl.edu.mimuw.nesc.parser.ParserListener;
import pl.edu.mimuw.nesc.parser.SymbolTable;
import pl.edu.mimuw.nesc.preprocessor.PreprocessorMacro;
import pl.edu.mimuw.nesc.preprocessor.directive.PreprocessorDirective;
import pl.edu.mimuw.nesc.token.MacroToken;
import pl.edu.mimuw.nesc.token.Token;

import java.io.IOException;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static pl.edu.mimuw.nesc.common.FileType.C;
import static pl.edu.mimuw.nesc.common.FileType.HEADER;
import static pl.edu.mimuw.nesc.common.util.file.FileUtils.fileTypeFromExtension;
import static pl.edu.mimuw.nesc.filesgraph.walker.FilesGraphWalkerFactory.ofDfsPostOrderWalker;

/**
 * The class responsible for processing source files in proper order.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class ParseExecutor {

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
     * Parses file. Cached data is used when possible.
     *
     * @param filePath      file path
     * @param isDefaultFile indicates if default files is included by default
     * @throws IOException
     */
    public void parse(String filePath, boolean isDefaultFile) throws IOException {
        new ParseFileExecutor(context, isDefaultFile).parseFile(filePath);
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

        private final FrontendContext context;
        /**
         * Files which cached data was already used in parsing process.
         * Each file should be "visited" at most once.
         */
        private final Set<String> visitedFiles;
        /**
         * Cache builder of currently parsed file.
         */
        private final FileCache.Builder fileCacheBuilder;
        private final boolean isDefaultFile;
        private final ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder;
        private final ImmutableListMultimap.Builder<Integer, NescIssue> issuesListBuilder;

        private NescLexer lexer;
        private Parser parser;
        private SymbolTable.Scope globalScope;

        // TODO: maybe use some kind of place holder to unable to modify
        // these value after assignments.
        private String currentFilePath;
        private FileType fileType;


        /**
         * Creates parse file executor.
         *
         * @param context       context
         * @param isDefaultFile indicates if default files is included by default
         */
        public ParseFileExecutor(FrontendContext context, boolean isDefaultFile) {
            this.context = context;
            this.visitedFiles = new HashSet<>();
            this.isDefaultFile = isDefaultFile;
            this.fileCacheBuilder = FileCache.builder();
            this.tokensMultimapBuilder = ImmutableListMultimap.builder();
            this.issuesListBuilder = ImmutableListMultimap.builder();
        }

        /**
         * Parses file.
         *
         * @param filePath file path
         * @throws java.io.IOException
         * @throws pl.edu.mimuw.nesc.exception.LexerException
         */
        public void parseFile(final String filePath) throws IOException, LexerException {
            checkNotNull(filePath, "file path cannot be null");
            LOG.info("Start parsing file: " + filePath);

            /* Set file path. */
            this.currentFilePath = filePath;

            /* Infer file type. */
            this.fileType = fileTypeFromExtension(currentFilePath);
            LOG.debug("Inferred file type: " + fileType);

            /*
             * Update files graph. Remove outgoing edges (dependencies) of the
             * node corresponding to currently parsed file. The ingoing edges
             * must be preserved since dependencies of other untouched files
             * have not been changed.
             * (Create files graph node if absent).
             */
            createFilesGraphNode(currentFilePath, fileType);
            context.getFilesGraph().removeOutgoingDependencies(currentFilePath);

            /* Clear cache for current file. */
            context.getCache().remove(currentFilePath);

            /*
             * Collect macros and global definitions from files included by
             * default.
             * Files included by default depend not only on predefined macros,
             * but also on each other. We assume that they are given by client
             * in topological order.
             */
            final Map<String, PreprocessorMacro> macros = new HashMap<>();
            final Map<String, Integer> idTypes = new HashMap<>();

            collectDefaultData(macros, idTypes, currentFilePath, isDefaultFile);

            /*
             * Build symbol table with given global scope.
             * Put global definitions from header files included by default.
             */
            this.globalScope = SymbolTable.Scope.ofGlobalScope();
            final SymbolTable symbolTable = new SymbolTable(this.globalScope);
            for (Map.Entry<String, Integer> entry : idTypes.entrySet()) {
                symbolTable.add(entry.getKey(), entry.getValue());
            }

		    /* Setup lexer */
            this.lexer = NescLexer.builder().mainFile(currentFilePath)
                    .systemIncludePaths(context.getPathsResolver().getSearchOrder())
                    .userIncludePaths(context.getPathsResolver().getSearchOrder())
                    .unparsedMacros(context.getPredefinedMacros())
                    .macros(macros.values())
                    .build();

            lexer.setListener(this);
            lexer.start();

            /* Setup parser */
            this.parser = new Parser(currentFilePath, lexer, symbolTable, fileType, tokensMultimapBuilder,
                    issuesListBuilder);
            parser.setListener(this);

		    /* Parsing */
            final boolean parseSuccess = parser.parse();
            final boolean errors = parser.errors();

		    /* Cleanup parser */
            parser.removeListener();

		    /* Cleanup lexer. */
            lexer.removeListener();
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

            /*
             * Public macros for nesc entities are handled on extdefsFinished
             * callback. For c and header files all macros are public so that
             * they are handled at the end of file.
             * (Handling means putting into cache structures).
             */
            if (C.equals(fileType) || HEADER.equals(fileType)) {
                handlePublicMacros(lexer.getMacros());
            }

            final Optional<Node> entity = parser.getEntityRoot();
            if (entity.isPresent()) {
                LOG.debug("AST was built successfully.");
                // entity.get().accept(new Printer(), null);
            } else {
                LOG.debug("AST was not built.");
            }
            final List<Declaration> extdefs = parser.getExtdefs();

            fileCacheBuilder.filePath(currentFilePath)
                    .fileType(fileType)
                    .entityRoot(entity.orNull())
                    .tokens(tokensMultimapBuilder.build())
                    .issues(issuesListBuilder.build());

            for (Declaration extdef : extdefs) {
                fileCacheBuilder.extdef(extdef);
            }

            // FIXME: in the future file cache should be built after semantic analysis
            final FileCache cache = fileCacheBuilder.build();
            context.getCache().put(currentFilePath, cache);
            LOG.info("Put file cache into context; file: " + currentFilePath);

            LOG.info("File parsing finished: " + currentFilePath);
        }

        @Override
        public void fileChanged(Optional<String> from, String to, boolean push) {
            // nothing to do
        }

        @Override
        public boolean beforeInclude(String filePath) {
            includeDependency(currentFilePath, filePath);
            /*
             * Never include body of files, they should be parsed separately.
             */
            return true;
        }

        @Override
        public void preprocessorDirective(PreprocessorDirective directive) {
            fileCacheBuilder.directive(directive);
        }

        @Override
        public void comment(Comment comment) {
            fileCacheBuilder.comment(comment);
        }

        @Override
        public void error(String fileName, int startLine, int startColumn,
                          Optional<Integer> endLine, Optional<Integer> endColumn, String message) {
            final Location startLocation = new Location(fileName, startLine, startColumn);
            final Optional<Location> endLocation;
            if (endLine.isPresent()) {
                endLocation = Optional.of(new Location(fileName, endLine.get(), endColumn.get()));
            } else {
                endLocation = Optional.absent();
            }
            final NescError error = new NescError(startLocation, endLocation, message);
            issuesListBuilder.put(startLine, error);
        }

        @Override
        public void warning(String fileName, int startLine, int startColumn,
                            Optional<Integer> endLine, Optional<Integer> endColumn, String message) {
            final Location startLocation = new Location(fileName, startLine, startColumn);
            final Optional<Location> endLocation;
            if (endLine.isPresent()) {
                endLocation = Optional.of(new Location(fileName, endLine.get(), endColumn.get()));
            } else {
                endLocation = Optional.absent();
            }
            final NescWarning warning = new NescWarning(startLocation, endLocation, message);
            issuesListBuilder.put(startLine, warning);
        }

        @Override
        public void macroInstantiation(MacroToken macroToken) {
            // TODO real implementation of this method
        }

        @Override
        public void extdefsFinished() {
            LOG.info("Extdefs finished; file: " + currentFilePath);

            /* Handle public macros. */
            // TODO private macros are currently ignored, handle them at the
            // end of file
            handlePublicMacros(lexer.getMacros());
        }

        @Override
        public void globalId(String id, Integer type) {
            fileCacheBuilder.globalId(id, type);
        }

        @Override
        public boolean interfaceDependency(String currentEntityPath, String interfaceName) {
            return nescDependency(currentEntityPath, interfaceName);
            // TODO update components graph
        }

        @Override
        public boolean componentDependency(String currentEntityPath, String componentName) {
            return nescDependency(currentEntityPath, componentName);
            // TODO update components graph
        }

        private boolean nescDependency(String currentEntityPath, String dependencyName) {
            final Optional<String> filePathOptional = context.getPathsResolver().getEntityFile(dependencyName);
            if (!filePathOptional.isPresent()) {
                return false;
            }

            fileDependency(currentEntityPath, filePathOptional.get());
            return true;
        }

        private void includeDependency(String currentFilePath, String includedFilePath) {
            fileDependency(currentFilePath, includedFilePath);
        }

        /**
         * Resolves dependency upon specified file. It checks whether the
         * file's data was already used or the file is already cached or
         * the file needs to be parsed.
         *
         * @param currentFilePath current file path
         * @param otherFilePath   other file path
         */
        private void fileDependency(String currentFilePath, String otherFilePath) {
            /* Check if file was already visited. */
            if (visitedFiles.contains(otherFilePath)) {
                return;
            }
            /*
             * Check if file data is cached. If not, it must be parsed first.
             */
            if (!context.getCache().containsKey(otherFilePath)) {
                final ParseExecutor executor = new ParseExecutor(context);
                try {
                    /* isDefaultFile is inherited from current file. */
                    executor.parse(otherFilePath, isDefaultFile);
                } catch (IOException e) {
                    // TODO
                    e.printStackTrace();
                }
            }

            final Map<String, PreprocessorMacro> macros = new HashMap<>();
            final Map<String, Integer> idTypes = new HashMap<>();

            collectParsedData(macros, idTypes, otherFilePath);

            /* Put cached data into proper structures. */
            for (Map.Entry<String, Integer> entry : idTypes.entrySet()) {
                globalScope.add(entry.getKey(), entry.getValue());
            }
            lexer.addMacros(macros.values());

            /* Update files graph. */
            updateFilesGraph(currentFilePath, otherFilePath);
        }

        /**
         * <p>Collects cached data from files included by default and all
         * its dependencies.</p>
         * <p>The data is put into structures passed as parameters.</p>
         * <p>It skips files that have already been visited.</p>
         *
         * @param macros        preprocessor macros
         * @param idTypes       map that associates id names to id types (i.e.
         *                      typename, identifier, componentref)
         * @param fileName      currently parsed file name
         * @param isDefaultFile indicates if default files is included by
         *                      default
         */
        private void collectDefaultData(Map<String, PreprocessorMacro> macros,
                                        Map<String, Integer> idTypes,
                                        String fileName,
                                        boolean isDefaultFile) {
            final CollectAction action = new CollectAction(context, visitedFiles, macros, idTypes);
            final FilesGraphWalker walker = ofDfsPostOrderWalker(context.getFilesGraph(), action);

            for (String filePath : getDefaultIncludeFiles(fileName, isDefaultFile)) {
                walker.walk(filePath);
            }
        }

        /**
         * <p>Files included by default may depend on each other. We assume
         * they are listed in topological order.</p>
         *
         * @param filePath files to be parsed
         * @return list of files included by default which data should be
         * added into current file
         */
        private List<String> getDefaultIncludeFiles(String filePath, boolean isDefaultFile) {
            final List<String> defaultFiles = context.getOptions().getDefaultIncludeFiles();
            if (!isDefaultFile) {
                return defaultFiles;
            }
            /*
             * Files that are included by default files should not include
             * by default other default files.
             */
            if (!defaultFiles.contains(filePath)) {
                return new LinkedList<>();
            }
            /*
             * Only "root" default files should include by default preceding
             * default files.
             */
            final List<String> result = new ArrayList<>();

            for (String otherFile : defaultFiles) {
                if (otherFile.equals(filePath)) {
                    break;
                }
                result.add(otherFile);
            }

            return result;
        }

        /**
         * Collects cached data from specified file and all its dependencies.
         * The data is put into structures passed as parameters.
         * It skips files that have already been visited.
         *
         * @param macros   preprocessor macros
         * @param idTypes  map that associates id names to id types (i.e.
         *                 typename, identifier, componentref)
         * @param filePath file path of file which data should be collected
         */
        private void collectParsedData(Map<String, PreprocessorMacro> macros,
                                       Map<String, Integer> idTypes,
                                       String filePath) {
            final CollectAction action = new CollectAction(context, visitedFiles, macros, idTypes);
            final FilesGraphWalker walker = ofDfsPostOrderWalker(context.getFilesGraph(), action);

            walker.walk(filePath);
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
                if (sourceFilePath.isPresent() && currentFilePath.equals(sourceFilePath.get())) {
                    fileCacheBuilder.macro(macroName, preprocessorMacro);
                }
            }
        }

        private void createFilesGraphNode(String filePath, FileType fileType) {
            if (context.getFilesGraph().containsFile(filePath)) {
                return;
            }
            final GraphFile graphFile = new GraphFile(filePath, fileType);
            context.getFilesGraph().addFile(graphFile);
        }

        /**
         * Updates files graph. Creates nodes if absent. Adds edge from
         * the first specified file to the second one.
         *
         * @param currentFilePath current file path
         * @param otherFilePath   other file path
         */
        private void updateFilesGraph(String currentFilePath, String otherFilePath) {
            final FileType fileType = fileTypeFromExtension(otherFilePath);
            if (!context.getFilesGraph().containsFile(otherFilePath)) {
                final GraphFile graphFile = new GraphFile(otherFilePath, fileType);
                context.getFilesGraph().addFile(graphFile);
            }
            context.getFilesGraph().addEdge(currentFilePath, otherFilePath);
        }

    }

}
