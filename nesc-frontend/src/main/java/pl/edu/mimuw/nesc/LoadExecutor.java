package pl.edu.mimuw.nesc;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import org.anarres.cpp.InternalException;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.common.FileType;
import pl.edu.mimuw.nesc.declaration.object.ObjectDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.TagDeclaration;
import pl.edu.mimuw.nesc.environment.DefaultEnvironment;
import pl.edu.mimuw.nesc.environment.NescEntityEnvironment;
import pl.edu.mimuw.nesc.environment.TranslationUnitEnvironment;
import pl.edu.mimuw.nesc.exception.LexerException;
import pl.edu.mimuw.nesc.filesgraph.GraphFile;
import pl.edu.mimuw.nesc.filesgraph.walker.FilesGraphWalker;
import pl.edu.mimuw.nesc.filesgraph.walker.NodeAction;
import pl.edu.mimuw.nesc.lexer.Comment;
import pl.edu.mimuw.nesc.lexer.LexerListener;
import pl.edu.mimuw.nesc.lexer.NescLexer;
import pl.edu.mimuw.nesc.parser.Parser;
import pl.edu.mimuw.nesc.parser.ParserListener;
import pl.edu.mimuw.nesc.preprocessor.PreprocessorMacro;
import pl.edu.mimuw.nesc.preprocessor.directive.PreprocessorDirective;
import pl.edu.mimuw.nesc.problem.NescError;
import pl.edu.mimuw.nesc.problem.NescIssue;
import pl.edu.mimuw.nesc.problem.NescWarning;
import pl.edu.mimuw.nesc.token.MacroToken;
import pl.edu.mimuw.nesc.token.Token;

import java.io.IOException;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static pl.edu.mimuw.nesc.common.FileType.C;
import static pl.edu.mimuw.nesc.common.FileType.HEADER;
import static pl.edu.mimuw.nesc.common.util.file.FileUtils.fileTypeFromExtension;
import static pl.edu.mimuw.nesc.common.util.file.FileUtils.getFileNameWithoutExtension;
import static pl.edu.mimuw.nesc.filesgraph.walker.FilesGraphWalkerFactory.ofDfsPostOrderWalker;

/**
 * The class responsible for processing source files in the proper order.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class LoadExecutor {

    private static final Logger LOG = Logger.getLogger(LoadExecutor.class);

    private final FrontendContext context;

    /**
     * Creates parser executor.
     *
     * @param context context
     */
    public LoadExecutor(FrontendContext context) {
        checkNotNull(context, "context cannot be null");

        this.context = context;
    }

    /**
     * Parses file. Cached data is used when possible.
     *
     * @param filePath      file path
     * @param isDefaultFile indicates if default files is included by default
     * @return list of file datas (the first one is the "root" file's data)
     * @throws IOException
     */
    public LinkedList<FileData> parse(String filePath, boolean isDefaultFile) throws IOException {
        return new ParseFileExecutor(context, isDefaultFile).parseFile(filePath);
    }

    /**
     * Implementation of {@link NodeAction} that collects preprocessor macros
     * and global definitions from files visited during walk over the files
     * graph.
     */
    private static final class CollectAction implements NodeAction {

        private final FrontendContext context;

        private final Set<String> visitedFiles;

        private final Map<String, PreprocessorMacro> macros;
        private final Map<String, ObjectDeclaration> objects;
        private final Map<String, TagDeclaration> tags;

        public CollectAction(FrontendContext context,
                             Set<String> visitedFiles,
                             Map<String, PreprocessorMacro> macros,
                             Map<String, ObjectDeclaration> objects,
                             Map<String, TagDeclaration> tags) {
            this.context = context;
            this.visitedFiles = visitedFiles;
            this.macros = macros;
            this.objects = objects;
            this.tags = tags;
        }

        @Override
        public void run(GraphFile graphFile) {
            final String filePath = graphFile.getFilePath();
            if (visitedFiles.contains(filePath)) {
                return;
            }

            final FileCache cache = context.getCache().get(filePath);
            assert (cache != null);

            this.visitedFiles.add(filePath);
            this.macros.putAll(cache.getMacros());
            for (Map.Entry<String, ObjectDeclaration> entry : cache.getEnvironment().getObjects().getAllFromFile()) {
                this.objects.put(entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, TagDeclaration> entry : cache.getEnvironment().getTags().getAllFromFile()) {
                this.tags.put(entry.getKey(), entry.getValue());
            }
        }

    }

    /**
     * Executor that parses a single source file. Included header files
     * are not pasted into the current file, they are parsed separately.
     *
     * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
     */
    private static final class ParseFileExecutor implements LexerListener, ParserListener {

        private final FrontendContext context;
        /**
         * Files which cached data was already used in the parsing process.
         * Each file should be "visited" at most once.
         */
        private final Set<String> visitedFiles;
        /**
         * Cache builder of the currently parsed file.
         */
        private final FileCache.Builder fileCacheBuilder;
        private final boolean isDefaultFile;
        private final ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder;
        private final ImmutableListMultimap.Builder<Integer, NescIssue> issuesListBuilder;

        private NescLexer lexer;
        private Parser parser;

        private NescEntityEnvironment nescEntityEnvironment;
        private TranslationUnitEnvironment environment;

        private String currentFilePath;
        private FileType fileType;

        private Optional<Node> entity;

        private final LinkedList<FileData> fileDatas;

        /**
         * Creates parse file executor.
         *
         * @param context       context
         * @param isDefaultFile indicates if default file is included by default
         */
        public ParseFileExecutor(FrontendContext context, boolean isDefaultFile) {
            this.context = context;
            this.visitedFiles = new HashSet<>();
            this.isDefaultFile = isDefaultFile;
            this.fileCacheBuilder = FileCache.builder();
            this.tokensMultimapBuilder = ImmutableListMultimap.builder();
            this.issuesListBuilder = ImmutableListMultimap.builder();
            this.fileDatas = new LinkedList<>();
        }

        /**
         * Parses file.
         *
         * @param filePath file path
         * @return list of file datas (the first one is the "root" file's data)
         * @throws java.io.IOException
         * @throws pl.edu.mimuw.nesc.exception.LexerException
         */
        public LinkedList<FileData> parseFile(final String filePath) throws IOException, LexerException {
            checkNotNull(filePath, "file path cannot be null");
            LOG.info("Start parsing file: " + filePath);

            setUp(filePath);
            load();
            finish();

            LOG.info("File parsing finished: " + currentFilePath);
            return fileDatas;
        }

        private void setUp(String filePath) {
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
            // TODO: maybe clear cache after a successful build of a new one?
            context.getCache().remove(currentFilePath);

            /* Set environment. */
            if (this.context.isStandalone()) {
                this.environment = this.context.getEnvironment();
            } else {
                this.environment = new TranslationUnitEnvironment();
            }
            this.nescEntityEnvironment = this.context.getNescEntityEnvironment();

            /*
             * Put dummy file cache to prevent from infinite loop in case of
             * circular dependencies.
             */
            final FileCache dummyCache = FileCache.builder()
                    .filePath(currentFilePath)
                    .fileType(fileType)
                    .tokens(ImmutableListMultimap.<Integer, Token>builder().build())
                    .issues(ImmutableListMultimap.<Integer, NescIssue>builder().build())
                    .environment(new DefaultEnvironment())
                    .build();
            context.getCache().put(currentFilePath, dummyCache);

            /* Remove the nesc entity from the entity namespace to avoid
             * false redeclaration error. */
            if (fileType == FileType.NESC) {
                /* We assume that entity name is the same as file name. */
                final String entityName = getFileNameWithoutExtension(currentFilePath);
                LOG.trace("Removing entity: " + entityName);
                nescEntityEnvironment.remove(entityName);
            }
        }

        private void load() throws IOException {
            /* Setup lexer */

            this.lexer = NescLexer.builder().mainFile(currentFilePath)
                    .systemIncludePaths(context.getPathsResolver().getSearchOrder())
                    .userIncludePaths(context.getPathsResolver().getSearchOrder())
                    .unparsedMacros(context.getPredefinedMacros())
                    .build();

            lexer.setListener(this);
            lexer.start();

            /*
             * Collect macros and global definitions from files included by
             * default.
             * Files included by default depend not only on predefined macros,
             * but also on each other. We assume that they are given by client
             * in topological order.
             * In the standalone mode just load all macros recognized so far.
             */
            if (this.context.isStandalone()) {
                lexer.addMacros(this.context.getMacroManager().getAll());
            } else {
                collectDefaultData(currentFilePath, isDefaultFile);
            }

            /* Setup parser */
            this.parser = new Parser(currentFilePath, lexer, environment,
                    nescEntityEnvironment, fileType, tokensMultimapBuilder, issuesListBuilder);
            parser.setListener(this);

		    /* Parsing */
            boolean parseSuccess;
            try {
                parser.parse();
                parseSuccess = true;
            } catch (LexerException e) {
                // TODO: parse exception message and create error object?
                parseSuccess = false;
                e.printStackTrace();
            } catch (InternalException e) {
                // TODO: parse exception message and create error object?
                parseSuccess = false;
                e.printStackTrace();
            }
            final boolean errors = parser.errors();

		    /* Cleanup parser */
            parser.removeListener();

		    /* Cleanup lexer. */
            lexer.removeListener();
            lexer.close();

		    /* Errors occurred. */
            //noinspection StatementWithEmptyBody
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

            this.entity = parser.getEntityRoot();
            if (entity.isPresent() || !FileType.NESC.equals(fileType)) {
                LOG.info("AST was built successfully.");
            } else {
                LOG.info("AST was not built.");
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
        }

        private void finish() {
            final FileCache cache = fileCacheBuilder
                    .environment(environment)
                    .build();
            context.getCache().put(currentFilePath, cache);
            final FileData data = FileData.convertFrom(cache);
            fileDatas.addFirst(data);
            LOG.trace("Put file cache into context; file: " + currentFilePath);
        }

        @Override
        public void fileChanged(Optional<String> from, String to, boolean push) {
            // nothing to do
        }

        @Override
        public boolean beforeInclude(String filePath, int line) {
            final Location visibleFrom = new Location("", line, 0);
            includeDependency(currentFilePath, filePath, visibleFrom);
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
            final Optional<Location> startLocation = Optional.of(new Location(fileName, startLine, startColumn));
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
            final Optional<Location> startLocation = Optional.of(new Location(fileName, startLine, startColumn));
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
            tokensMultimapBuilder.put(macroToken.getStartLocation().getLine(), macroToken);
        }

        @Override
        public void extdefsFinished() {
            LOG.trace("Extdefs finished; file: " + currentFilePath);

            /* Handle public macros. */
            /*
             * TODO private macros are currently ignored, handle them at the
             * end of file.
             */
            handlePublicMacros(lexer.getMacros());
        }

        @Override
        public boolean interfaceDependency(String currentEntityPath, String interfaceName, Location visibleFrom) {
            return nescDependency(currentEntityPath, interfaceName, visibleFrom, true, false, true);
        }

        @Override
        public boolean componentDependency(String currentEntityPath, String componentName, Location visibleFrom) {
            return nescDependency(currentEntityPath, componentName, visibleFrom, false, false, false);
        }

        private boolean nescDependency(String currentEntityPath, String dependencyName, Location visibleFrom,
                                       boolean collectData, boolean includeMacros, boolean includeSymbols) {
            final Optional<String> filePathOptional = context.getPathsResolver().getEntityFile(dependencyName);
            if (!filePathOptional.isPresent()) {
                return false;
            }

            fileDependency(currentEntityPath, filePathOptional.get(), visibleFrom,
                    collectData, includeMacros, includeSymbols);
            // TODO update components graph
            return true;
        }

        private void includeDependency(String currentFilePath, String includedFilePath, Location visibleFrom) {
            fileDependency(currentFilePath, includedFilePath, visibleFrom, true, true, true);
        }

        /**
         * Resolves dependency upon specified file. It checks whether the
         * file's data was already used or the file is already cached or
         * the file needs to be parsed.
         *
         * @param currentFilePath current file path
         * @param otherFilePath   other file path
         * @param visibleFrom     location from which imported declarations
         *                        will be visible in current file
         * @param collectData     indicates whether any macros or symbols
         *                        should be included
         * @param includeMacros   indicates whether macros from dependency
         *                        file should be visible
         * @param includeSymbols  indicates whether symbols from dependency
         *                        file should be visible
         */
        private void fileDependency(String currentFilePath, String otherFilePath, Location visibleFrom,
                                    boolean collectData, boolean includeMacros, boolean includeSymbols) {
            /* Check if file was already visited. */
            if (visitedFiles.contains(otherFilePath)) {
                return;
            }
            /*
             * Check if file data is cached. If not, it must be parsed first.
             */
            if (!context.getCache().containsKey(otherFilePath)) {
                final LoadExecutor executor = new LoadExecutor(context);
                try {
                    /* isDefaultFile is inherited from current file. */
                    final LinkedList<FileData> datas = executor.parse(otherFilePath, isDefaultFile);
                    fileDatas.addAll(datas);
                } catch (IOException e) {
                    LOG.error("Unexpected IOException occurred.", e);
                }
            }

            /*
             * In the standalone mode, load only macros and only in
             * case of included files.
             * In the case of components neither macros nor symbols are
             * loaded.
             */
            if (this.context.isStandalone()) {
                collectParsedData(otherFilePath, includeMacros, false);
            } else if (collectData) {
                collectParsedData(otherFilePath, includeMacros, includeSymbols);
            }

            /* Update files graph. */
            updateFilesGraph(currentFilePath, otherFilePath);
        }

        /**
         * <p>Collects cached data from files included by default and all
         * its dependencies.</p>
         * <p>The data is put into structures passed as parameters.</p>
         * <p>It skips files that have already been visited.</p>
         *
         * @param fileName      currently parsed file name
         * @param isDefaultFile indicates if default files is included by
         *                      default
         */
        private void collectDefaultData(String fileName, boolean isDefaultFile) {
            final Map<String, PreprocessorMacro> macros = new HashMap<>();
            final Map<String, ObjectDeclaration> objects = new HashMap<>();
            final Map<String, TagDeclaration> tags = new HashMap<>();

            final CollectAction action = new CollectAction(context, visitedFiles, macros, objects, tags);
            final FilesGraphWalker walker = ofDfsPostOrderWalker(context.getFilesGraph(), action);

            for (String filePath : getDefaultIncludeFiles(fileName, isDefaultFile)) {
                walker.walk(filePath);
            }

            putData(macros, objects, tags, true, true);
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
         * Collects cached data from the specified file and all its
         * dependencies. It skips files that have already been visited.
         *
         * @param filePath file path of file which data should be collected
         */
        private void collectParsedData(String filePath, boolean includeMacros, boolean includeSymbols) {
            if (!includeMacros && !includeSymbols) {
                /* Nothing to include. */
                return;
            }
            final Map<String, PreprocessorMacro> macros = new HashMap<>();
            final Map<String, ObjectDeclaration> objects = new HashMap<>();
            final Map<String, TagDeclaration> tags = new HashMap<>();

            final CollectAction action = new CollectAction(context, visitedFiles, macros, objects, tags);
            final FilesGraphWalker walker = ofDfsPostOrderWalker(context.getFilesGraph(), action);

            walker.walk(filePath);

            putData(macros, objects, tags, includeMacros, includeSymbols);
        }

        private void putData(Map<String, PreprocessorMacro> macros,
                             Map<String, ObjectDeclaration> objects,
                             Map<String, TagDeclaration> tags,
                             boolean includeMacros, boolean includeSymbols) {
            if (includeMacros) {
                lexer.addMacros(macros.values());
            }
            if (includeSymbols) {
                for (Map.Entry<String, ObjectDeclaration> entry : objects.entrySet()) {
                    environment.getObjects().add(entry.getKey(), entry.getValue(), false);
                }
                for (Map.Entry<String, TagDeclaration> entry : tags.entrySet()) {
                    environment.getTags().add(entry.getKey(), entry.getValue(), false);
                }
            }
        }

        private void handlePublicMacros(Map<String, PreprocessorMacro> macros) {
            LOG.debug("Handling public macros; number of macros=" + macros.size());
            LOG.trace("Public macros: " + macros.entrySet());

            if (this.context.isStandalone()) {
                this.context.getMacroManager().clear();
            }
            for (Map.Entry<String, PreprocessorMacro> entry : macros.entrySet()) {
                final String macroName = entry.getKey();
                final PreprocessorMacro preprocessorMacro = entry.getValue();
                final Optional<String> sourceFilePath = preprocessorMacro.getSourceFile();

                /*
                 * Only macros from "real" source files are taken into
                 * account (neither predefined nor internal).
                 */
                if (sourceFilePath.isPresent() && currentFilePath.equals(sourceFilePath.get())) {
                    fileCacheBuilder.macro(macroName, preprocessorMacro);
                }
                if (this.context.isStandalone()) {
                    this.context.getMacroManager().addMacro(preprocessorMacro);
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
