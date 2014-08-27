package pl.edu.mimuw.nesc.load;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableListMultimap;
import org.anarres.cpp.InternalException;
import org.apache.log4j.Logger;
import pl.edu.mimuw.nesc.FrontendContext;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Declaration;
import pl.edu.mimuw.nesc.ast.gen.Node;
import pl.edu.mimuw.nesc.common.FileType;
import pl.edu.mimuw.nesc.common.NesCFileType;
import pl.edu.mimuw.nesc.environment.DefaultEnvironment;
import pl.edu.mimuw.nesc.environment.NescEntityEnvironment;
import pl.edu.mimuw.nesc.environment.TranslationUnitEnvironment;
import pl.edu.mimuw.nesc.exception.LexerException;
import pl.edu.mimuw.nesc.filesgraph.GraphFile;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static pl.edu.mimuw.nesc.common.FileType.C;
import static pl.edu.mimuw.nesc.common.FileType.HEADER;
import static pl.edu.mimuw.nesc.common.util.file.FileUtils.fileTypeFromExtension;

/**
 * A skeleton strategy for parsing a single file.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class LoadFileExecutor implements LexerListener, ParserListener {

    private static final Logger LOG = Logger.getLogger(LoadFileExecutor.class);

    protected final FrontendContext context;
    protected final FileCache.Builder fileCacheBuilder;
    protected final ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder;
    protected final ImmutableListMultimap.Builder<Integer, NescIssue> issuesListBuilder;
    /**
     * List of datas for files parsed in this load executor and all recursive
     * calls of load executors.
     */
    protected final LinkedList<FileCache> fileCacheList;

    protected NescLexer lexer;
    protected Parser parser;

    protected NescEntityEnvironment nescEntityEnvironment;
    protected TranslationUnitEnvironment environment;

    protected String currentFilePath;
    protected FileType fileType;

    protected Optional<Node> entity;

    /**
     * Indicates whether we have finished parsing a part of file prior to
     * the module/configuration/interface keyword. In the case of header
     * files it means EOF.
     */
    protected boolean wasExtdefsFinished;

    /**
     * Creates parse file executor.
     *
     * @param context         context
     * @param currentFilePath current file path
     */
    public LoadFileExecutor(FrontendContext context, String currentFilePath) {
        checkNotNull(context, "context cannot be null");
        checkNotNull(currentFilePath, "file path cannot be null");

        this.context = context;
        this.currentFilePath = currentFilePath;
        this.fileCacheBuilder = FileCache.builder();
        this.tokensMultimapBuilder = ImmutableListMultimap.builder();
        this.issuesListBuilder = ImmutableListMultimap.builder();
        this.fileCacheList = new LinkedList<>();
        this.wasExtdefsFinished = false;
    }

    /**
     * Parses file.
     *
     * @return list of file caches (the first one is the "root" file's cache)
     * @throws java.io.IOException
     * @throws pl.edu.mimuw.nesc.exception.LexerException
     */
    public LinkedList<FileCache> parseFile() throws IOException, LexerException {
        LOG.debug("Start parsing file: " + currentFilePath + (context.isStandalone() ? " (standalone)" : " (plug-in)"));

        setUp();
        load();
        finish();

        LOG.debug("File parsing finished: " + currentFilePath);
        return fileCacheList;
    }

    protected void setUp() throws IOException {
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

        setUpEnvironments();

        /*
         * Put dummy file cache to prevent from infinite loop in case of
         * circular dependencies. Put only if cache does not contain an
         * entry for current file.
         */
        if (!context.getCache().containsKey(currentFilePath)) {
            final FileCache dummyCache = FileCache.builder()
                    .filePath(currentFilePath)
                    .fileType(fileType)
                    .tokens(ImmutableListMultimap.<Integer, Token>builder().build())
                    .issues(ImmutableListMultimap.<Integer, NescIssue>builder().build())
                    .environment(new DefaultEnvironment())
                    .build();
            context.getCache().put(currentFilePath, dummyCache);
        }

        /*
         * Remove the nesc entity from the entity namespace to avoid
         * false redeclaration error.
         */
        if (fileType == FileType.NESC) {
            /* We should not assume that the entity has the same name as
             * the file does. Therefore, we need to keep a mapping between
             * the file and the entity. */
            final String entityOldName = context.getFileToComponent().get(currentFilePath);
            if (entityOldName != null) {
                LOG.trace("Removing entity: " + entityOldName);
                nescEntityEnvironment.remove(entityOldName);
            }
        }

        setUpLexer();
        setUpParser();
    }

    protected void setUpEnvironments() {
        this.nescEntityEnvironment = this.context.getNescEntityEnvironment();
    }

    protected void setUpLexer() throws IOException {
        this.lexer = NescLexer.builder().mainFile(currentFilePath)
                .systemIncludePaths(context.getPathsResolver().getSearchOrder())
                .userIncludePaths(context.getPathsResolver().getSearchOrder())
                .unparsedMacros(context.getPredefinedMacros())
                .build();

        lexer.setListener(this);
        lexer.start();
    }

    protected void setUpParser() {
        this.parser = new Parser(currentFilePath, lexer, context, environment,
                nescEntityEnvironment, fileType, tokensMultimapBuilder, issuesListBuilder);
        parser.setListener(this);
    }

    protected void load() throws IOException {
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
            LOG.debug("AST was built successfully.");
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
    }

    protected void finish() {
        final FileCache cache = fileCacheBuilder
                .environment(environment)
                .build();
        consumeData(cache);
    }

    protected abstract void consumeData(FileCache newCache);

    @Override
    public void fileChanged(Optional<String> from, String to, boolean push) {
        // nothing to do
    }

    @Override
    public void preprocessorDirective(PreprocessorDirective directive) {
        if (currentFilePath.equals(directive.getSourceFile())) {
            fileCacheBuilder.directive(directive);
        } // no need to collect other directives
    }

    @Override
    public void comment(Comment comment) {
        if (currentFilePath.equals(comment.getLocation().getFilePath())) {
            fileCacheBuilder.comment(comment);
        } // no need to collect other comments
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
        if (currentFilePath.equals(fileName)) {
            issuesListBuilder.put(startLine, error);
        }
        /*
         * TODO: "private includes" are pasted.
         * TODO: Collect errors from that include.
         */
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
        if (currentFilePath.equals(fileName)) {
            issuesListBuilder.put(startLine, warning);
        }
        /*
         * TODO: "private includes" are pasted.
         * TODO: Collect warnings from that include.
         */
    }

    @Override
    public void macroInstantiation(MacroToken macroToken) {
        if (currentFilePath.equals(macroToken.getMacroName())) {
            tokensMultimapBuilder.put(macroToken.getStartLocation().getLine(), macroToken);
        }
    }

    @Override
    public void nescEntityRecognized(NesCFileType nesCFileType) {
        context.getFilesGraph().getFile(currentFilePath).setNesCFileType(nesCFileType);
    }

    @Override
    public void extdefsFinished() {
        LOG.trace("Extdefs finished; file: " + currentFilePath);
        this.wasExtdefsFinished = true;
        /* TODO: private macros are currently ignored, handle them at the
         * end of file. */
        handlePublicMacros(lexer.getMacros());
    }

    protected void handlePublicMacros(Map<String, PreprocessorMacro> macros) {
        LOG.debug("Handling public macros; number of macros=" + macros.size());

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
            } // no need to collect other macros
        }
    }

    /**
     * Updates files graph. Creates nodes if absent. Adds edge from
     * the first specified file to the second one.
     *
     * @param currentFilePath current file path
     * @param otherFilePath   other file path
     */
    protected void updateFilesGraph(String currentFilePath, String otherFilePath) {
        final FileType fileType = fileTypeFromExtension(otherFilePath);
        if (!context.getFilesGraph().containsFile(otherFilePath)) {
            final GraphFile graphFile = new GraphFile(otherFilePath, fileType);
            context.getFilesGraph().addFile(graphFile);
        }
        context.getFilesGraph().addEdge(currentFilePath, otherFilePath);
    }

    private void createFilesGraphNode(String filePath, FileType fileType) {
        if (context.getFilesGraph().containsFile(filePath)) {
            return;
        }
        final GraphFile graphFile = new GraphFile(filePath, fileType);
        context.getFilesGraph().addFile(graphFile);
    }
}
